package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AddonGroupDao {
    // Basic CRUD
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddonGroup(addonGroup: AddonGroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addonGroups: List<AddonGroupEntity>)
    
    @Query("SELECT * FROM addon_groups WHERE isDeletedLocally = 0 ORDER BY sortOrder ASC")
    suspend fun getAllAddonGroups(): List<AddonGroupEntity>
    
    @Query("SELECT * FROM addon_groups WHERE isDeletedLocally = 0 ORDER BY sortOrder ASC")
    fun getAllAddonGroupsFlow(): Flow<List<AddonGroupEntity>>
    
    @Query("SELECT * FROM addon_groups WHERE isActive = 1 AND isDeletedLocally = 0 ORDER BY sortOrder ASC")
    suspend fun getAllActiveAddonGroups(): List<AddonGroupEntity>
    
    @Query("SELECT * FROM addon_groups WHERE id = :id AND isDeletedLocally = 0")
    suspend fun getAddonGroupById(id: String): AddonGroupEntity?
    
    @Update
    suspend fun updateAddonGroup(addonGroup: AddonGroupEntity)
    
    @Query("UPDATE addon_groups SET isDeletedLocally = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteAddonGroup(id: String, updatedAt: Date)
    
    @Query("DELETE FROM addon_groups WHERE id = :id")
    suspend fun permanentlyDeleteAddonGroup(id: String)
    
    @Query("DELETE FROM addon_groups")
    suspend fun deleteAll()
    
    // Sync operations
    @Query("SELECT * FROM addon_groups WHERE isSynced = 0 AND isDeletedLocally = 0")
    suspend fun getUnsyncedAddonGroups(): List<AddonGroupEntity>
    
    @Query("SELECT * FROM addon_groups WHERE isDeletedLocally = 1 AND isSynced = 0")
    suspend fun getDeletedUnsyncedAddonGroups(): List<AddonGroupEntity>
    
    @Query("UPDATE addon_groups SET isSynced = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsSynced(id: String, updatedAt: Date)
    
    @Query("UPDATE addon_groups SET isSynced = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsUnsynced(id: String, updatedAt: Date)
    
    // Status operations
    @Query("UPDATE addon_groups SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleStatus(id: String, isActive: Boolean, updatedAt: Date)
    
    // For management screen (including inactive, excluding deleted)
    @Query("SELECT * FROM addon_groups WHERE isDeletedLocally = 0 ORDER BY sortOrder ASC")
    fun getAllAddonGroupsForManagementFlow(): Flow<List<AddonGroupEntity>>
}

