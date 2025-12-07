package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

/**
 * StoreEntity from StoreDataModel
 * Standalone entity (no relationships)
 */
@Entity(tableName = "stores")
@TypeConverters(DateConverter::class)
data class StoreEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val storeDescription: String?,
    val imagePath: String?,
    val isActive: Boolean,
    val createdAt: Date,
    val updatedAt: Date
)

