package com.indybrain.indypos_Android.data.repository

import com.google.gson.Gson
import com.indybrain.indypos_Android.data.remote.api.EmployeesApi
import com.indybrain.indypos_Android.data.remote.dto.CreateEmployeeRequestDto
import com.indybrain.indypos_Android.domain.model.CreateEmployeeRequest
import com.indybrain.indypos_Android.domain.repository.EmployeeRepository
import okhttp3.ResponseBody
import retrofit2.HttpException
import javax.inject.Inject

class EmployeeRepositoryImpl @Inject constructor(
    private val employeesApi: EmployeesApi,
    private val gson: Gson
) : EmployeeRepository {
    override suspend fun createEmployee(request: CreateEmployeeRequest): Result<Unit> {
        return try {
            val response = employeesApi.createEmployee(
                CreateEmployeeRequestDto(
                    username = request.username.trim(),
                    firstName = request.firstName.trim(),
                    lastName = request.lastName.trim(),
                    email = request.email.trim(),
                    phone = request.phone.trim(),
                    password = request.password,
                    roleId = request.roleId
                )
            )

            val isSuccessStatus = response.status == 200 || response.status == 201
            if (isSuccessStatus && response.data != null) {
                Result.success(Unit)
            } else {
                val errorMessage = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message.takeIf { it.isNotBlank() }
                    ?: "เกิดข้อผิดพลาดในการสร้างพนักงาน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody())
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้ กรุณาตรวจสอบการเชื่อมต่ออินเทอร์เน็ต"
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "การเชื่อมต่อหมดเวลา กรุณาลองใหม่อีกครั้ง"
                e.message?.contains("No address associated with hostname", ignoreCase = true) == true ->
                    "ไม่พบเซิร์ฟเวอร์ กรุณาตรวจสอบการเชื่อมต่อ"
                e.message?.contains("Connection refused", ignoreCase = true) == true ->
                    "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้"
                e.message?.contains("Network is unreachable", ignoreCase = true) == true ->
                    "ไม่สามารถเชื่อมต่ออินเทอร์เน็ตได้"
                else -> e.message ?: "เกิดข้อผิดพลาดในการสร้างพนักงาน"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }

    private fun parseErrorMessage(errorBody: ResponseBody?): String {
        return try {
            if (errorBody == null) {
                return "เกิดข้อผิดพลาดในการสร้างพนักงาน"
            }

            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return "เกิดข้อผิดพลาดในการสร้างพนักงาน"
            }

            val errorResponse = gson.fromJson(errorJson, EmployeeErrorResponse::class.java)
            errorResponse?.error?.takeIf { it.isNotBlank() }
                ?: errorResponse?.message?.takeIf { it.isNotBlank() }
                ?: "เกิดข้อผิดพลาดในการสร้างพนักงาน"
        } catch (e: Exception) {
            "เกิดข้อผิดพลาดในการสร้างพนักงาน"
        }
    }
}

private data class EmployeeErrorResponse(
    val error: String?,
    val message: String?
)
