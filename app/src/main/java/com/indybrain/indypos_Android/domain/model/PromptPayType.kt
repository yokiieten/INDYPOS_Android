package com.indybrain.indypos_Android.domain.model

/**
 * Enum for PromptPay identifier types
 */
enum class PromptPayType(val value: String, val displayName: String) {
    PHONE_NUMBER("phoneNumber", "เบอร์โทรศัพท์"),
    NATIONAL_ID("nationalID", "เลขบัตรประชาชน"),
    E_WALLET("eWallet", "e-Wallet ID");
    
    companion object {
        fun fromString(value: String?): PromptPayType {
            return values().find { it.value == value } ?: PHONE_NUMBER
        }
    }
}

