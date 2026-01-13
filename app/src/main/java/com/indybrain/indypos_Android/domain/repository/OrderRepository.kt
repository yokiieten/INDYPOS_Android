package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<Result<List<OrderEntity>>>
    suspend fun refreshOrders()
    
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
}

