package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(tableName = "addon_groups")
@TypeConverters(DateConverter::class)
data class AddonGroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isRequired: Boolean,
    val isSingleSelection: Boolean,
    val maxSelection: Int?,
    val minSelection: Int?,
    val sortOrder: Int,
    val isActive: Boolean,
    val userId: Int,
    val createdAt: Date,
    val updatedAt: Date
)

