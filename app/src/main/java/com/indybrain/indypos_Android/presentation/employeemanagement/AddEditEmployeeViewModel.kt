package com.indybrain.indypos_Android.presentation.employeemanagement

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/** Permissions that can be assigned to employees (from API) */
val AVAILABLE_EMPLOYEE_PERMISSIONS = listOf(
    "order.create",
    "order.cancel",
    "report.view",
    "role.manage"
)

@HiltViewModel
class AddEditEmployeeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditEmployeeUiState())
    val uiState: StateFlow<AddEditEmployeeUiState> = _uiState.asStateFlow()

    private var employeeId: Int? = null

    val availablePermissions: List<String> = AVAILABLE_EMPLOYEE_PERMISSIONS

    init {
        // Load employee data from navigation argument if exists
        val employeeJson = savedStateHandle.get<String>("employeeData")
        if (employeeJson != null) {
            try {
                val decodedJson = URLDecoder.decode(employeeJson, "UTF-8")
                val employeeData = gson.fromJson(decodedJson, EmployeeNavigationData::class.java)
                employeeId = employeeData.id
                _uiState.update {
                    it.copy(
                        employeeId = employeeData.id,
                        isLoading = false,
                        username = employeeData.username,
                        firstName = employeeData.firstName,
                        lastName = employeeData.lastName,
                        email = employeeData.email,
                        phone = employeeData.phone,
                        roleIdStr = employeeData.roleId?.toString() ?: "",
                        selectedPermissions = employeeData.permissions.toSet(),
                        isActivated = employeeData.isActivated
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "ไม่สามารถโหลดข้อมูลพนักงานได้"
                    )
                }
            }
        }
    }

    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }
    fun updateFirstName(value: String) = _uiState.update { it.copy(firstName = value, errorMessage = null) }
    fun updateLastName(value: String) = _uiState.update { it.copy(lastName = value, errorMessage = null) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun updatePhone(value: String) = _uiState.update { it.copy(phone = value, errorMessage = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun updateRoleIdStr(value: String) = _uiState.update { it.copy(roleIdStr = value, errorMessage = null) }
    fun updateIsActivated(value: Boolean) = _uiState.update { it.copy(isActivated = value) }

    fun togglePermission(permission: String) {
        _uiState.update { state ->
            val newSet = state.selectedPermissions.toMutableSet()
            if (newSet.contains(permission)) newSet.remove(permission) else newSet.add(permission)
            state.copy(selectedPermissions = newSet, errorMessage = null)
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun dismissSuccess() = _uiState.update { it.copy(isSuccess = false) }

    fun save() {
        val state = _uiState.value
        val isEdit = employeeId != null

        // Validation
        if (!isEdit) {
            if (state.username.isBlank()) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_username_required)) }
                return
            }
            if (state.username.length < 3) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_username_too_short)) }
                return
            }
            if (state.password.isBlank()) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_password_required)) }
                return
            }
            if (state.password.length < 6) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_password_too_short)) }
                return
            }
        }
        if (state.firstName.isBlank()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_first_name_required)) }
            return
        }
        if (state.lastName.isBlank()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_last_name_required)) }
            return
        }
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_email_required)) }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_email_invalid)) }
            return
        }
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_phone_required)) }
            return
        }
        if (state.phone.length < 10) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.employee_add_edit_validation_phone_too_short)) }
            return
        }

        val roleId = state.roleIdStr.trim().toIntOrNull()
        val permissions = state.selectedPermissions.toList().takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (isEdit) {
                authRepository.updateEmployee(
                    employeeId = employeeId!!,
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    roleId = roleId,
                    isActivated = state.isActivated,
                    permissions = permissions
                )
            } else {
                authRepository.createEmployee(
                    username = state.username.trim(),
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    password = state.password,
                    roleId = roleId,
                    permissions = permissions
                )
            }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: context.getString(R.string.employee_add_edit_validation_first_name_required)
                        )
                    }
                }
            )
        }
    }
}

data class AddEditEmployeeUiState(
    val employeeId: Int? = null,
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val roleIdStr: String = "",
    val selectedPermissions: Set<String> = emptySet(),
    val isActivated: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
