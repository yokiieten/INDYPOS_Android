package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.ApiResponseDto
import com.indybrain.indypos_Android.data.remote.dto.CreateEmployeeRequestDto
import com.indybrain.indypos_Android.data.remote.dto.EmployeeDto
import com.indybrain.indypos_Android.data.remote.dto.UpdateEmployeeRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit API interface for employee endpoints
 */
interface EmployeesApi {
    @POST("protected/employees")
    suspend fun createEmployee(
        @Body request: CreateEmployeeRequestDto
    ): ApiResponseDto<EmployeeDto>

    @GET("protected/employees")
    suspend fun getEmployees(): ApiResponseDto<List<EmployeeDto>>

    @PUT("protected/employees/{id}")
    suspend fun updateEmployee(
        @Path("id") id: Int,
        @Body request: UpdateEmployeeRequestDto
    ): ApiResponseDto<EmployeeDto>

    @DELETE("protected/employees/{id}")
    suspend fun deleteEmployee(
        @Path("id") id: Int
    ): ApiResponseDto<Unit>
}
