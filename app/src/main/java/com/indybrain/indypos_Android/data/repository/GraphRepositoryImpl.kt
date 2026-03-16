package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.data.remote.api.GraphApi
import com.indybrain.indypos_Android.domain.repository.GraphDashboardResult
import com.indybrain.indypos_Android.domain.repository.GraphRepository
import com.indybrain.indypos_Android.presentation.graph.BestSellerData
import com.indybrain.indypos_Android.presentation.graph.ChartDataPoint
import com.indybrain.indypos_Android.presentation.graph.GraphSummary
import com.indybrain.indypos_Android.presentation.graph.ProductStatsData
import com.indybrain.indypos_Android.presentation.graph.RevenueComparison
import com.indybrain.indypos_Android.presentation.graph.TimePeriod
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GraphRepositoryImpl @Inject constructor(
    private val graphApi: GraphApi
) : GraphRepository {

    override suspend fun getDashboard(
        filterType: TimePeriod,
        startDateMillis: Long?,
        endDateMillis: Long?
    ): Result<GraphDashboardResult> {
        return try {
            val filterTypeStr = when (filterType) {
                TimePeriod.Today -> "today"
                TimePeriod.Week -> "week"
                TimePeriod.Month -> "month"
                TimePeriod.Custom -> "custom"
            }

            val startDate = if (filterType == TimePeriod.Custom && startDateMillis != null) {
                formatDateForApi(startDateMillis)
            } else null

            val endDate = if (filterType == TimePeriod.Custom && endDateMillis != null) {
                formatDateForApi(endDateMillis)
            } else null

            val response = graphApi.getDashboard(
                filterType = filterTypeStr,
                startDate = startDate,
                endDate = endDate,
                timezone = "Asia/Bangkok"
            )

            if (response.status == 200 && response.data != null) {
                val data = response.data
                val dashboard = data.dashboard

                val summary = GraphSummary(
                    todaySales = dashboard.revenue,
                    costOfExpenses = dashboard.expenses,
                    ordersToday = dashboard.successfulOrders,
                    cancelledOrders = dashboard.cancelledOrders,
                    totalSales = dashboard.revenue
                )

                val chartData = data.weeklyChart.map { item ->
                    ChartDataPoint(time = item.label, value = item.amount)
                }

                val revenueComparison = RevenueComparison(
                    transferAmount = data.revenueComparison.transfer,
                    cashAmount = data.revenueComparison.cash
                )

                val productStats = data.productStats.map { item ->
                    ProductStatsData(
                        name = item.name,
                        amount = item.amount,
                        quantity = item.salesCount,
                        progress = item.percentage
                    )
                }

                val totalProductSalesInPeriod = productStats.sumOf { it.amount }

                val bestSellers = data.bestSeller.map { item ->
                    BestSellerData(
                        productName = item.productName,
                        totalSales = item.totalSales,
                        salesCount = item.salesCount,
                        imageUrl = item.imageUrl?.takeIf { it.isNotBlank() },
                        colorHex = item.colorHex?.takeIf { it.isNotBlank() },
                        rank = item.rank
                    )
                }

                Result.success(
                    GraphDashboardResult(
                        summary = summary,
                        chartData = chartData,
                        revenueComparison = revenueComparison,
                        productStats = productStats,
                        totalProductSalesInPeriod = totalProductSalesInPeriod,
                        bestSellers = bestSellers,
                        growthPercentage = data.growthPercentage
                    )
                )
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

    private fun formatDateForApi(millis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
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
