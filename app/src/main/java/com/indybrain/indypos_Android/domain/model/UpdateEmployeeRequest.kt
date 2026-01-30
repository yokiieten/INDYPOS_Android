package com.indybrain.indypos_Android.domain.model

data class UpdateEmployeeRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val roleId: Int? = null,
    val isActivated: Boolean? = null
)
