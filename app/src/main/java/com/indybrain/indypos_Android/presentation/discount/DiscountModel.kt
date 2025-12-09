package com.indybrain.indypos_Android.presentation.discount

/**
 * Discount type enum
 */
enum class DiscountType(val title: String, val symbol: String, val placeholder: String) {
    PERCENTAGE("เปอร์เซ็นต์ (%)", "%", "0"),
    FIXED_AMOUNT("จำนวนเงิน (บาท)", "฿", "0")
}

/**
 * Discount model
 */
data class DiscountModel(
    val type: DiscountType,
    val value: Double
) {
    val displayText: String
        get() = when (type) {
            DiscountType.PERCENTAGE -> {
                val intValue = value.toInt()
                "ส่วนลด $intValue%"
            }
            DiscountType.FIXED_AMOUNT -> {
                val intValue = value.toInt()
                "ลด $intValue บาท"
            }
        }
    
    val previewText: String
        get() = when (type) {
            DiscountType.PERCENTAGE -> {
                val intValue = value.toInt()
                "$intValue%"
            }
            DiscountType.FIXED_AMOUNT -> {
                val intValue = value.toInt()
                "$intValue บาท"
            }
        }
    
    private fun formatCurrency(value: Double): String {
        val formatter = java.text.DecimalFormat("#,##0.00")
        return "฿${formatter.format(value)}"
    }
}

