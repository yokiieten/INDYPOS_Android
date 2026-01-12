package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.PrinterSettingsEntity
import com.indybrain.indypos_Android.core.printer.PrinterType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for printer settings operations
 */
interface PrinterSettingsRepository {
    /**
     * Get printer settings by type
     */
    fun getPrinterSettings(type: PrinterType): Flow<PrinterSettingsEntity?>
    
    /**
     * Get printer settings by type synchronously
     */
    suspend fun getPrinterSettingsSync(type: PrinterType): PrinterSettingsEntity?
    
    /**
     * Get all printer settings
     */
    fun getAllPrinterSettings(): Flow<List<PrinterSettingsEntity>>
    
    /**
     * Save or update printer settings
     */
    suspend fun savePrinterSettings(
        type: PrinterType,
        printerName: String?,
        macAddress: String?,
        enabled: Boolean = true,
        autoConnect: Boolean = false
    ): Result<Unit>
    
    /**
     * Update printer connection status
     */
    suspend fun updateConnectionStatus(type: PrinterType, isConnected: Boolean): Result<Unit>
    
    /**
     * Update printer enabled status
     */
    suspend fun updateEnabled(type: PrinterType, enabled: Boolean): Result<Unit>
    
    /**
     * Update auto connect setting
     */
    suspend fun updateAutoConnect(type: PrinterType, autoConnect: Boolean): Result<Unit>
    
    /**
     * Delete printer settings by type
     */
    suspend fun deletePrinterSettings(type: PrinterType): Result<Unit>
    
    /**
     * Initialize default settings for all printer types if not exists
     */
    suspend fun initializeDefaultSettings()
}
