package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateEmployeeRequestDto(
    @SerializedName("first_name")
    val firstName: String? = null,
    @SerializedName("last_name")
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("role_id")
    val roleId: Int? = null,
    @SerializedName("is_activated")
    val isActivated: Boolean? = null,
    @SerializedName("permissions")
    val permissions: List<String> = emptyList()
)
