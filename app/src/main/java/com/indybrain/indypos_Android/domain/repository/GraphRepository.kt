package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.presentation.graph.BestSellerData
import com.indybrain.indypos_Android.presentation.graph.ChartDataPoint
import com.indybrain.indypos_Android.presentation.graph.GraphSummary
import com.indybrain.indypos_Android.presentation.graph.RevenueComparison
import com.indybrain.indypos_Android.presentation.graph.ProductStatsData
import com.indybrain.indypos_Android.presentation.graph.TimePeriod

/**
 * Result wrapper for Graph Dashboard API
 */
data class GraphDashboardResult(
    val summary: GraphSummary,
    val chartData: List<ChartDataPoint>,
    val revenueComparison: RevenueComparison,
    val productStats: List<ProductStatsData>,
    val totalProductSalesInPeriod: Double,
    val bestSellers: List<BestSellerData>,
    val growthPercentage: Double = 0.0
)

/**
 * Repository for Graph Dashboard data
 */
interface GraphRepository {
    /**
     * Fetch graph dashboard data from API
     *
     * @param filterType today | week | month | custom
     * @param startDateMillis Required when filterType=Custom (start of day)
     * @param endDateMillis Required when filterType=Custom (end of day)
     */
    suspend fun getDashboard(
        filterType: TimePeriod,
        startDateMillis: Long? = null,
        endDateMillis: Long? = null
    ): Result<GraphDashboardResult>
}
