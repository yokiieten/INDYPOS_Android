package com.indybrain.indypos_Android.domain.model

/**
 * PaymentType enum matching Core Data structure
 * 0=cash, 1=transfer, 2=card, 3=qrCode
 */
enum class PaymentType(val code: Int) {
    CASH(0),
    TRANSFER(1),
    CARD(2),
    QR_CODE(3);
    
    companion object {
        fun fromCode(code: Int): PaymentType {
            return entries.firstOrNull { it.code == code } ?: CASH
        }
    }
}

