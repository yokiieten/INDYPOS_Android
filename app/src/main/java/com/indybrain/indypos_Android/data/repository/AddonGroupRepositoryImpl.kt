package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupAddonJunctionDao
import com.indybrain.indypos_Android.data.local.entity.AddonGroupAddonJunctionEntity
import com.indybrain.indypos_Android.data.local.entity.AddonGroupWithAddons
import com.indybrain.indypos_Android.data.mapper.ProductMapper
import com.indybrain.indypos_Android.data.remote.api.*
import com.indybrain.indypos_Android.data.remote.dto.AddonGroupDto
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

class AddonGroupRepositoryImpl @Inject constructor(
    private val productsApi: ProductsApi,
    private val addonGroupDao: AddonGroupDao,
    private val addonDao: AddonDao,
    private val junctionDao: AddonGroupAddonJunctionDao,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : AddonGroupRepository {
    
    override fun getAllAddonGroupsFlow(): Flow<List<com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity>> {
        return addonGroupDao.getAllAddonGroupsForManagementFlow()
    }
    
    override suspend fun getAddonGroupById(id: String): com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity? {
        return addonGroupDao.getAddonGroupById(id)
    }
    
    override suspend fun getAddonGroupWithAddonsById(id: String): AddonGroupWithAddons? {
        val addonGroup = addonGroupDao.getAddonGroupById(id) ?: return null
        val addonIds = junctionDao.getAddonIdsByAddonGroupIdSync(id)
        val addons = addonIds.mapNotNull { addonId -> addonDao.getAddonById(addonId) }
        return AddonGroupWithAddons(addonGroup, addons)
    }
    
    override suspend fun createAddonGroup(
        name: String,
        isRequired: Boolean,
        isSingleSelection: Boolean,
        maxSelection: Int,
        minSelection: Int,
        sortOrder: Int,
        selectedAddonIds: List<String>
    ): Result<com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity> {
        return try {
            val addonGroupEntity: com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = CreateAddonGroupRequestDto(
                        addonGroup = CreateAddonGroupRequestDto.AddonGroupData(
                            name = name.trim(),
                            isRequired = isRequired,
                            isSingleSelection = isSingleSelection,
                            maxSelection = maxSelection,
                            minSelection = minSelection,
                            sortOrder = sortOrder,
                            isActive = true
                        ),
                        addons = selectedAddonIds.mapIndexed { index, addonId ->
                            CreateAddonGroupRequestDto.AddonData(
                                addonId = addonId,
                                sortOrder = index + 1
                            )
                        }
                    )
                    
                    val response = productsApi.createAddonGroup(request)
                    
                    // Check if response indicates success (200 or 201) and has data
                    // Also check if message contains success keywords even if status is not 200/201
                    val isSuccessStatus = response.status == 200 || response.status == 201
                    val hasSuccessMessage = response.message?.contains("success", ignoreCase = true) == true
                        || response.message?.contains("created", ignoreCase = true) == true
                    
                    if ((isSuccessStatus && response.data != null) || (hasSuccessMessage && response.data != null)) {
                        // API success - convert to entity and save to Room
                        addonGroupEntity = ProductMapper.toEntity(response.data)
                        addonGroupDao.insertAddonGroup(addonGroupEntity)
                        
                        // Save relationships
                        response.data.addons?.forEachIndexed { index, addonDto ->
                            val addonEntity = ProductMapper.toEntity(addonDto)
                            addonDao.insert(addonEntity)
                            junctionDao.insert(
                                AddonGroupAddonJunctionEntity(
                                    addonGroupId = addonGroupEntity.id,
                                    addonId = addonEntity.id,
                                    sortOrder = index + 1
                                )
                            )
                        }
                        
                        Result.success(addonGroupEntity)
                    } else {
                        // API returned error status
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: response.error?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการสร้างกลุ่ม Addon"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        403 -> {
                            val errorBody = e.response()?.errorBody()?.string()
                            if (errorBody?.contains("free_plan_limit_exceeded", ignoreCase = true) == true) {
                                "free_plan_limit_exceeded"
                            } else {
                                e.message() ?: "เกิดข้อผิดพลาดในการสร้างกลุ่ม Addon"
                            }
                        }
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการสร้างกลุ่ม Addon"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - save to Room only (for sync later)
                val now = Date()
                val localId = UUID.randomUUID().toString()
                addonGroupEntity = com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity(
                    id = localId,
                    name = name.trim(),
                    isRequired = isRequired,
                    isSingleSelection = isSingleSelection,
                    maxSelection = maxSelection,
                    minSelection = minSelection,
                    sortOrder = sortOrder,
                    isActive = true,
                    isDeletedLocally = false,
                    isFromServer = false,
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
                addonGroupDao.insertAddonGroup(addonGroupEntity)
                
                // Save relationships
                selectedAddonIds.forEachIndexed { index, addonId ->
                    junctionDao.insert(
                        AddonGroupAddonJunctionEntity(
                            addonGroupId = localId,
                            addonId = addonId,
                            sortOrder = index + 1
                        )
                    )
                }
                
                Result.success(addonGroupEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการสร้างกลุ่ม Addon"))
        }
    }
    
    override suspend fun updateAddonGroup(
        addonGroupId: String,
        name: String?,
        isRequired: Boolean?,
        isSingleSelection: Boolean?,
        maxSelection: Int?,
        minSelection: Int?,
        sortOrder: Int?,
        isActive: Boolean?,
        selectedAddonIds: List<String>?
    ): Result<com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity> {
        return try {
            val existing = addonGroupDao.getAddonGroupById(addonGroupId)
                ?: return Result.failure(Exception("ไม่พบกลุ่ม Addon ที่ต้องการแก้ไข"))
            
            val addonGroupEntity: com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
            val finalName = name?.trim() ?: existing.name
            val finalIsRequired = isRequired ?: existing.isRequired
            val finalIsSingleSelection = isSingleSelection ?: existing.isSingleSelection
            val finalMaxSelection = maxSelection ?: existing.maxSelection ?: 1
            val finalMinSelection = minSelection ?: existing.minSelection ?: 0
            val finalSortOrder = sortOrder ?: existing.sortOrder ?: 0
            val finalIsActive = isActive ?: existing.isActive
            val finalSelectedAddonIds = selectedAddonIds ?: junctionDao.getAddonIdsByAddonGroupIdSync(addonGroupId)
            
            // Check if should update via API or locally only
            val shouldUpdateViaAPI = networkConnectivityChecker.isConnected() && 
                                    existing.isSynced && 
                                    existing.isFromServer
            
            if (shouldUpdateViaAPI) {
                // Has network and addon group is synced - call API first
                try {
                    val request = UpdateAddonGroupRequestDto(
                        addonGroup = UpdateAddonGroupRequestDto.AddonGroupData(
                            id = addonGroupId,
                            name = finalName,
                            isRequired = finalIsRequired,
                            isSingleSelection = finalIsSingleSelection,
                            maxSelection = finalMaxSelection,
                            minSelection = finalMinSelection,
                            sortOrder = finalSortOrder,
                            isActive = finalIsActive
                        ),
                        addons = finalSelectedAddonIds.mapIndexed { index, addonId ->
                            UpdateAddonGroupRequestDto.AddonData(
                                addonId = addonId,
                                sortOrder = index + 1
                            )
                        }
                    )
                    
                    val response = productsApi.updateAddonGroup(request)
                    
                    // Check if response indicates success (200 or 201) and has data
                    // Also check if message contains success keywords even if status is not 200/201
                    val isSuccessStatus = response.status == 200 || response.status == 201
                    val hasSuccessMessage = response.message?.contains("success", ignoreCase = true) == true
                        || response.message?.contains("updated", ignoreCase = true) == true
                    
                    if ((isSuccessStatus && response.data != null) || (hasSuccessMessage && response.data != null)) {
                        // API success - convert to entity and save to Room
                        addonGroupEntity = ProductMapper.toEntity(response.data)
                        addonGroupDao.updateAddonGroup(addonGroupEntity)
                        
                        // Update relationships
                        junctionDao.deleteByAddonGroupId(addonGroupId)
                        response.data.addons?.forEachIndexed { index, addonDto ->
                            val addonEntity = ProductMapper.toEntity(addonDto)
                            addonDao.insert(addonEntity)
                            junctionDao.insert(
                                AddonGroupAddonJunctionEntity(
                                    addonGroupId = addonGroupId,
                                    addonId = addonEntity.id,
                                    sortOrder = index + 1
                                )
                            )
                        }
                        
                        Result.success(addonGroupEntity)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: response.error?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการแก้ไขกลุ่ม Addon"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        404 -> "ไม่พบกลุ่ม Addon ที่ต้องการแก้ไข"
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการแก้ไขกลุ่ม Addon"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network or not synced - update in Room only (for sync later)
                addonGroupEntity = existing.copy(
                    name = finalName,
                    isRequired = finalIsRequired,
                    isSingleSelection = finalIsSingleSelection,
                    maxSelection = finalMaxSelection,
                    minSelection = finalMinSelection,
                    sortOrder = finalSortOrder,
                    isActive = finalIsActive,
                    updatedAt = Date(),
                    isSynced = false
                )
                addonGroupDao.updateAddonGroup(addonGroupEntity)
                
                // Update relationships if provided
                if (selectedAddonIds != null) {
                    junctionDao.deleteByAddonGroupId(addonGroupId)
                    selectedAddonIds.forEachIndexed { index, addonId ->
                        junctionDao.insert(
                            AddonGroupAddonJunctionEntity(
                                addonGroupId = addonGroupId,
                                addonId = addonId,
                                sortOrder = index + 1
                            )
                        )
                    }
                }
                
                Result.success(addonGroupEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการแก้ไขกลุ่ม Addon"))
        }
    }
    
    override suspend fun isDuplicateName(name: String, excludeId: String?): Boolean {
        val normalizedName = name.trim().lowercase()
        val allGroups = getAllAddonGroupsFlow().first()
        return allGroups.any { group ->
            val groupName = group.name.trim().lowercase()
            groupName == normalizedName && group.id != excludeId
        }
    }
    
    override suspend fun toggleAddonGroupStatus(
        addonGroupId: String,
        newStatus: Boolean
    ): Result<com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity> {
        return try {
            val existing = addonGroupDao.getAddonGroupById(addonGroupId)
                ?: return Result.failure(Exception("ไม่พบกลุ่ม Addon ที่ต้องการอัปเดต"))
            
            val addonGroupEntity: com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = ToggleAddonGroupStatusRequestDto(status = newStatus)
                    val response = productsApi.toggleAddonGroupStatus(addonGroupId, request)
                    
                    if (response.status == 200 && response.data != null) {
                        // API success - convert to entity and save to Room
                        addonGroupEntity = ProductMapper.toEntity(response.data)
                        addonGroupDao.updateAddonGroup(addonGroupEntity)
                        Result.success(addonGroupEntity)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะกลุ่ม Addon"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        404 -> "ไม่พบกลุ่ม Addon ที่ต้องการอัปเดต"
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะกลุ่ม Addon"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - update in Room only (for sync later)
                addonGroupEntity = existing.copy(
                    isActive = newStatus,
                    updatedAt = Date(),
                    isSynced = false
                )
                addonGroupDao.updateAddonGroup(addonGroupEntity)
                Result.success(addonGroupEntity)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะกลุ่ม Addon"))
        }
    }
    
    override suspend fun deleteAddonGroup(addonGroupId: String): Result<Unit> {
        return try {
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val response = productsApi.deleteAddonGroup(addonGroupId)
                    
                    if (response.status == 200) {
                        // API success - permanently delete from Room
                        addonGroupDao.permanentlyDeleteAddonGroup(addonGroupId)
                        Result.success(Unit)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        404 -> "ไม่พบกลุ่ม Addon ที่ต้องการลบ"
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - mark as deleted locally (for sync later)
                val existing = addonGroupDao.getAddonGroupById(addonGroupId)
                if (existing == null) {
                    return Result.failure(Exception("ไม่พบกลุ่ม Addon ที่ต้องการลบ"))
                }
                
                if (!existing.isSynced && !existing.isFromServer) {
                    // Not synced and not from server - permanently delete
                    addonGroupDao.permanentlyDeleteAddonGroup(addonGroupId)
                } else {
                    // Mark as deleted locally and unsynced for sync later
                    addonGroupDao.softDeleteAddonGroup(addonGroupId, Date())
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"))
        }
    }
    
    override suspend fun deleteMultipleAddonGroups(addonGroupIds: List<String>): Result<Unit> {
        return try {
            if (addonGroupIds.isEmpty()) {
                return Result.failure(Exception("กรุณาเลือกกลุ่ม Addon ที่ต้องการลบ"))
            }
            
            if (networkConnectivityChecker.isConnected()) {
                // Has network - call API first
                try {
                    val request = DeleteAddonGroupsRequestDto(addonGroupIds = addonGroupIds)
                    val response = productsApi.deleteMultipleAddonGroups(request)
                    
                    if (response.status == 200) {
                        // API success - permanently delete from Room
                        val deletedIds = response.data?.deletedIds ?: addonGroupIds
                        deletedIds.forEach { id ->
                            addonGroupDao.permanentlyDeleteAddonGroup(id)
                        }
                        Result.success(Unit)
                    } else {
                        val errorMessage = response.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: HttpException) {
                    val errorMessage = when (e.code()) {
                        401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                        500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                        else -> e.message() ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // No network - mark as deleted locally
                addonGroupIds.forEach { id ->
                    val existing = addonGroupDao.getAddonGroupById(id)
                    if (existing != null) {
                        if (!existing.isSynced && !existing.isFromServer) {
                            addonGroupDao.permanentlyDeleteAddonGroup(id)
                        } else {
                            addonGroupDao.softDeleteAddonGroup(id, Date())
                        }
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"))
        }
    }
    
    override suspend fun fetchAndSyncAddonGroups(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
            }
            
            val response = productsApi.getAddonGroups()
            
            if (response.status == 200) {
                // If status is 200, treat as success even if data is null or empty (new user might have no data)
                val addonGroupsList = response.data ?: emptyList()
                // Convert and save addon groups
                if (addonGroupsList.isNotEmpty()) {
                    val addonGroups = addonGroupsList.map { ProductMapper.toEntity(it) }
                    addonGroupDao.insertAll(addonGroups)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "เกิดข้อผิดพลาดในการดึงข้อมูล"))
            }
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
    
    override suspend fun syncAddonGroups(): Result<Unit> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.success(Unit) // No network, skip sync
            }
            
            // Get all unsynced addon groups
            val unsynced = addonGroupDao.getUnsyncedAddonGroups()
            val deletedUnsynced = addonGroupDao.getDeletedUnsyncedAddonGroups()
            
            if (unsynced.isEmpty() && deletedUnsynced.isEmpty()) {
                return Result.success(Unit) // Nothing to sync
            }
            
            // Date formatter for ISO string
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            // Convert to sync items
            val syncItems = (unsynced + deletedUnsynced).map { entity ->
                // Get addons for this addon group
                val junctions = junctionDao.getJunctionsByAddonGroupId(entity.id)
                val addonItems = junctions.map { junction ->
                    AddonGroupAddonItemDto(
                        addonId = junction.addonId,
                        sortOrder = junction.sortOrder
                    )
                }
                
                SyncAddonGroupItemDto(
                    id = entity.id,
                    name = entity.name,
                    description = null,
                    isRequired = entity.isRequired,
                    isSingleSelection = entity.isSingleSelection,
                    maxSelection = entity.maxSelection ?: 0,
                    minSelection = entity.minSelection ?: 0,
                    sortOrder = entity.sortOrder ?: 0,
                    isActive = entity.isActive,
                    isSynced = entity.isSynced,
                    isDeletedLocally = entity.isDeletedLocally,
                    createdAt = dateFormat.format(entity.createdAt),
                    updatedAt = dateFormat.format(entity.updatedAt),
                    addons = addonItems
                )
            }
            
            val request = SyncAddonGroupsRequestDto(addonGroups = syncItems)
            val response = productsApi.syncAddonGroups(request)
            
            // Process sync results
            response.data?.forEach { result ->
                when {
                    result.shouldDelete == true -> {
                        // Delete locally
                        result.id?.let { id ->
                            addonGroupDao.permanentlyDeleteAddonGroup(id)
                        }
                    }
                    result.serverData != null -> {
                        // Update with server data
                        val serverEntity = ProductMapper.toEntity(result.serverData)
                        addonGroupDao.insertAddonGroup(serverEntity)
                    }
                    else -> {
                        // Mark as synced
                        result.id?.let { id ->
                            addonGroupDao.markAsSynced(id, Date())
                        }
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized - กรุณาเข้าสู่ระบบใหม่"
                403 -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody?.contains("free_plan_limit_exceeded", ignoreCase = true) == true) {
                        "คุณใช้กลุ่มตัวเลือกเพิ่มเติมครบจำนวนที่กำหนดแล้ว กรุณาอัปเกรดแผน"
                    } else {
                        e.message() ?: "เกิดข้อผิดพลาดในการ sync"
                    }
                }
                500 -> "Server error - กรุณาลองใหม่อีกครั้ง"
                else -> e.message() ?: "เกิดข้อผิดพลาดในการ sync"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการ sync"))
        }
    }
    
    override suspend fun getSyncStatistics(): com.indybrain.indypos_Android.domain.repository.AddonGroupSyncStatistics {
        val allAddonGroups = addonGroupDao.getAllAddonGroups()
        val unsynced = addonGroupDao.getUnsyncedAddonGroups()
        val deleted = addonGroupDao.getDeletedUnsyncedAddonGroups()
        
        val total = allAddonGroups.size + deleted.size
        val synced = allAddonGroups.count { it.isSynced }
        val unsyncedCount = unsynced.size
        val deletedCount = deleted.size
        
        return com.indybrain.indypos_Android.domain.repository.AddonGroupSyncStatistics(
            total = total,
            synced = synced,
            unsynced = unsyncedCount,
            deleted = deletedCount
        )
    }
}

