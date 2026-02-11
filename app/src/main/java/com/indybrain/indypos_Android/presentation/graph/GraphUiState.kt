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
    val customStartDateMillis: Long? = null,
    val customEndDateMillis: Long? = null,
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

enum class TimePeriod(val stringResId: Int) {
    Today(com.indybrain.indypos_Android.R.string.graph_period_today),
    Week(com.indybrain.indypos_Android.R.string.graph_period_week),
    Month(com.indybrain.indypos_Android.R.string.graph_period_month),
    Custom(com.indybrain.indypos_Android.R.string.graph_period_custom)
}

data class RevenueComparison(
    val transferAmount: Double = 0.0,
    val cashAmount: Double = 0.0
)

data class ProductStatsData(
    val name: String,
    val amount: Double,
    val quantity: Int = 0,
    val progress: Double = 0.0 // 0.0 - 1.0
)

data class BestSellerData(
    val productName: String,
    val totalSales: Double,
    val salesCount: Int,
    val imageUrl: String? = null,
    val colorHex: String? = null,
    val rank: Int = 1
)
