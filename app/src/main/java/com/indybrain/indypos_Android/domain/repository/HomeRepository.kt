package com.indybrain.indypos_Android.domain.repository

data class TodaySalesResult(
    val todaysSales: Double,
    val ordersToday: Int,
    val topProductName: String,
    val topProductQuantity: Int,
    val topProductAmount: Double
)

interface HomeRepository {
    /**
     * Fetch today's sales summary from API.
     *
     * @param date Optional date (YYYY-MM-DD). Default: today
     * @param timezone IANA timezone, default Asia/Bangkok
     * @return Result with TodaySalesResult on success
     */
    suspend fun getTodaySales(
        date: String? = null,
        timezone: String = "Asia/Bangkok"
    ): Result<TodaySalesResult>
}
