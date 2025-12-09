package com.indybrain.indypos_Android.presentation.cashpayment

data class CashPaymentUiState(
    val enteredAmount: String = "",
    val receivedAmount: Double = 0.0,
    val isProcessingOrder: Boolean = false,
    val errorMessage: String? = null
)

