package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.data.local.dao.PrinterSettingsDao
import com.indybrain.indypos_Android.data.local.entity.PrinterSettingsEntity
import com.indybrain.indypos_Android.core.printer.PrinterType
import com.indybrain.indypos_Android.domain.repository.PrinterSettingsRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrinterSettingsRepositoryImpl @Inject constructor(
    private val printerSettingsDao: PrinterSettingsDao
) : PrinterSettingsRepository {
    
    override fun getPrinterSettings(type: PrinterType): Flow<PrinterSettingsEntity?> {
        return printerSettingsDao.getByType(type.name)
    }
    
    override suspend fun getPrinterSettingsSync(type: PrinterType): PrinterSettingsEntity? {
        return printerSettingsDao.getByTypeSync(type.name)
    }
    
    override fun getAllPrinterSettings(): Flow<List<PrinterSettingsEntity>> {
        return printerSettingsDao.getAll()
    }
    
    override suspend fun savePrinterSettings(
        type: PrinterType,
        printerName: String?,
        macAddress: String?,
        enabled: Boolean,
        autoConnect: Boolean
    ): Result<Unit> {
        return try {
            val existing = printerSettingsDao.getByTypeSync(type.name)
            val now = Date()
            
            val settings = if (existing != null) {
                existing.copy(
                    printerName = printerName,
                    printerMacAddress = macAddress,
                    enabled = enabled,
                    autoConnect = autoConnect,
                    updatedAt = now
                )
            } else {
                PrinterSettingsEntity(
                    id = type.name.lowercase(),
                    printerType = type.name,
                    printerName = printerName,
                    printerMacAddress = macAddress,
                    isConnected = false,
                    autoConnect = autoConnect,
                    enabled = enabled,
                    createdAt = now,
                    updatedAt = now
                )
            }
            
            printerSettingsDao.insertOrUpdate(settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateConnectionStatus(type: PrinterType, isConnected: Boolean): Result<Unit> {
        return try {
            val current = printerSettingsDao.getByTypeSync(type.name)
                ?: return Result.failure(Exception("ไม่พบการตั้งค่าเครื่องพิมพ์"))
            
            val updated = current.copy(
                isConnected = isConnected,
                updatedAt = Date()
            )
            printerSettingsDao.insertOrUpdate(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateEnabled(type: PrinterType, enabled: Boolean): Result<Unit> {
        return try {
            val current = printerSettingsDao.getByTypeSync(type.name)
                ?: return Result.failure(Exception("ไม่พบการตั้งค่าเครื่องพิมพ์"))
            
            val updated = current.copy(
                enabled = enabled,
                updatedAt = Date()
            )
            printerSettingsDao.insertOrUpdate(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateAutoConnect(type: PrinterType, autoConnect: Boolean): Result<Unit> {
        return try {
            val current = printerSettingsDao.getByTypeSync(type.name)
                ?: return Result.failure(Exception("ไม่พบการตั้งค่าเครื่องพิมพ์"))
            
            val updated = current.copy(
                autoConnect = autoConnect,
                updatedAt = Date()
            )
            printerSettingsDao.insertOrUpdate(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deletePrinterSettings(type: PrinterType): Result<Unit> {
        return try {
            printerSettingsDao.deleteById(type.name.lowercase())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun initializeDefaultSettings() {
        // Initialize settings for both printer types if they don't exist
        PrinterType.values().forEach { type ->
            val existing = printerSettingsDao.getByTypeSync(type.name)
            if (existing == null) {
                val now = Date()
                val defaultSettings = PrinterSettingsEntity(
                    id = type.name.lowercase(),
                    printerType = type.name,
                    printerName = null,
                    printerMacAddress = null,
                    isConnected = false,
                    autoConnect = false,
                    enabled = false,
                    createdAt = now,
                    updatedAt = now
                )
                printerSettingsDao.insert(defaultSettings)
            }
        }
    }
}
