package com.indybrain.indypos_Android.domain.model

data class CreateEmployeeRequest(
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
    val roleId: Int,
    val permissions: List<String> = emptyList()
)
