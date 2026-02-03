package com.indybrain.indypos_Android.presentation.employeemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Employee Management screen
 */
@HiltViewModel
class EmployeeManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EmployeeManagementUiState())
    val uiState: StateFlow<EmployeeManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadEmployees()
    }
    
    /**
     * Load employees from repository
     */
    fun loadEmployees() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = authRepository.getEmployees()
            
            result.fold(
                onSuccess = { employees ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            employees = employees,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูลพนักงาน"
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Refresh employee list (for pull-to-refresh)
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            
            val result = authRepository.getEmployees()
            
            result.fold(
                onSuccess = { employees ->
                    _uiState.update { 
                        it.copy(
                            isRefreshing = false,
                            employees = employees,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isRefreshing = false,
                            errorMessage = exception.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูลพนักงาน"
                        )
                    }
                }
            )
        }
    }
}

/**
 * UI State for Employee Management screen
 */
data class EmployeeManagementUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val employees: List<User> = emptyList(),
    val errorMessage: String? = null
)
