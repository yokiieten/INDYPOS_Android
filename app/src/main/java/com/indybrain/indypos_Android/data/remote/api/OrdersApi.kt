package com.indybrain.indypos_Android.data.remote.api

import com.indybrain.indypos_Android.data.remote.dto.CreateOrderRequestDto
import com.indybrain.indypos_Android.data.remote.dto.CreateOrderResponseDto
import com.indybrain.indypos_Android.data.remote.dto.OrderDetailResponseDto
import com.indybrain.indypos_Android.data.remote.dto.OrdersListResponseWrapper
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
     * Paginated order history: tab, server-side sort, date range.
     * @param tab `completed` | `cancelled` or omit for all (not used by current app tabs)
     * @param sortBy `latest` | `oldest` | `highestAmount`
     */
    @GET("protected/indypos/orders/paginated")
    suspend fun getOrders(
        @Query("tab") tab: String? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("status") status: Int? = null
    ): OrdersResponseDto

    @GET("protected/indypos/orders/{orderId}")
    suspend fun getOrderById(
        @Path("orderId") orderId: String
    ): OrderDetailResponseDto
    
    /**
     * Get orders list endpoint (non-paginated) - used by HomeScreen
     */
    @GET("protected/indypos/orders")
    suspend fun getOrdersList(): OrdersListResponseWrapper
    
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

