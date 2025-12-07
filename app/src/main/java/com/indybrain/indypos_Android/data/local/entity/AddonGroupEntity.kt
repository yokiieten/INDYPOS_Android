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
    val isActive: Boolean,
    val isDeletedLocally: Boolean = false,
    val isFromServer: Boolean = true,
    val isSynced: Boolean = true,
    val createdAt: Date,
    val updatedAt: Date,
    // Extra fields (not in Core Data but kept for compatibility)
    val minSelection: Int? = null,
    val sortOrder: Int? = null,
    val userId: Int? = null
)

