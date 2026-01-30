package com.indybrain.indypos_Android.presentation.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.CreateEmployeeRequest
import com.indybrain.indypos_Android.domain.model.Employee
import com.indybrain.indypos_Android.domain.model.UpdateEmployeeRequest
import com.indybrain.indypos_Android.domain.repository.EmployeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditEmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val employeeId: Int? = savedStateHandle.get<String>("employeeId")?.toIntOrNull()

    private val _uiState = MutableStateFlow(AddEditEmployeeUiState(isEditMode = employeeId != null))
    val uiState: StateFlow<AddEditEmployeeUiState> = _uiState.asStateFlow()

    init {
        if (employeeId != null) {
            loadEmployee()
        }
    }

    private fun loadEmployee() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            employeeRepository.getEmployees()
                .onSuccess { employees ->
                    val employee = employees.find { it.id == employeeId }
                    if (employee != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                employee = employee
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "ไม่พบข้อมูลพนักงาน"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "ไม่สามารถโหลดข้อมูลพนักงานได้"
                        )
                    }
                }
        }
    }

    fun createEmployee(request: CreateEmployeeRequest) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            employeeRepository.createEmployee(request)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, showSuccessDialog = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการสร้างพนักงาน"
                        )
                    }
                }
        }
    }

    fun updateEmployee(request: UpdateEmployeeRequest) {
        if (_uiState.value.isLoading || employeeId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            employeeRepository.updateEmployee(employeeId, request)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, showSuccessDialog = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการแก้ไขพนักงาน"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(showSuccessDialog = false) }
    }
}

data class AddEditEmployeeUiState(
    val isLoading: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,
    val employee: Employee? = null
)
