package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.ReceiptSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: ReceiptSettingsEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: ReceiptSettingsEntity)
    
    @Query("SELECT * FROM receipt_settings WHERE id = :id")
    fun getById(id: String): Flow<ReceiptSettingsEntity?>
    
    @Query("SELECT * FROM receipt_settings WHERE id = :id")
    suspend fun getByIdSync(id: String): ReceiptSettingsEntity?
    
    @Query("SELECT * FROM receipt_settings LIMIT 1")
    fun getFirst(): Flow<ReceiptSettingsEntity?>
    
    @Query("SELECT * FROM receipt_settings LIMIT 1")
    suspend fun getFirstSync(): ReceiptSettingsEntity?
    
    @Query("DELETE FROM receipt_settings WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM receipt_settings")
    suspend fun deleteAll()
}


