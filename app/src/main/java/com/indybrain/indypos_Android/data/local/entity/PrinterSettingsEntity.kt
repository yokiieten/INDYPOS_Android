package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

/**
 * PrinterSettingsEntity to store printer configurations
 * Supports multiple printer types (Receipt and Label printers)
 */
@Entity(tableName = "printer_settings")
@TypeConverters(DateConverter::class)
data class PrinterSettingsEntity(
    @PrimaryKey
    val id: String, // "receipt" or "label"
    val printerType: String, // "RECEIPT" or "LABEL"
    val printerName: String?,
    val printerMacAddress: String?,
    val isConnected: Boolean = false,
    val autoConnect: Boolean = false,
    val enabled: Boolean = false, // Enable/disable this printer
    val createdAt: Date,
    val updatedAt: Date
)
