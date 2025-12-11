package com.indybrain.indypos_Android.presentation.orderdetail

import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity

data class OrderDetailUiState(
    val order: OrderEntity? = null,
    val orderItems: List<OrderItemEntity> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCancelling: Boolean = false
)


