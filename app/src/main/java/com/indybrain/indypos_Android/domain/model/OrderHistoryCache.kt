package com.indybrain.indypos_Android.domain.model

import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity

data class OrderTabBucket(
    val orders: List<OrderEntity> = emptyList(),
    val itemsByOrderId: Map<String, List<OrderItemEntity>> = emptyMap()
)

data class OrderHistoryCache(
    val completed: OrderTabBucket = OrderTabBucket(),
    val cancelled: OrderTabBucket = OrderTabBucket()
)
