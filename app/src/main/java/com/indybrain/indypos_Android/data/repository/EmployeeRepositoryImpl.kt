package com.indybrain.indypos_Android.data.repository

import com.google.gson.Gson
import com.indybrain.indypos_Android.data.remote.api.EmployeesApi
import com.indybrain.indypos_Android.data.remote.dto.CreateEmployeeRequestDto
import com.indybrain.indypos_Android.data.remote.dto.UpdateEmployeeRequestDto
import com.indybrain.indypos_Android.domain.model.CreateEmployeeRequest
import com.indybrain.indypos_Android.domain.model.Employee
import com.indybrain.indypos_Android.domain.model.UpdateEmployeeRequest
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

    override suspend fun getEmployees(): Result<List<Employee>> {
        return try {
            val response = employeesApi.getEmployees()

            val isSuccessStatus = response.status == 200
            if (isSuccessStatus && response.data != null) {
                val employees = response.data.mapNotNull { dto ->
                    if (dto.id != null && dto.username != null) {
                        Employee(
                            id = dto.id,
                            username = dto.username,
                            firstName = dto.firstName.orEmpty(),
                            lastName = dto.lastName.orEmpty(),
                            email = dto.email.orEmpty(),
                            phone = dto.phone.orEmpty(),
                            role = dto.role.orEmpty(),
                            roleId = dto.roleId ?: 0,
                            roleName = dto.roleName.orEmpty(),
                            isActivated = dto.isActivated ?: false,
                            shopName = dto.shopName.orEmpty(),
                            createdAt = dto.createdAt.orEmpty(),
                            updatedAt = dto.updatedAt.orEmpty()
                        )
                    } else null
                }
                Result.success(employees)
            } else {
                val errorMessage = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message.takeIf { it.isNotBlank() }
                    ?: "ไม่สามารถโหลดรายการพนักงานได้"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody(), "ไม่สามารถโหลดรายการพนักงานได้")
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
                else -> e.message ?: "ไม่สามารถโหลดรายการพนักงานได้"
            }
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }

    override suspend fun updateEmployee(id: Int, request: UpdateEmployeeRequest): Result<Unit> {
        return try {
            val response = employeesApi.updateEmployee(
                id = id,
                request = UpdateEmployeeRequestDto(
                    firstName = request.firstName?.trim(),
                    lastName = request.lastName?.trim(),
                    email = request.email?.trim(),
                    phone = request.phone?.trim(),
                    roleId = request.roleId,
                    isActivated = request.isActivated
                )
            )

            val isSuccessStatus = response.status == 200
            if (isSuccessStatus) {
                Result.success(Unit)
            } else {
                val errorMessage = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message.takeIf { it.isNotBlank() }
                    ?: "เกิดข้อผิดพลาดในการแก้ไขพนักงาน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody(), "เกิดข้อผิดพลาดในการแก้ไขพนักงาน")
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = parseNetworkError(e, "เกิดข้อผิดพลาดในการแก้ไขพนักงาน")
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }

    override suspend fun deleteEmployee(id: Int): Result<Unit> {
        return try {
            val response = employeesApi.deleteEmployee(id)

            val isSuccessStatus = response.status == 200
            if (isSuccessStatus) {
                Result.success(Unit)
            } else {
                val errorMessage = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message.takeIf { it.isNotBlank() }
                    ?: "เกิดข้อผิดพลาดในการลบพนักงาน"
                Result.failure(IllegalStateException(errorMessage))
            }
        } catch (e: HttpException) {
            val errorMessage = parseErrorMessage(e.response()?.errorBody(), "เกิดข้อผิดพลาดในการลบพนักงาน")
            Result.failure(IllegalStateException(errorMessage, e))
        } catch (e: Exception) {
            val errorMessage = parseNetworkError(e, "เกิดข้อผิดพลาดในการลบพนักงาน")
            Result.failure(IllegalStateException(errorMessage, e))
        }
    }

    private fun parseErrorMessage(errorBody: ResponseBody?, defaultMessage: String = "เกิดข้อผิดพลาดในการสร้างพนักงาน"): String {
        return try {
            if (errorBody == null) {
                return defaultMessage
            }

            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return defaultMessage
            }

            val errorResponse = gson.fromJson(errorJson, EmployeeErrorResponse::class.java)
            errorResponse?.error?.takeIf { it.isNotBlank() }
                ?: errorResponse?.message?.takeIf { it.isNotBlank() }
                ?: defaultMessage
        } catch (e: Exception) {
            defaultMessage
        }
    }

    private fun parseNetworkError(e: Exception, defaultMessage: String): String {
        return when {
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
            else -> e.message ?: defaultMessage
        }
    }
}

private data class EmployeeErrorResponse(
    val error: String?,
    val message: String?
)
