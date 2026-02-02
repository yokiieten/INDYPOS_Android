package com.indybrain.indypos_Android.domain.model

data class Employee(
    val id: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val role: String,
    val roleId: Int,
    val roleName: String,
    val isActivated: Boolean,
    val shopName: String,
    val createdAt: String,
    val updatedAt: String,
    val permissions: List<String> = emptyList(),
    val rolePermissions: List<String> = emptyList()
)
