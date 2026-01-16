package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.domain.model.CreateEmployeeRequest

interface EmployeeRepository {
    suspend fun createEmployee(request: CreateEmployeeRequest): Result<Unit>
}
