package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.AddonEntity

@Dao
interface AddonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addons: List<AddonEntity>)
    
    @Query("SELECT * FROM addons WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAllActiveAddons(): List<AddonEntity>
    
    @Query("SELECT * FROM addons WHERE addonGroupId = :addonGroupId AND isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAddonsByGroup(addonGroupId: String): List<AddonEntity>
    
    @Query("SELECT * FROM addons ORDER BY sortOrder ASC")
    suspend fun getAllAddons(): List<AddonEntity>
    
    @Query("DELETE FROM addons")
    suspend fun deleteAll()
}

