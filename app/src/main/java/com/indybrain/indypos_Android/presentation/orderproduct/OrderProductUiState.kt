package com.indybrain.indypos_Android.presentation.orderproduct

import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.presentation.discount.DiscountModel
import com.indybrain.indypos_Android.presentation.discount.DiscountType

data class OrderProductUiState(
    val cartItems: List<CartItemEntity> = emptyList(),
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







