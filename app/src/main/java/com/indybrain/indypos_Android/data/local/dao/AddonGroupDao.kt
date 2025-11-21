package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity

@Dao
interface AddonGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(addonGroups: List<AddonGroupEntity>)
    
    @Query("SELECT * FROM addon_groups WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAllActiveAddonGroups(): List<AddonGroupEntity>
    
    @Query("SELECT * FROM addon_groups ORDER BY sortOrder ASC")
    suspend fun getAllAddonGroups(): List<AddonGroupEntity>
    
    @Query("DELETE FROM addon_groups")
    suspend fun deleteAll()
}

