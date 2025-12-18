package com.indybrain.indypos_Android.presentation.orderproduct

import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.domain.model.GroupedCartItem
import com.indybrain.indypos_Android.presentation.discount.DiscountModel
import com.indybrain.indypos_Android.presentation.discount.DiscountType

data class OrderProductUiState(
    val groupedItems: List<GroupedCartItem> = emptyList(),
    val cartItems: List<CartItemEntity> = emptyList(), // Keep for backward compatibility with order creation
    val isLoading: Boolean = false,
    val selectedPaymentType: PaymentType = PaymentType.CASH,
    val discountAmount: Double = 0.0,
    val discountType: DiscountType? = null,
    val discountValue: Double = 0.0,
    val errorMessage: String? = null
)

enum class PaymentType(val displayName: String) {
    CASH("จ่ายเงินสด"),
    TRANSFER("โอนเงิน")
}







