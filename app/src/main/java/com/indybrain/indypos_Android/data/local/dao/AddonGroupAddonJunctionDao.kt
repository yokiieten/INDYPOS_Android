package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.AddonGroupAddonJunctionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddonGroupAddonJunctionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(junction: AddonGroupAddonJunctionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(junctions: List<AddonGroupAddonJunctionEntity>)
    
    @Query("SELECT addonId FROM addon_group_addon_junction WHERE addonGroupId = :addonGroupId ORDER BY sortOrder ASC")
    fun getAddonIdsByAddonGroupId(addonGroupId: String): Flow<List<String>>
    
    @Query("SELECT addonId FROM addon_group_addon_junction WHERE addonGroupId = :addonGroupId ORDER BY sortOrder ASC")
    suspend fun getAddonIdsByAddonGroupIdSync(addonGroupId: String): List<String>
    
    @Query("SELECT * FROM addon_group_addon_junction WHERE addonGroupId = :addonGroupId ORDER BY sortOrder ASC")
    suspend fun getJunctionsByAddonGroupId(addonGroupId: String): List<AddonGroupAddonJunctionEntity>
    
    @Query("SELECT addonGroupId FROM addon_group_addon_junction WHERE addonId = :addonId")
    fun getAddonGroupIdsByAddonId(addonId: String): Flow<List<String>>
    
    @Query("SELECT addonGroupId FROM addon_group_addon_junction WHERE addonId = :addonId")
    suspend fun getAddonGroupIdsByAddonIdSync(addonId: String): List<String>
    
    @Query("DELETE FROM addon_group_addon_junction WHERE addonGroupId = :addonGroupId")
    suspend fun deleteByAddonGroupId(addonGroupId: String)
    
    @Query("DELETE FROM addon_group_addon_junction WHERE addonId = :addonId")
    suspend fun deleteByAddonId(addonId: String)
    
    @Query("DELETE FROM addon_group_addon_junction WHERE addonGroupId = :addonGroupId AND addonId = :addonId")
    suspend fun delete(addonGroupId: String, addonId: String)
    
    @Query("DELETE FROM addon_group_addon_junction")
    suspend fun deleteAll()
}

