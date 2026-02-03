package com.indybrain.indypos_Android.presentation.employeemanagement

import com.indybrain.indypos_Android.domain.model.User

/**
 * Lightweight data class for passing employee data via navigation
 */
data class EmployeeNavigationData(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val roleId: Int?,
    val permissions: List<String>,
    val isActivated: Boolean
) {
    companion object {
        fun fromUser(user: User): EmployeeNavigationData {
            return EmployeeNavigationData(
                id = user.id,
                username = user.username ?: "",
                firstName = user.firstName ?: "",
                lastName = user.lastName ?: "",
                email = user.email,
                phone = user.phone ?: "",
                roleId = user.roleId,
                permissions = user.permissions ?: emptyList(),
                isActivated = user.isActivated ?: true
            )
        }
    }
}
