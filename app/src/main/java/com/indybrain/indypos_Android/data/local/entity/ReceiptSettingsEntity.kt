package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

/**
 * ReceiptSettingsEntity from ReceiptSettingsDataModel
 * Standalone entity (no relationships)
 */
@Entity(tableName = "receipt_settings")
@TypeConverters(DateConverter::class)
data class ReceiptSettingsEntity(
    @PrimaryKey
    val id: String,
    val printShopLogo: Boolean,
    val printAfterFinish: Boolean,
    val showQRCode: Boolean,
    val openCashDrawer: Boolean,
    val paperSize: String?,
    val footer: String?,
    val shopLogoImagePath: String?,
    val promptPayType: String?,
    val promptPayIdentifier: String?,
    val taxIdentificationNumber: Boolean,
    val tinNumber: String?,
    val createdAt: Date,
    val updatedAt: Date
)


