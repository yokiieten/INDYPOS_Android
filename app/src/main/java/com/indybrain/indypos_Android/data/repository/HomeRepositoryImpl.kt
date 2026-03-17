package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.remote.api.HomeApi
import com.indybrain.indypos_Android.domain.repository.HomeRepository
import com.indybrain.indypos_Android.domain.repository.TodaySalesResult
import retrofit2.HttpException
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val homeApi: HomeApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : HomeRepository {

    override suspend fun getTodaySales(
        date: String?,
        timezone: String
    ): Result<TodaySalesResult> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("No network connection"))
        }
        return try {
            val response = homeApi.getTodaySales(date = date, timezone = timezone)
            if (response.status == 200 && response.data != null) {
                val data = response.data
                val topProduct = data.topProduct
                val result = TodaySalesResult(
                    todaysSales = data.todaysSales,
                    ordersToday = data.ordersToday,
                    topProductName = topProduct?.name ?: "",
                    topProductQuantity = topProduct?.quantity ?: 0,
                    topProductAmount = topProduct?.amount ?: 0.0
                )
                Result.success(result)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val message = errorBody?.let { parseErrorMessage(it) } ?: e.message()
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorJson: String): String? {
        return try {
            val gson = com.google.gson.Gson()
            val obj = gson.fromJson(errorJson, com.google.gson.JsonObject::class.java)
            obj?.get("message")?.asString?.takeIf { it.isNotBlank() }
                ?: obj?.get("error")?.asString?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
