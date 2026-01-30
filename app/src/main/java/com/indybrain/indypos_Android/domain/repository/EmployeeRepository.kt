package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.domain.model.CreateEmployeeRequest
import com.indybrain.indypos_Android.domain.model.Employee
import com.indybrain.indypos_Android.domain.model.UpdateEmployeeRequest

interface EmployeeRepository {
    suspend fun createEmployee(request: CreateEmployeeRequest): Result<Unit>
    suspend fun getEmployees(): Result<List<Employee>>
    suspend fun updateEmployee(id: Int, request: UpdateEmployeeRequest): Result<Unit>
    suspend fun deleteEmployee(id: Int): Result<Unit>
}
