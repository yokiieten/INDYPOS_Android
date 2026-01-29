package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.domain.model.CreateEmployeeRequest
import com.indybrain.indypos_Android.domain.model.Employee

interface EmployeeRepository {
    suspend fun createEmployee(request: CreateEmployeeRequest): Result<Unit>
    suspend fun getEmployees(): Result<List<Employee>>
}
