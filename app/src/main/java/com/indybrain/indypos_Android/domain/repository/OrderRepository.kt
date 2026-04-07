package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.domain.model.OrderHistoryCache
import com.indybrain.indypos_Android.domain.model.OrderListPageInfo
import com.indybrain.indypos_Android.domain.model.OrderListQuery
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrderHistoryCache(): Flow<Result<OrderHistoryCache>>

    /** ล้างรายการหน้า Order history ทั้งสองแท็บ (ใช้เมื่อเปลี่ยนฟิลเตอร์/เรียง) */
    fun clearOrderHistoryCaches()
    /** One-shot get of all orders (e.g. after refresh to avoid race with order items). */
    suspend fun getOrdersSync(): Result<List<OrderEntity>>

    /**
     * Replace in-memory list with page 1 for the given query (order history screen).
     */
    suspend fun refreshOrders(query: OrderListQuery): Result<OrderListPageInfo>

    /**
     * Append next page for the same tab/sort/dates as [query] (page field must be next page index).
     */
    suspend fun loadMoreOrders(query: OrderListQuery): Result<OrderListPageInfo>

    /**
     * GET `/orders/{orderId}` and merge into cache for detail screen.
     */
    suspend fun fetchOrderDetail(orderId: String): Result<Unit>
    
    /**
     * Refresh orders using the new list endpoint (non-paginated)
     * Used by HomeScreen
     */
    suspend fun refreshOrdersList()
    
    /**
     * Total sales amount for today's orders (local time).
     */
    suspend fun getTodaySales(): Double
    
    /**
     * Total number of orders created today (local time).
     */
    suspend fun getTodayOrderCount(): Int
    
    /**
     * Number of cancelled orders created today (local time).
     */
    suspend fun getTodayCancelledOrderCount(): Int
    
    /**
     * Total cost of goods sold for today's orders, based on unitCost * quantity.
     */
    suspend fun getTodayCostOfExpenses(): Double
    
    suspend fun getOrderById(orderId: String): OrderEntity?
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>
    suspend fun updateOrderStatus(orderId: String, status: Int): Result<OrderEntity>

    /**
     * Top-selling product for today (by quantity).
     * Returns (productName, quantity, amount) or null if no items today.
     */
    suspend fun getTodayTopProduct(): Triple<String, Int, Double>?
}

