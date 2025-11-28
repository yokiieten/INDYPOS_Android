package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.OrdersResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for orders endpoints
 */
interface OrdersApi {
    /**
     * Get orders endpoint
     */
    @GET("protected/indypos/orders/paginated")
    suspend fun getOrders(
        @Query("limit") limit: Int,
        @Query("page") page: Int,
        @Query("status") status: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): OrdersResponseDto
}

