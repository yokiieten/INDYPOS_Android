package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.*
import com.indybrain.indypos_Android.data.remote.dto.AddonDto
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import com.indybrain.indypos_Android.domain.repository.AddonSyncStatistics
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

class AddonRepositoryImpl @Inject constructor(
    private val productsApi: ProductsApi,
    private val addonDao: AddonDao,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : AddonRepository {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    override fun getAllAddonsForManagementFlow(): Flow<List<com.indybrain.indypos_Android.data.local.entity.AddonEntity>> {
        return addonDao.getAllAddonsForManagementFlow()
    }
    
    override suspend fun getAddonById(id: String): com.indybrain.indypos_Android.data.local.entity.AddonEntity? {
        return addonDao.getAddonById(id)
    }
    
    override suspend fun getDeletedAddons(): List<com.indybrain.indypos_Android.data.local.entity.AddonEntity> {
        return addonDao.getDeletedAddons()
    }
    
    override suspend fun fetchAndSyncAddons(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception("ไม่มีอินเทอร์เน็ต"))
            }
            
            val response = productsApi.getAddons()
            if (response.status != 200 || response.data == null) {
                return Result.failure(Exception(response.message ?: "Failed to fetch addons"))
            }
            
            // Convert and save addons
            val addons = response.data.map { ProductMapper.toEntity(it) }
            // Use REPLACE strategy to update existing addons
            addonDao.insertAll(addons)
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการดึงข้อมูล"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดที่ไม่คาดคิด"))
        }
    }
    
    override suspend fun toggleAddonStatus(addonId: String, newStatus: Boolean): Result<com.indybrain.indypos_Android.data.local.entity.AddonEntity> {
        return try {
            val existing = addonDao.getAddonById(addonId)
                ?: return Result.failure(Exception("ไม่พบ Addon ที่ต้องการ"))
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = ToggleAddonStatusRequestDto(status = newStatus)
                    val response = productsApi.toggleAddonStatus(addonId, request)
                    
                    if (response.status == 200 && response.data != null) {
                        // API success - convert to entity and save to Room
                        val addonEntity = ProductMapper.toEntity(response.data)
                        addonDao.insert(addonEntity)
                        Result.success(addonEntity)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะ"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        404 -> "ไม่พบ Addon ที่ต้องการ"
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะ"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - update in Room only (for sync later)
                val updatedAt = Date()
                addonDao.updateAddonStatus(addonId, newStatus, updatedAt)
                
                // Return updated entity
                val updatedEntity = existing.copy(
                    isActive = newStatus,
                    updatedAt = updatedAt,
                    isSynced = false
                )
                Result.success(updatedEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะ"))
        }
    }
    
    override suspend fun deleteAddon(addonId: String): Result<Unit> {
        return try {
            val existing = addonDao.getAddonById(addonId)
                ?: return Result.failure(Exception("ไม่พบ Addon ที่ต้องการลบ"))
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.deleteAddon(addonId)
                    
                    if (response.status == 200) {
                        // API success - permanently delete from Room
                        addonDao.permanentlyDeleteAddon(addonId)
                        Result.success(Unit)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบ"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        404 -> "ไม่พบ Addon ที่ต้องการลบ"
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการลบ"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - soft delete in Room only (for sync later)
                val updatedAt = Date()
                addonDao.softDeleteAddon(addonId, updatedAt)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบ"))
        }
    }
    
    override suspend fun deleteMultipleAddons(addonIds: List<String>): Result<Unit> {
        return try {
            if (addonIds.isEmpty()) {
                return Result.failure(Exception("กรุณาเลือก Addon ที่ต้องการลบ"))
            }
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = DeleteAddonsRequestDto(addonIds = addonIds)
                    val response = productsApi.deleteMultipleAddons(request)
                    
                    if (response.status == 200) {
                        // API success - permanently delete from Room
                        addonIds.forEach { addonDao.permanentlyDeleteAddon(it) }
                        Result.success(Unit)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบ"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการลบ"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - soft delete in Room only (for sync later)
                val updatedAt = Date()
                addonIds.forEach { addonDao.softDeleteAddon(it, updatedAt) }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบ"))
        }
    }
    
    override suspend fun permanentlyDeleteAddon(addonId: String) {
        addonDao.permanentlyDeleteAddon(addonId)
    }
    
    override suspend fun getSyncStatistics(): AddonSyncStatistics {
        val allAddons = addonDao.getAllAddons()
        val deletedAddons = addonDao.getDeletedAddons()
        
        val total = allAddons.size + deletedAddons.size
        val synced = allAddons.count { it.isSynced }
        val unsynced = allAddons.count { !it.isSynced }
        val deleted = deletedAddons.size
        
        return AddonSyncStatistics(
            total = total,
            synced = synced,
            unsynced = unsynced,
            deleted = deleted
        )
    }
    
    override suspend fun syncPendingAddons(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception("ไม่มีอินเทอร์เน็ต"))
            }
            
            // Get pending addons (unsynced) and deleted addons
            val allAddons = addonDao.getAllAddons()
            val pendingAddons = allAddons.filter { !it.isSynced }
            val deletedAddons = addonDao.getDeletedAddons()
            
            if (pendingAddons.isEmpty() && deletedAddons.isEmpty()) {
                return Result.success(Unit)
            }
            
            // Prepare sync items
            val syncItems = (pendingAddons + deletedAddons).map { addon ->
                SyncAddonItemDto(
                    id = addon.id,
                    name = addon.name,
                    price = addon.price,
                    sortOrder = addon.sortOrder ?: 1,
                    isActive = addon.isActive,
                    isSynced = addon.isSynced,
                    isDeletedLocally = addon.isDeletedLocally,
                    createdAt = dateFormatter.format(addon.createdAt),
                    updatedAt = dateFormatter.format(addon.updatedAt)
                )
            }
            
            val request = SyncAddonsRequestDto(addons = syncItems)
            val response = productsApi.syncAddons(request)
            
            if (response.status != 200 || response.data == null) {
                return Result.failure(Exception(response.message ?: "เกิดข้อผิดพลาดในการ sync"))
            }
            
            // Process sync results
            val results = response.data
            val serverSideAddons = results.mapNotNull { it.serverData }
            
            // Save server data to Room
            if (serverSideAddons.isNotEmpty()) {
                val addonEntities = serverSideAddons.map { ProductMapper.toEntity(it) }
                addonDao.insertAll(addonEntities)
            }
            
            // Mark as synced for successful items
            val successfulIds = results
                .filter { (it.status ?: "").lowercase() == "success" || (it.status ?: "").lowercase() == "conflict" }
                .mapNotNull { it.id }
            
            if (successfulIds.isNotEmpty()) {
                addonDao.markAddonsAsSynced(successfulIds)
            }
            
            // Permanently delete items that server says should be deleted
            val idsToHardDelete = results
                .filter { it.shouldDelete == true }
                .mapNotNull { it.id }
            
            idsToHardDelete.forEach { addonDao.permanentlyDeleteAddon(it) }
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการ sync"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการ sync"))
        }
    }
}

