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

    /**
     * Delete employee by id
     */
    fun deleteEmployee(employeeId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            val result = authRepository.deleteEmployee(employeeId)

            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            employees = state.employees.filterNot { it.id == employeeId },
                            successMessage = "ลบพนักงานสำเร็จ"
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "ไม่สามารถลบพนักงานได้"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

/**
 * UI State for Employee Management screen
 */
data class EmployeeManagementUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val employees: List<User> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)
