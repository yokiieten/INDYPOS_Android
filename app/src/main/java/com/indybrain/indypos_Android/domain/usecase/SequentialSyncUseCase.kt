package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Use case for sequential sync operations
 * Syncs in order: Categories -> Addons -> AddonGroups -> Products
 */
class SequentialSyncUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val addonRepository: AddonRepository,
    private val addonGroupRepository: AddonGroupRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) {
    
    data class SyncError(
        val itemName: String,
        val errorMessage: String,
        val localizedMessage: String
    )
    
    data class SyncResult(
        val success: Boolean,
        val message: String,
        val errors: List<SyncError> = emptyList()
    )
    
    /**
     * Perform sequential sync in order:
     * 1. Categories
     * 2. Addons
     * 3. AddonGroups
     * 4. Products
     */
    suspend operator fun invoke(): SyncResult {
        // Check network connectivity
        if (!networkConnectivityChecker.isConnected()) {
            return SyncResult(
                success = false,
                message = "ไม่มีอินเทอร์เน็ต",
                errors = emptyList()
            )
        }
        
        val allErrors = mutableListOf<SyncError>()
        
        // Step 1: Sync Categories
        val categoriesResult = syncCategories()
        allErrors.addAll(categoriesResult.errors)
        if (!categoriesResult.success) {
            return SyncResult(
                success = false,
                message = categoriesResult.message,
                errors = allErrors
            )
        }
        
        // Step 2: Sync Addons
        val addonsResult = syncAddons()
        allErrors.addAll(addonsResult.errors)
        if (!addonsResult.success) {
            return SyncResult(
                success = false,
                message = addonsResult.message,
                errors = allErrors
            )
        }
        
        // Step 3: Sync AddonGroups
        val addonGroupsResult = syncAddonGroups()
        allErrors.addAll(addonGroupsResult.errors)
        if (!addonGroupsResult.success) {
            return SyncResult(
                success = false,
                message = addonGroupsResult.message,
                errors = allErrors
            )
        }
        
        // Step 4: Sync Products
        val productsResult = syncProducts()
        allErrors.addAll(productsResult.errors)
        if (!productsResult.success) {
            return SyncResult(
                success = false,
                message = productsResult.message,
                errors = allErrors
            )
        }
        
        return SyncResult(
            success = allErrors.isEmpty(),
            message = if (allErrors.isEmpty()) {
                "Sync สำเร็จทั้งหมด"
            } else {
                "Sync เสร็จสิ้น แต่มีข้อผิดพลาดบางรายการ"
            },
            errors = allErrors
        )
    }
    
    private suspend fun syncCategories(): SyncResult {
        return try {
            val result = productRepository.syncCategories()
            
            if (result.isSuccess) {
                SyncResult(
                    success = true,
                    message = "Sync หมวดหมู่สำเร็จ"
                )
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาดในการ sync หมวดหมู่"
                SyncResult(
                    success = false,
                    message = errorMessage,
                    errors = listOf(
                        SyncError(
                            itemName = "",
                            errorMessage = errorMessage,
                            localizedMessage = parseError(errorMessage, "category")
                        )
                    )
                )
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "เกิดข้อผิดพลาดในการ sync หมวดหมู่"
            SyncResult(
                success = false,
                message = errorMessage,
                errors = listOf(
                    SyncError(
                        itemName = "",
                        errorMessage = errorMessage,
                        localizedMessage = parseError(errorMessage, "category")
                    )
                )
            )
        }
    }
    
    private suspend fun syncAddons(): SyncResult {
        return try {
            val result = addonRepository.syncPendingAddons()
            if (result.isSuccess) {
                SyncResult(
                    success = true,
                    message = "Sync ตัวเลือกเพิ่มเติมสำเร็จ"
                )
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาดในการ sync ตัวเลือกเพิ่มเติม"
                SyncResult(
                    success = false,
                    message = errorMessage,
                    errors = listOf(
                        SyncError(
                            itemName = "",
                            errorMessage = errorMessage,
                            localizedMessage = parseError(errorMessage, "addon")
                        )
                    )
                )
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "เกิดข้อผิดพลาดในการ sync ตัวเลือกเพิ่มเติม"
            SyncResult(
                success = false,
                message = errorMessage,
                errors = listOf(
                    SyncError(
                        itemName = "",
                        errorMessage = errorMessage,
                        localizedMessage = parseError(errorMessage, "addon")
                    )
                )
            )
        }
    }
    
    private suspend fun syncAddonGroups(): SyncResult {
        return try {
            val result = addonGroupRepository.syncAddonGroups()
            if (result.isSuccess) {
                SyncResult(
                    success = true,
                    message = "Sync กลุ่มตัวเลือกเพิ่มเติมสำเร็จ"
                )
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาดในการ sync กลุ่มตัวเลือกเพิ่มเติม"
                SyncResult(
                    success = false,
                    message = errorMessage,
                    errors = listOf(
                        SyncError(
                            itemName = "",
                            errorMessage = errorMessage,
                            localizedMessage = parseError(errorMessage, "addon_group")
                        )
                    )
                )
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "เกิดข้อผิดพลาดในการ sync กลุ่มตัวเลือกเพิ่มเติม"
            SyncResult(
                success = false,
                message = errorMessage,
                errors = listOf(
                    SyncError(
                        itemName = "",
                        errorMessage = errorMessage,
                        localizedMessage = parseError(errorMessage, "addon_group")
                    )
                )
            )
        }
    }
    
    private suspend fun syncProducts(): SyncResult {
        return try {
            val result = productRepository.syncProducts()
            
            if (result.isSuccess) {
                SyncResult(
                    success = true,
                    message = "Sync สินค้าสำเร็จ"
                )
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "เกิดข้อผิดพลาดในการ sync สินค้า"
                SyncResult(
                    success = false,
                    message = errorMessage,
                    errors = listOf(
                        SyncError(
                            itemName = "",
                            errorMessage = errorMessage,
                            localizedMessage = parseError(errorMessage, "product")
                        )
                    )
                )
            }
        } catch (e: Exception) {
            val errorMessage = e.message ?: "เกิดข้อผิดพลาดในการ sync สินค้า"
            SyncResult(
                success = false,
                message = errorMessage,
                errors = listOf(
                    SyncError(
                        itemName = "",
                        errorMessage = errorMessage,
                        localizedMessage = parseError(errorMessage, "product")
                    )
                )
            )
        }
    }
    
    private fun parseError(message: String, itemType: String): String {
        val lowercased = message.lowercase()
        
        // Check for free plan limit error
        if (lowercased.contains("free_plan_limit") || 
            (lowercased.contains("ครบ") && lowercased.contains("รายการ"))) {
            return when (itemType.lowercase()) {
                "category" -> "คุณใช้หมวดหมู่ครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                "addon" -> "คุณใช้ตัวเลือกเพิ่มเติมครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                "addon_group" -> "คุณใช้กลุ่มตัวเลือกเพิ่มเติมครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                "product" -> "คุณใช้สินค้าครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                else -> message
            }
        }
        
        // Check for other common errors
        return when {
            lowercased.contains("unauthorized") -> "กรุณาเข้าสู่ระบบใหม่"
            lowercased.contains("network") || lowercased.contains("internet") -> "กรุณาเชื่อมต่ออินเทอร์เน็ต"
            lowercased.contains("duplicate") -> when (itemType.lowercase()) {
                "category" -> "ชื่อหมวดหมู่ซ้ำ"
                "addon" -> "ชื่อตัวเลือกเพิ่มเติมซ้ำ"
                "addon_group" -> "ชื่อกลุ่มตัวเลือกเพิ่มเติมซ้ำ"
                "product" -> "ชื่อสินค้าหรือรหัสสินค้าซ้ำ"
                else -> "ข้อมูลซ้ำกัน"
            }
            else -> message
        }
    }
}

