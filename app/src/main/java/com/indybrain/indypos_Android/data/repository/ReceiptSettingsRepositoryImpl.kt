package com.indybrain.indypos_Android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.indybrain.indypos_Android.core.utils.ImageUtils
import com.indybrain.indypos_Android.data.local.dao.ReceiptSettingsDao
import com.indybrain.indypos_Android.data.local.entity.ReceiptSettingsEntity
import com.indybrain.indypos_Android.domain.model.PromptPayType
import com.indybrain.indypos_Android.domain.repository.ReceiptSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptSettingsRepositoryImpl @Inject constructor(
    private val receiptSettingsDao: ReceiptSettingsDao,
    @ApplicationContext private val context: Context
) : ReceiptSettingsRepository {
    
    companion object {
        private const val SETTINGS_ID = "default_receipt_settings"
        private const val SHOP_LOGO_DIR = "shop_logos"
    }
    
    override fun getReceiptSettings(): Flow<ReceiptSettingsEntity?> {
        return receiptSettingsDao.getFirst()
    }
    
    override suspend fun getReceiptSettingsSync(): ReceiptSettingsEntity? {
        return receiptSettingsDao.getFirstSync()
    }
    
    override suspend fun updatePrintShopLogo(enabled: Boolean): Result<Unit> {
        return updateSetting { it.copy(printShopLogo = enabled) }
    }
    
    override suspend fun updatePrintAfterFinish(enabled: Boolean): Result<Unit> {
        return updateSetting { it.copy(printAfterFinish = enabled) }
    }
    
    override suspend fun updateShowQRCode(enabled: Boolean): Result<Unit> {
        return updateSetting { it.copy(showQRCode = enabled) }
    }
    
    override suspend fun updateOpenCashDrawer(enabled: Boolean): Result<Unit> {
        return updateSetting { it.copy(openCashDrawer = enabled) }
    }
    
    override suspend fun updatePaperSize(size: String): Result<Unit> {
        return updateSetting { it.copy(paperSize = size) }
    }
    
    override suspend fun updateFooter(footer: String): Result<Unit> {
        return updateSetting { it.copy(footer = footer) }
    }
    
    override suspend fun updateShopLogoImage(imageUri: Uri?): Result<String?> {
        return try {
            if (imageUri == null) {
                // Remove image
                val currentSettings = getReceiptSettingsSync() ?: return Result.failure(Exception("ไม่พบการตั้งค่า"))
                val oldImagePath = currentSettings.shopLogoImagePath
                if (oldImagePath != null) {
                    val oldFile = File(context.filesDir, oldImagePath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }
                val result = updateSetting { it.copy(shopLogoImagePath = null) }
                if (result.isSuccess) {
                    Result.success(null)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("ไม่สามารถลบรูปภาพได้"))
                }
            } else {
                // Save new image
                val logoDir = File(context.filesDir, SHOP_LOGO_DIR)
                if (!logoDir.exists()) {
                    logoDir.mkdirs()
                }
                
                // Resize image to reasonable size for receipt printing
                val resizedBitmap = ImageUtils.resizeImage(
                    imageUri = imageUri,
                    targetWidth = 200,
                    targetHeight = 200,
                    context = context
                ) ?: return Result.failure(Exception("ไม่สามารถประมวลผลรูปภาพได้"))
                
                // Save to file
                val imageFile = File(logoDir, "shop_logo_${System.currentTimeMillis()}.jpg")
                val saved = ImageUtils.saveBitmapToFile(resizedBitmap, imageFile, quality = 85)
                
                if (!saved) {
                    resizedBitmap.recycle()
                    return Result.failure(Exception("ไม่สามารถบันทึกไฟล์รูปภาพได้"))
                }
                
                // Delete old image if exists
                val currentSettings = getReceiptSettingsSync()
                currentSettings?.shopLogoImagePath?.let { oldPath ->
                    val oldFile = File(context.filesDir, oldPath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }
                
                val relativePath = "${SHOP_LOGO_DIR}/${imageFile.name}"
                resizedBitmap.recycle()
                
                val result = updateSetting { it.copy(shopLogoImagePath = relativePath) }
                if (result.isSuccess) {
                    Result.success(relativePath)
                } else {
                    imageFile.delete()
                    Result.failure(result.exceptionOrNull() ?: Exception("ไม่สามารถบันทึกการตั้งค่าได้"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getShopLogoBitmap(): Bitmap? {
        val settings = getReceiptSettingsSync() ?: return null
        val imagePath = settings.shopLogoImagePath ?: return null
        
        val imageFile = File(context.filesDir, imagePath)
        if (!imageFile.exists()) return null
        
        return try {
            BitmapFactory.decodeFile(imageFile.absolutePath)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun updatePromptPayType(type: PromptPayType): Result<Unit> {
        return updateSetting { it.copy(promptPayType = type.value) }
    }
    
    override suspend fun updatePromptPayIdentifier(identifier: String): Result<Unit> {
        return updateSetting { it.copy(promptPayIdentifier = identifier) }
    }
    
    override suspend fun updateTaxIdentificationNumber(enabled: Boolean): Result<Unit> {
        return updateSetting { it.copy(taxIdentificationNumber = enabled) }
    }
    
    override suspend fun updateTINNumber(tinNumber: String): Result<Unit> {
        return updateSetting { it.copy(tinNumber = tinNumber) }
    }
    
    override suspend fun initializeDefaultSettings() {
        val existing = getReceiptSettingsSync()
        if (existing != null) return
        
        val defaultSettings = ReceiptSettingsEntity(
            id = SETTINGS_ID,
            printShopLogo = false,
            printAfterFinish = false,
            showQRCode = false,
            openCashDrawer = false,
            paperSize = "58",
            footer = null,
            shopLogoImagePath = null,
            promptPayType = null,
            promptPayIdentifier = null,
            taxIdentificationNumber = false,
            tinNumber = null,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        receiptSettingsDao.insert(defaultSettings)
    }
    
    private suspend fun updateSetting(update: (ReceiptSettingsEntity) -> ReceiptSettingsEntity): Result<Unit> {
        return try {
            val current = getReceiptSettingsSync()
                ?: return Result.failure(Exception("ไม่พบการตั้งค่า กรุณาเริ่มต้นการตั้งค่าก่อน"))
            
            val updated = update(current).copy(updatedAt = Date())
            receiptSettingsDao.insertOrUpdate(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

