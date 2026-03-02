package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for addon operations
 */
interface AddonRepository {
    /**
     * Get all addons from local database (including inactive, excluding deleted)
     */
    fun getAllAddonsForManagementFlow(): Flow<List<AddonEntity>>
    
    /**
     * Get addon by ID
     */
    suspend fun getAddonById(id: String): AddonEntity?
    
    /**
     * Create a new addon
     * If network is available, calls API and saves to Room
     * If network is not available, saves to Room only (for sync later)
     */
    suspend fun createAddon(name: String, price: Double): Result<AddonEntity>
    
    /**
     * Update an existing addon
     * If network is available and addon is synced, calls API and updates Room
     * If network is not available or addon is not synced, updates Room only (for sync later)
     */
    suspend fun updateAddon(addonId: String, name: String, price: Double): Result<AddonEntity>
    
    /**
     * Get deleted addons (soft deleted)
     */
    suspend fun getDeletedAddons(): List<AddonEntity>
    
    /**
     * Load addons from API and sync with Room
     */
    suspend fun fetchAndSyncAddons(): Result<Unit>
    
    /**
     * Toggle addon status (activate/deactivate)
     * If network is available, calls API and saves to Room
     * If network is not available, updates in Room only (for sync later)
     */
    suspend fun toggleAddonStatus(addonId: String, newStatus: Boolean): Result<AddonEntity>
    
    /**
     * Delete an addon
     * If network is available, calls API and soft deletes from Room
     * If network is not available, soft deletes in Room only (for sync later)
     */
    suspend fun deleteAddon(addonId: String): Result<Unit>
    
    /**
     * Delete multiple addons
     * If network is available, calls API. Returns DeleteAddonsResult with deleted/failed counts.
     * If network is not available, soft deletes in Room only (for sync later)
     */
    suspend fun deleteMultipleAddons(addonIds: List<String>): Result<DeleteAddonsResult>
    
    /**
     * Permanently delete an addon from Room
     */
    suspend fun permanentlyDeleteAddon(addonId: String)
    
    /**
     * Get sync statistics
     */
    suspend fun getSyncStatistics(): AddonSyncStatistics
    
    /**
     * Sync pending addons with server
     */
    suspend fun syncPendingAddons(): Result<Unit>
}

/**
 * Result of batch delete addons operation
 */
data class DeleteAddonsResult(
    val deletedCount: Int = 0,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList()
)

/**
 * Sync statistics for addons
 */
data class AddonSyncStatistics(
    val total: Int,
    val synced: Int,
    val unsynced: Int,
    val deleted: Int
)

