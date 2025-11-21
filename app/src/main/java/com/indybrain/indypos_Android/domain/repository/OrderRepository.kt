package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<Result<List<OrderEntity>>>
    suspend fun refreshOrders()
    suspend fun getTodaySales(): Double
    suspend fun getTodayOrderCount(): Int
}

