package com.indybrain.indypos_Android.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Graph Dashboard API response
 * GET /api/v1/protected/indypos/graph/dashboard
 */

data class GraphDashboardResponseDto(
    val dashboard: GraphDashboardDataDto,
    @SerializedName("weekly_chart")
    val weeklyChart: List<WeeklyChartItemDto>,
    @SerializedName("revenue_comparison")
    val revenueComparison: RevenueComparisonDto,
    @SerializedName("product_stats")
    val productStats: List<ProductStatsItemDto>,
    @SerializedName("best_seller")
    val bestSeller: List<BestSellerItemDto>,
    @SerializedName("growth_percentage")
    val growthPercentage: Double = 0.0,
    @SerializedName("date_range")
    val dateRange: DateRangeDto? = null
)

data class GraphDashboardDataDto(
    val revenue: Double = 0.0,
    val expenses: Double = 0.0,
    val profit: Double = 0.0,
    @SerializedName("total_orders")
    val totalOrders: Int = 0,
    @SerializedName("successful_orders")
    val successfulOrders: Int = 0,
    @SerializedName("cancelled_orders")
    val cancelledOrders: Int = 0
)

data class WeeklyChartItemDto(
    val label: String,
    val amount: Double
)

data class RevenueComparisonDto(
    val transfer: Double = 0.0,
    val cash: Double = 0.0
)

data class ProductStatsItemDto(
    val name: String,
    val amount: Double,
    val percentage: Double = 0.0,
    val rank: Int = 0,
    @SerializedName("sales_count")
    val salesCount: Int = 0
)

data class BestSellerItemDto(
    @SerializedName("product_name")
    val productName: String,
    @SerializedName("total_sales")
    val totalSales: Double,
    @SerializedName("sales_count")
    val salesCount: Int,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("color_hex")
    val colorHex: String? = null,
    val rank: Int = 1
)

data class DateRangeDto(
    val start: String,
    val end: String,
    @SerializedName("filter_type")
    val filterType: String
)
