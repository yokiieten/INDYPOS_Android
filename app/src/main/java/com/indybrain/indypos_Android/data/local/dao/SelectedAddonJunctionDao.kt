package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.SelectedAddonJunctionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedAddonJunctionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(junction: SelectedAddonJunctionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(junctions: List<SelectedAddonJunctionEntity>)
    
    @Query("SELECT addonId FROM selected_addon_junction WHERE selectedAddonId = :selectedAddonId")
    fun getAddonIdsBySelectedAddonId(selectedAddonId: String): Flow<List<String>>
    
    @Query("SELECT addonId FROM selected_addon_junction WHERE selectedAddonId = :selectedAddonId")
    suspend fun getAddonIdsBySelectedAddonIdSync(selectedAddonId: String): List<String>
    
    @Query("SELECT selectedAddonId FROM selected_addon_junction WHERE addonId = :addonId")
    fun getSelectedAddonIdsByAddonId(addonId: String): Flow<List<String>>
    
    @Query("SELECT selectedAddonId FROM selected_addon_junction WHERE addonId = :addonId")
    suspend fun getSelectedAddonIdsByAddonIdSync(addonId: String): List<String>
    
    @Query("DELETE FROM selected_addon_junction WHERE selectedAddonId = :selectedAddonId")
    suspend fun deleteBySelectedAddonId(selectedAddonId: String)
    
    @Query("DELETE FROM selected_addon_junction WHERE addonId = :addonId")
    suspend fun deleteByAddonId(addonId: String)
    
    @Query("DELETE FROM selected_addon_junction WHERE selectedAddonId = :selectedAddonId AND addonId = :addonId")
    suspend fun delete(selectedAddonId: String, addonId: String)
    
    @Query("DELETE FROM selected_addon_junction")
    suspend fun deleteAll()
}

