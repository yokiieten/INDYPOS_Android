package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.PrinterSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: PrinterSettingsEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: PrinterSettingsEntity)
    
    @Query("SELECT * FROM printer_settings WHERE id = :id")
    fun getById(id: String): Flow<PrinterSettingsEntity?>
    
    @Query("SELECT * FROM printer_settings WHERE id = :id")
    suspend fun getByIdSync(id: String): PrinterSettingsEntity?
    
    @Query("SELECT * FROM printer_settings WHERE printerType = :printerType LIMIT 1")
    fun getByType(printerType: String): Flow<PrinterSettingsEntity?>
    
    @Query("SELECT * FROM printer_settings WHERE printerType = :printerType LIMIT 1")
    suspend fun getByTypeSync(printerType: String): PrinterSettingsEntity?
    
    @Query("SELECT * FROM printer_settings")
    fun getAll(): Flow<List<PrinterSettingsEntity>>
    
    @Query("SELECT * FROM printer_settings")
    suspend fun getAllSync(): List<PrinterSettingsEntity>
    
    @Query("DELETE FROM printer_settings WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM printer_settings")
    suspend fun deleteAll()
}
