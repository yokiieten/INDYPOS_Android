package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.ApiResponseDto
import com.indybrain.indypos_Android.data.remote.dto.CreateEmployeeRequestDto
import com.indybrain.indypos_Android.data.remote.dto.EmployeeDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for employee endpoints
 */
interface EmployeesApi {
    @POST("protected/employees")
    suspend fun createEmployee(
        @Body request: CreateEmployeeRequestDto
    ): ApiResponseDto<EmployeeDto>
}
