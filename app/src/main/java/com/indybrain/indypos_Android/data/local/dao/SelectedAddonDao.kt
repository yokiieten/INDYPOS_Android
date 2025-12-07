package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.indybrain.indypos_Android.data.local.entity.SelectedAddonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedAddonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(selectedAddon: SelectedAddonEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(selectedAddons: List<SelectedAddonEntity>)
    
    @Query("SELECT * FROM selected_addons WHERE cartItemId = :cartItemId")
    fun getSelectedAddonsByCartItemId(cartItemId: String): Flow<List<SelectedAddonEntity>>
    
    @Query("SELECT * FROM selected_addons WHERE cartItemId = :cartItemId")
    suspend fun getSelectedAddonsByCartItemIdSync(cartItemId: String): List<SelectedAddonEntity>
    
    @Query("SELECT * FROM selected_addons WHERE id = :id")
    suspend fun getById(id: String): SelectedAddonEntity?
    
    @Query("DELETE FROM selected_addons WHERE cartItemId = :cartItemId")
    suspend fun deleteByCartItemId(cartItemId: String)
    
    @Query("DELETE FROM selected_addons WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM selected_addons")
    suspend fun deleteAll()
}

