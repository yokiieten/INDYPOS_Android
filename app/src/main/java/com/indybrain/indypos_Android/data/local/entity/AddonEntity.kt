package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import java.util.Date

@Entity(
    tableName = "addons",
    foreignKeys = [
        ForeignKey(
            entity = AddonGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["addonGroupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
@TypeConverters(DateConverter::class)
data class AddonEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val price: Double,
    val sortOrder: Int,
    val isActive: Boolean,
    val userId: Int,
    val addonGroupId: String?,
    val createdAt: Date,
    val updatedAt: Date
)

