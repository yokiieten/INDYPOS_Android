package com.indybrain.indypos_Android.presentation.settings.receipt

import android.graphics.Bitmap
import com.indybrain.indypos_Android.domain.model.PromptPayType

/**
 * UI State for Receipt Settings Screen
 */
data class ReceiptSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    
    // Switch states
    val printShopLogo: Boolean = false,
    val printAfterFinish: Boolean = false,
    val showQRCode: Boolean = false,
    val openCashDrawer: Boolean = false,
    val taxIdentificationNumber: Boolean = false,
    
    // Paper size
    val paperSize: String = "58",
    
    // Footer
    val footer: String = "",
    
    // Shop logo
    val shopLogoBitmap: Bitmap? = null,
    
    // PromptPay
    val promptPayType: PromptPayType = PromptPayType.PHONE_NUMBER,
    val promptPayIdentifier: String = "",
    
    // TIN
    val tinNumber: String = "",
    
    // Validation states
    val promptPayIdentifierError: String? = null,
    val tinNumberError: String? = null,
    
    // Success/Error messages
    val successMessage: String? = null,
    val errorMessage: String? = null,
    
    // Has changes flag
    val hasChanges: Boolean = false,
    /** True when there are draft changes and validation allows save. */
    val canSave: Boolean = false
)

