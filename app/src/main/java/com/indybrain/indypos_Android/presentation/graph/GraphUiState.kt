package com.indybrain.indypos_Android.presentation.graph

/**
 * UI data holder for the graph screen
 */
data class GraphUiState(
    val isLoading: Boolean = false,
    val selectedPeriod: TimePeriod = TimePeriod.Today,
    val summary: GraphSummary = GraphSummary(),
    val chartData: List<ChartDataPoint> = emptyList(),
    val revenueComparison: RevenueComparison = RevenueComparison(),
    val productStats: List<ProductStatsData> = emptyList(),
    val bestSellers: List<BestSellerData> = emptyList(),
    val errorMessage: String? = null
)

data class GraphSummary(
    val todaySales: Double = 0.0,
    val costOfExpenses: Double = 0.0,
    val ordersToday: Int = 0,
    val cancelledOrders: Int = 0,
    val totalSales: Double = 0.0
)

data class ChartDataPoint(
    val time: String,
    val value: Double
)

enum class TimePeriod(val displayName: String) {
    Today("วันนี้"),
    Week("1 สัปดาห์"),
    Month("1 เดือน"),
    Custom("กำหนดเอง")
}

data class RevenueComparison(
    val transferAmount: Double = 0.0,
    val cashAmount: Double = 0.0
)

data class ProductStatsData(
    val name: String,
    val amount: Double,
    val progress: Double // 0.0 - 1.0
)

data class BestSellerData(
    val productName: String,
    val totalSales: Double,
    val salesCount: Int,
    val imageUrl: String? = null,
    val colorHex: String? = null,
    val rank: Int = 1
)

