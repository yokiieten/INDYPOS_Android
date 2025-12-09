package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
import com.indybrain.indypos_Android.data.local.entity.AddonGroupWithAddons
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for addon group operations
 */
interface AddonGroupRepository {
    /**
     * Get all addon groups from local database (including inactive, excluding deleted)
     */
    fun getAllAddonGroupsFlow(): Flow<List<AddonGroupEntity>>
    
    /**
     * Get addon group by ID
     */
    suspend fun getAddonGroupById(id: String): AddonGroupEntity?
    
    /**
     * Get addon group with addons by ID
     */
    suspend fun getAddonGroupWithAddonsById(id: String): AddonGroupWithAddons?
    
    /**
     * Create a new addon group
     * If network is available, calls API and saves to Room
     * If network is not available, saves to Room only (for sync later)
     */
    suspend fun createAddonGroup(
        name: String,
        isRequired: Boolean,
        isSingleSelection: Boolean,
        maxSelection: Int,
        minSelection: Int,
        sortOrder: Int,
        selectedAddonIds: List<String>
    ): Result<AddonGroupEntity>
    
    /**
     * Update an existing addon group
     * If network is available, calls API and saves to Room
     * If network is not available, saves to Room only (for sync later)
     */
    suspend fun updateAddonGroup(
        addonGroupId: String,
        name: String? = null,
        isRequired: Boolean? = null,
        isSingleSelection: Boolean? = null,
        maxSelection: Int? = null,
        minSelection: Int? = null,
        sortOrder: Int? = null,
        isActive: Boolean? = null,
        selectedAddonIds: List<String>? = null
    ): Result<AddonGroupEntity>
    
    /**
     * Check for duplicate name (case-insensitive, trimmed)
     */
    suspend fun isDuplicateName(name: String, excludeId: String? = null): Boolean
    
    /**
     * Toggle addon group status (activate/deactivate)
     * If network is available, calls API and saves to Room
     * If network is not available, updates in Room only (for sync later)
     */
    suspend fun toggleAddonGroupStatus(
        addonGroupId: String,
        newStatus: Boolean
    ): Result<AddonGroupEntity>
    
    /**
     * Delete an addon group
     * If network is available, calls API and deletes from Room
     * If network is not available, marks as deleted locally (isDeletedLocally = true)
     */
    suspend fun deleteAddonGroup(addonGroupId: String): Result<Unit>
    
    /**
     * Delete multiple addon groups
     */
    suspend fun deleteMultipleAddonGroups(addonGroupIds: List<String>): Result<Unit>
    
    /**
     * Fetch addon groups from server and sync with local database
     */
    suspend fun fetchAndSyncAddonGroups(): Result<Unit>
    
    /**
     * Sync unsynced addon groups with server
     */
    suspend fun syncAddonGroups(): Result<Unit>
    
    /**
     * Get sync statistics
     */
    suspend fun getSyncStatistics(): AddonGroupSyncStatistics
}

/**
 * Sync statistics for addon groups
 */
data class AddonGroupSyncStatistics(
    val total: Int,
    val synced: Int,
    val unsynced: Int,
    val deleted: Int
)

