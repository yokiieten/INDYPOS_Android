package com.indybrain.indypos_Android.data.repository

import android.content.Context
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.SelectedAddonJunctionDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupAddonJunctionDao
import com.indybrain.indypos_Android.data.local.dao.CartDao
import com.indybrain.indypos_Android.data.local.dao.OrderAddonDao
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.*
import com.indybrain.indypos_Android.data.remote.dto.AddonDto
import com.indybrain.indypos_Android.data.remote.dto.DeleteAddonsResponseDto
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import com.indybrain.indypos_Android.domain.repository.AddonsPaginatedResult
import com.indybrain.indypos_Android.domain.repository.AddonSyncStatistics
import com.indybrain.indypos_Android.domain.repository.DeleteAddonsResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
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
    private val selectedAddonJunctionDao: SelectedAddonJunctionDao,
    private val addonGroupAddonJunctionDao: AddonGroupAddonJunctionDao,
    private val cartDao: CartDao,
    private val orderAddonDao: OrderAddonDao,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val languageLocalDataSource: LanguageLocalDataSource,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : AddonRepository {
    
    private fun getLocalizedString(resourceName: String, fallback: String): String {
        val resourceId = context.resources.getIdentifier(
            resourceName,
            "string",
            context.packageName
        )
        return if (resourceId != 0) {
            val localeCode = languageLocalDataSource.getLanguageLocale()
            val localizedContext = LocaleHelper.setLocale(context, localeCode)
            localizedContext.getString(resourceId)
        } else {
            fallback
        }
    }
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    override fun getAllAddonsForManagementFlow(): Flow<List<com.indybrain.indypos_Android.data.local.entity.AddonEntity>> {
        return addonDao.getAllAddonsForManagementFlow()
    }
    
    override suspend fun getAddonById(id: String): com.indybrain.indypos_Android.data.local.entity.AddonEntity? {
        return addonDao.getAddonById(id)
    }
    
    override suspend fun getAddonsPaginated(
        page: Int,
        limit: Int,
        search: String?
    ): Result<AddonsPaginatedResult> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception(getLocalizedString("error_no_internet", "ไม่มีการเชื่อมต่ออินเทอร์เน็ต")))
            }
            val searchParam = search?.takeIf { it.isNotBlank() }
            val response = productsApi.getAddonsPaginated(page = page, limit = limit, search = searchParam)
            val data = response.data
            if (data == null) {
                return Result.failure(Exception(response.message ?: "ไม่พบข้อมูล"))
            }
            val addons = (data.addons ?: emptyList()).map { ProductMapper.toEntity(it) }
            // Upsert addons to Room so AddEditAddonScreen can load by id
            addons.forEach { addonDao.insert(it) }
            val pagination = data.pagination
            val result = AddonsPaginatedResult(
                addons = addons,
                currentPage = pagination?.currentPage ?: page,
                totalCount = pagination?.totalCount ?: addons.size,
                totalPages = pagination?.totalPages ?: 1,
                hasNext = pagination?.hasNext ?: false,
                hasPrevious = pagination?.hasPrevious ?: false
            )
            Result.success(result)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()
            val errorMessage = parseApiErrorResponse(errorBody, e.code())
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการโหลด Addons"))
        }
    }
    
    override suspend fun createAddon(name: String, price: Double): Result<com.indybrain.indypos_Android.data.local.entity.AddonEntity> {
        return try {
            val addonEntity: com.indybrain.indypos_Android.data.local.entity.AddonEntity
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = CreateAddonRequestDto(
                        name = name.trim(),
                        price = price,
                        sortOrder = 1,
                        isActive = true
                    )
                    
                    val response = productsApi.createAddon(request)
                    
                    // Check if response indicates success (200 or 201) and has data
                    // Also check if message contains success keywords even if status is not 200/201
                    val isSuccessStatus = response.status == 200 || response.status == 201
                    val hasSuccessMessage = response.message?.contains("success", ignoreCase = true) == true
                        || response.message?.contains("created", ignoreCase = true) == true
                    
                    if ((isSuccessStatus && response.data != null) || (hasSuccessMessage && response.data != null)) {
                        // API success - convert to entity and save to Room
                        addonEntity = ProductMapper.toEntity(response.data)
                        addonDao.insert(addonEntity)
                        Result.success(addonEntity)
                    } else {
                        // API returned error status
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: response.error?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการสร้าง Addon"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    // Handle HTTP errors
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = when (e.code()) {
                        400 -> {
                            // Bad Request - parse error message
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        401 -> {
                            // Unauthorized - parse specific error
                            val parsed = parseApiErrorResponse(errorBody, e.code())
                            if (parsed.contains("Unauthorized", ignoreCase = true)) {
                                "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                            } else {
                                parsed
                            }
                        }
                        403 -> {
                            // Forbidden - Free plan limit exceeded or Access denied
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        409 -> {
                            // Conflict - Duplicate addon name
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        500 -> {
                            // Internal Server Error
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        else -> {
                            parseApiErrorResponse(errorBody, e.code())
                        }
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - create in Room only (for sync later)
                val now = Date()
                val localId = UUID.randomUUID().toString()
                addonEntity = com.indybrain.indypos_Android.data.local.entity.AddonEntity(
                    id = localId,
                    name = name.trim(),
                    price = price,
                    isActive = true,
                    isDeletedLocally = false,
                    isFromServer = false,
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = 1
                )
                addonDao.insert(addonEntity)
                Result.success(addonEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการสร้าง Addon"))
        }
    }
    
    override suspend fun updateAddon(addonId: String, name: String, price: Double): Result<com.indybrain.indypos_Android.data.local.entity.AddonEntity> {
        return try {
            val existing = addonDao.getAddonById(addonId)
                ?: return Result.failure(Exception("ไม่พบ Addon ที่ต้องการแก้ไข"))
            
            val addonEntity: com.indybrain.indypos_Android.data.local.entity.AddonEntity
            
            // Check if should update via API or locally only
            val shouldUpdateViaAPI = networkConnectivityChecker.isConnected() && 
                                    existing.isSynced && 
                                    existing.isFromServer
            
            if (shouldUpdateViaAPI) {
                // Has network and addon is synced - call API first
                try {
                    val request = UpdateAddonRequestDto(
                        name = name.trim(),
                        price = price,
                        sortOrder = existing.sortOrder ?: 1,
                        isActive = existing.isActive
                    )
                    
                    val response = productsApi.updateAddon(addonId, request)
                    
                    // Check if response indicates success (200 or 201) and has data
                    // Also check if message contains success keywords even if status is not 200/201
                    val isSuccessStatus = response.status == 200 || response.status == 201
                    val hasSuccessMessage = response.message?.contains("success", ignoreCase = true) == true
                        || response.message?.contains("updated", ignoreCase = true) == true
                    
                    if ((isSuccessStatus && response.data != null) || (hasSuccessMessage && response.data != null)) {
                        // API success - convert to entity and save to Room
                        addonEntity = ProductMapper.toEntity(response.data)
                        addonDao.insert(addonEntity)
                        Result.success(addonEntity)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: response.error?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการแก้ไข Addon"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    // Handle HTTP errors
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = when (e.code()) {
                        400 -> {
                            // Bad Request - parse error message
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        401 -> {
                            // Unauthorized - parse specific error
                            val parsed = parseApiErrorResponse(errorBody, e.code())
                            if (parsed.contains("Unauthorized", ignoreCase = true)) {
                                "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                            } else {
                                parsed
                            }
                        }
                        403 -> {
                            // Forbidden - Access denied
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        404 -> {
                            // Not Found - Addon not found
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        409 -> {
                            // Conflict - Duplicate addon name
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        500 -> {
                            // Internal Server Error
                            parseApiErrorResponse(errorBody, e.code())
                        }
                        else -> {
                            parseApiErrorResponse(errorBody, e.code())
                        }
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network or not synced - update in Room only (for sync later)
                val now = Date()
                addonEntity = existing.copy(
                    name = name.trim(),
                    price = price,
                    updatedAt = now,
                    isSynced = false
                )
                addonDao.insert(addonEntity)
                Result.success(addonEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการแก้ไข Addon"))
        }
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
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "Failed to fetch addons"))
            }
            
            // If status is 200, treat as success even if data is null or empty (new user might have no data)
            val addonsList = response.data ?: emptyList()
            // Convert and save addons
            if (addonsList.isNotEmpty()) {
                val addons = addonsList.map { ProductMapper.toEntity(it) }
                // Use REPLACE strategy to update existing addons
                addonDao.insertAll(addons)
            }
            // Remove local addons that are no longer in API (e.g. deleted on another device)
            val apiAddonIds = addonsList.map { it.id }.toSet()
            val existingAddonIds = addonDao.getAllAddons().map { it.id }.toSet()
            (existingAddonIds - apiAddonIds).forEach { id ->
                permanentlyDeleteAddon(id)
            }
            
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
                    // Handle HTTP errors
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = when (e.code()) {
                        400 -> parseApiErrorResponse(errorBody, e.code())
                        401 -> {
                            val parsed = parseApiErrorResponse(errorBody, e.code())
                            if (parsed.contains("Unauthorized", ignoreCase = true)) {
                                "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                            } else {
                                parsed
                            }
                        }
                        403 -> parseApiErrorResponse(errorBody, e.code())
                        404 -> parseApiErrorResponse(errorBody, e.code())
                        500 -> parseApiErrorResponse(errorBody, e.code())
                        else -> parseApiErrorResponse(errorBody, e.code())
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
                        // API success - ลบความสัมพันธ์ (junction, cart, order) ก่อน แล้วค่อยลบ addon
                        permanentlyDeleteAddon(addonId)
                        Result.success(Unit)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบ"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseAddonDeleteApiError(errorBody, e.code())
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
    
    override suspend fun deleteMultipleAddons(addonIds: List<String>): Result<DeleteAddonsResult> {
        return try {
            if (addonIds.isEmpty()) {
                return Result.failure(Exception(getLocalizedString("api_error_delete_addon_ids_required", "ต้องระบุรหัสแอดออนอย่างน้อย 1 รายการ")))
            }
            
            if (networkConnectivityChecker.isConnected()) {
                try {
                    val request = DeleteAddonsRequestDto(addonIds = addonIds)
                    val response = productsApi.deleteMultipleAddons(request)
                    
                    if (response.status == 200) {
                        val deletedIds = response.deletedIds.orEmpty()
                        val totalDeleted = response.data?.totalDeleted ?: response.count
                        val totalFailed = response.data?.totalFailed ?: response.failedDeletions.orEmpty().size
                        val errors = response.errors.orEmpty()
                        
                        deletedIds.forEach { id -> permanentlyDeleteAddon(id) }
                        
                        Result.success(
                            DeleteAddonsResult(
                                deletedCount = totalDeleted,
                                failedCount = totalFailed,
                                errors = errors
                            )
                        )
                    } else {
                        val errorMessage = response.message.takeIf { it.isNotBlank() }
                            ?: getLocalizedString("api_error_delete_addon_generic", "ไม่สามารถลบแอดออนได้ กรุณาลองใหม่อีกครั้ง")
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()
                    val errorMessage = parseAddonDeleteApiError(errorBody, e.code())
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val updatedAt = Date()
                addonIds.forEach { addonDao.softDeleteAddon(it, updatedAt) }
                Result.success(DeleteAddonsResult(deletedCount = addonIds.size))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: getLocalizedString("api_error_delete_addon_generic", "ไม่สามารถลบแอดออนได้ กรุณาลองใหม่อีกครั้ง")))
        }
    }
    
    override suspend fun permanentlyDeleteAddon(addonId: String) {
        // ลบความสัมพันธ์ทั้งหมดก่อน
        selectedAddonJunctionDao.deleteByAddonId(addonId)
        addonGroupAddonJunctionDao.deleteByAddonId(addonId)
        cartDao.deleteCartAddonsByAddonId(addonId)
        orderAddonDao.deleteOrderAddonsByAddonId(addonId)
        // แล้วค่อยลบ addon
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
            
            if (response.status != 200) {
                return Result.failure(Exception(response.message ?: "เกิดข้อผิดพลาดในการ sync"))
            }
            
            // Process sync results - handle null data as empty list
            val results = response.data ?: emptyList()
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
            
            idsToHardDelete.forEach { permanentlyDeleteAddon(it) }
            
            Result.success(Unit)
        } catch (e: HttpException) {
            // Handle HTTP errors
            val errorBody = e.response()?.errorBody()
            val errorMessage = when (e.code()) {
                400 -> parseApiErrorResponse(errorBody, e.code())
                401 -> {
                    val parsed = parseApiErrorResponse(errorBody, e.code())
                    if (parsed.contains("Unauthorized", ignoreCase = true)) {
                        "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                    } else {
                        parsed
                    }
                }
                403 -> parseApiErrorResponse(errorBody, e.code())
                500 -> parseApiErrorResponse(errorBody, e.code())
                else -> parseApiErrorResponse(errorBody, e.code())
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการ sync"))
        }
    }
    
    /**
     * Parse API error response for addon delete operations (single and batch).
     * Maps API errors to localized api_error_delete_addon_* strings.
     */
    private fun parseAddonDeleteApiError(errorBody: ResponseBody?, statusCode: Int): String {
        return try {
            if (errorBody == null) {
                return getLocalizedString("api_error_delete_addon_generic", "ไม่สามารถลบแอดออนได้ กรุณาลองใหม่อีกครั้ง")
            }
            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return getLocalizedString("api_error_delete_addon_generic", "ไม่สามารถลบแอดออนได้ กรุณาลองใหม่อีกครั้ง")
            }
            val errorResponse = gson.fromJson(errorJson, AddonErrorResponse::class.java)
            val errorText = errorResponse.error?.lowercase() ?: ""
            val messageText = errorResponse.message?.lowercase() ?: ""
            val combinedErrorText = "$errorText $messageText"
            
            when {
                combinedErrorText.contains("addon id is required") -> 
                    getLocalizedString("api_error_delete_addon_id_required", "จำเป็นต้องระบุรหัสแอดออน")
                combinedErrorText.contains("addon ids are required") ||
                combinedErrorText.contains("at least one addon id") -> 
                    getLocalizedString("api_error_delete_addon_ids_required", "ต้องระบุรหัสแอดออนอย่างน้อย 1 รายการ")
                combinedErrorText.contains("invalid request body") ||
                combinedErrorText.contains("invalid character") -> 
                    getLocalizedString("api_error_delete_addon_invalid_body", "ข้อมูลที่ส่งไม่ถูกต้อง")
                combinedErrorText.contains("addon not found") -> 
                    getLocalizedString("api_error_delete_addon_not_found", "ไม่พบแอดออน")
                combinedErrorText.contains("access denied") ||
                combinedErrorText.contains("addon does not belong") -> 
                    getLocalizedString("api_error_delete_addon_access_denied", "ไม่มีสิทธิ์เข้าถึง แอดออนนี้ไม่ใช่ของคุณ")
                statusCode == 500 || combinedErrorText.contains("failed to delete addon") -> 
                    getLocalizedString("api_error_delete_addon_server_error", "เกิดข้อผิดพลาดในการลบแอดออน กรุณาลองใหม่อีกครั้ง")
                else -> errorResponse.message?.takeIf { it.isNotBlank() }
                    ?: errorResponse.error?.takeIf { it.isNotBlank() }
                    ?: getLocalizedString("api_error_delete_addon_generic", "ไม่สามารถลบแอดออนได้ กรุณาลองใหม่อีกครั้ง")
            }
        } catch (e: Exception) {
            getLocalizedString("api_error_delete_addon_generic", "ไม่สามารถลบแอดออนได้ กรุณาลองใหม่อีกครั้ง")
        }
    }
    
    /**
     * Parse API error response body
     */
    private fun parseApiErrorResponse(errorBody: ResponseBody?, statusCode: Int): String {
        return try {
            if (errorBody == null) {
                return getDefaultErrorMessage(statusCode)
            }
            
            val errorJson = errorBody.string()
            if (errorJson.isBlank()) {
                return getDefaultErrorMessage(statusCode)
            }
            
            // Try to parse as error response
            try {
                val errorResponse = gson.fromJson(errorJson, AddonErrorResponse::class.java)
                val errorText = errorResponse.error?.lowercase() ?: ""
                val messageText = errorResponse.message?.lowercase() ?: ""
                
                // Check for specific error keys first
                val errorKey = errorResponse.error?.takeIf { it.isNotBlank() }
                val messageKey = errorResponse.message?.takeIf { it.isNotBlank() }
                val combinedErrorText = "$errorText $messageText"
                
                // Handle specific status codes
                when (statusCode) {
                    400 -> {
                        // Bad Request - return message or error field
                        // Check for specific error messages
                        when {
                            messageText.contains("addon name is required", ignoreCase = true) -> {
                                "กรุณากรอกชื่อ Addon"
                            }
                            else -> {
                                errorResponse.message?.takeIf { it.isNotBlank() }
                                    ?: errorResponse.error?.takeIf { it.isNotBlank() }
                                    ?: "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
                            }
                        }
                    }
                    401 -> {
                        // Unauthorized - return message or error field
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                    }
                    403 -> {
                        // Forbidden - Free plan limit exceeded or Access denied
                        val message = errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: ""
                        
                        when {
                            message.contains("free_plan_limit", ignoreCase = true) -> {
                                "คุณใช้ Addon ครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                            }
                            message.contains("access denied", ignoreCase = true) -> {
                                "คุณไม่มีสิทธิ์แก้ไข Addon นี้"
                            }
                            else -> {
                                message.ifBlank { "คุณไม่มีสิทธิ์เข้าถึง Addon นี้" }
                            }
                        }
                    }
                    404 -> {
                        // Not Found - Addon not found
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: "ไม่พบ Addon ที่ต้องการ"
                    }
                    409 -> {
                        // Conflict - Duplicate addon name
                        when {
                            combinedErrorText.contains("duplicate addon name") || 
                            combinedErrorText.contains("duplicate name") -> {
                                "ชื่อ Addon นี้มีอยู่แล้ว"
                            }
                            errorKey != null && messageKey != null -> "$errorKey ($messageKey)"
                            errorKey != null -> errorKey
                            messageKey != null -> messageKey
                            else -> "ชื่อ Addon นี้มีอยู่แล้ว"
                        }
                    }
                    500 -> {
                        // Internal Server Error
                        errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: "Server error - กรุณาลองใหม่อีกครั้ง"
                    }
                    else -> {
                        // Return error or message if available
                        errorResponse.error?.takeIf { it.isNotBlank() }
                            ?: errorResponse.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการสร้าง Addon"
                    }
                }
            } catch (e: Exception) {
                // If parsing fails, check raw string
                val errorLower = errorJson.lowercase()
                
                when {
                    statusCode == 409 && errorLower.contains("duplicate addon name") -> {
                        "ชื่อ Addon นี้มีอยู่แล้ว"
                    }
                    statusCode == 403 && errorLower.contains("free_plan_limit_exceeded") -> {
                        "คุณใช้ Addon ครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                    }
                    else -> {
                        getDefaultErrorMessage(statusCode)
                    }
                }
            }
        } catch (e: Exception) {
            getDefaultErrorMessage(statusCode)
        }
    }
    
    /**
     * Get default error message for status code
     */
    private fun getDefaultErrorMessage(statusCode: Int): String {
        return when (statusCode) {
            400 -> "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบอีกครั้ง"
            401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
            403 -> "คุณไม่มีสิทธิ์เข้าถึง Addon นี้"
            404 -> "ไม่พบ Addon ที่ต้องการ"
            409 -> "ชื่อ Addon นี้มีอยู่แล้ว"
            500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
            else -> "เกิดข้อผิดพลาดในการสร้าง Addon"
        }
    }
}

/**
 * Error response DTO for parsing API errors
 */
private data class AddonErrorResponse(
    val error: String?,
    val message: String?
)
