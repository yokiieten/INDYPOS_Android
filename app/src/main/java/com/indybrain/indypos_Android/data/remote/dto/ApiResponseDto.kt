package com.indybrain.indypos_Android.data.remote.dto

/**
 * Generic API response wrapper
 */
data class ApiResponseDto<T>(
    val status: Int,
    val message: String,
    val data: T?,
    val error: String? = null,
    val timestamp: String? = null
)

