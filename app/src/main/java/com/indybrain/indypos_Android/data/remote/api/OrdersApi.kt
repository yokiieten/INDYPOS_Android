package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.CreateOrderRequestDto
import com.indybrain.indypos_Android.data.remote.dto.CreateOrderResponseDto
import com.indybrain.indypos_Android.data.remote.dto.OrdersResponseDto
import com.indybrain.indypos_Android.data.remote.dto.UpdateOrderStatusResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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
    
    /**
     * Create order endpoint
     */
    @POST("protected/indypos/orders")
    suspend fun createOrder(
        @Body request: CreateOrderRequestDto
    ): CreateOrderResponseDto
    
    /**
     * Update order status endpoint
     */
    @PUT("protected/indypos/orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: String,
        @Body request: UpdateOrderStatusRequestDto
    ): UpdateOrderStatusResponseDto
}

data class UpdateOrderStatusRequestDto(
    val status: Int
)
