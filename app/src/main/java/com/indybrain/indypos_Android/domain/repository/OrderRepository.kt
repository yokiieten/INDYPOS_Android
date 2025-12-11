package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<Result<List<OrderEntity>>>
    suspend fun refreshOrders()
    suspend fun getTodaySales(): Double
    suspend fun getTodayOrderCount(): Int
    suspend fun getOrderById(orderId: String): OrderEntity?
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>
    suspend fun updateOrderStatus(orderId: String, status: Int): Result<OrderEntity>
}

