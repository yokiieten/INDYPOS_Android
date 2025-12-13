package com.indybrain.indypos_Android.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.indybrain.indypos_Android.domain.model.PromptPayType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for receipt settings operations
 */
interface ReceiptSettingsRepository {
    /**
     * Get receipt settings
     */
    fun getReceiptSettings(): Flow<com.indybrain.indypos_Android.data.local.entity.ReceiptSettingsEntity?>
    
    /**
     * Get receipt settings synchronously
     */
    suspend fun getReceiptSettingsSync(): com.indybrain.indypos_Android.data.local.entity.ReceiptSettingsEntity?
    
    /**
     * Update print shop logo setting
     */
    suspend fun updatePrintShopLogo(enabled: Boolean): Result<Unit>
    
    /**
     * Update print after finish setting
     */
    suspend fun updatePrintAfterFinish(enabled: Boolean): Result<Unit>
    
    /**
     * Update show QR code setting
     */
    suspend fun updateShowQRCode(enabled: Boolean): Result<Unit>
    
    /**
     * Update open cash drawer setting
     */
    suspend fun updateOpenCashDrawer(enabled: Boolean): Result<Unit>
    
    /**
     * Update paper size
     */
    suspend fun updatePaperSize(size: String): Result<Unit>
    
    /**
     * Update footer text
     */
    suspend fun updateFooter(footer: String): Result<Unit>
    
    /**
     * Update shop logo image
     */
    suspend fun updateShopLogoImage(imageUri: Uri?): Result<String?> // Returns image path
    
    /**
     * Get shop logo bitmap
     */
    suspend fun getShopLogoBitmap(): Bitmap?
    
    /**
     * Update PromptPay type
     */
    suspend fun updatePromptPayType(type: PromptPayType): Result<Unit>
    
    /**
     * Update PromptPay identifier
     */
    suspend fun updatePromptPayIdentifier(identifier: String): Result<Unit>
    
    /**
     * Update tax identification number setting
     */
    suspend fun updateTaxIdentificationNumber(enabled: Boolean): Result<Unit>
    
    /**
     * Update TIN number
     */
    suspend fun updateTINNumber(tinNumber: String): Result<Unit>
    
    /**
     * Initialize default settings if not exists
     */
    suspend fun initializeDefaultSettings()
}

