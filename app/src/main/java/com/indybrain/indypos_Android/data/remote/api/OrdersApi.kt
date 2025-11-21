package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.OrdersResponseDto
import retrofit2.http.GET

/**
 * Retrofit API interface for orders endpoints
 */
interface OrdersApi {
    /**
     * Get orders endpoint
     */
    @GET("protected/indypos/orders")
    suspend fun getOrders(): OrdersResponseDto
}

