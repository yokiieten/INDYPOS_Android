package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addons: List<AddonEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(addon: AddonEntity)
    
    @Query("SELECT * FROM addons WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAllActiveAddons(): List<AddonEntity>
    
    @Query("SELECT * FROM addons WHERE addonGroupId = :addonGroupId AND isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAddonsByGroup(addonGroupId: String): List<AddonEntity>
    
    @Query("SELECT * FROM addons ORDER BY sortOrder ASC")
    suspend fun getAllAddons(): List<AddonEntity>
    
    /**
     * Get all addons for management (including inactive, excluding soft deleted)
     */
    @Query("SELECT * FROM addons WHERE isDeletedLocally = 0 ORDER BY CASE WHEN isSynced = 0 THEN 0 ELSE 1 END, name ASC")
    fun getAllAddonsForManagementFlow(): Flow<List<AddonEntity>>
    
    /**
     * Get addon by ID
     */
    @Query("SELECT * FROM addons WHERE id = :id")
    suspend fun getAddonById(id: String): AddonEntity?
    
    /**
     * Get deleted addons (soft deleted)
     */
    @Query("SELECT * FROM addons WHERE isDeletedLocally = 1")
    suspend fun getDeletedAddons(): List<AddonEntity>
    
    /**
     * Update addon status
     */
    @Query("UPDATE addons SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAddonStatus(id: String, isActive: Boolean, updatedAt: java.util.Date)
    
    /**
     * Soft delete addon
     */
    @Query("UPDATE addons SET isDeletedLocally = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteAddon(id: String, updatedAt: java.util.Date)
    
    /**
     * Permanently delete addon
     */
    @Query("DELETE FROM addons WHERE id = :id")
    suspend fun permanentlyDeleteAddon(id: String)
    
    /**
     * Mark addons as synced
     */
    @Query("UPDATE addons SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAddonsAsSynced(ids: List<String>)
    
    @Query("DELETE FROM addons")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM addons WHERE id IN (:ids)")
    suspend fun getAddonsByIds(ids: List<String>): List<AddonEntity>
    
    /**
     * Get unsynced addons count
     */
    @Query("SELECT COUNT(*) FROM addons WHERE isSynced = 0 AND isDeletedLocally = 0")
    suspend fun getUnsyncedAddonsCount(): Int
}

