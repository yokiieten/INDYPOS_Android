package com.indybrain.indypos_Android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.Employee
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.EmployeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        loadEmployees()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.update { current ->
                    val name = buildString {
                        val first = user?.firstName.orEmpty()
                        val last = user?.lastName.orEmpty()
                        when {
                            first.isNotBlank() && last.isNotBlank() -> append("$first $last")
                            first.isNotBlank() -> append(first)
                            last.isNotBlank() -> append(last)
                            !user?.username.isNullOrBlank() -> append(user?.username)
                            else -> append("User")
                        }
                    }
                    current.copy(
                        displayName = name,
                        email = user?.email ?: "",
                        packageName = user?.subscriptionPlan ?: "Basic"
                    )
                }
            }
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingEmployees = true, employeesError = null) }

            employeeRepository.getEmployees()
                .onSuccess { employees ->
                    _uiState.update {
                        it.copy(
                            employees = employees,
                            isLoadingEmployees = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingEmployees = false,
                            employeesError = error.message ?: "ไม่สามารถโหลดรายการพนักงานได้"
                        )
                    }
                }
        }
    }

    fun clearEmployeesError() {
        _uiState.update { it.copy(employeesError = null) }
    }
}

data class AccountUiState(
    val displayName: String = "",
    val email: String = "",
    val packageName: String = "Basic",
    val employees: List<Employee> = emptyList(),
    val isLoadingEmployees: Boolean = false,
    val employeesError: String? = null
)







