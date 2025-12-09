package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for Many-to-Many relationship between AddonGroupEntity and AddonEntity
 */
@Entity(
    tableName = "addon_group_addon_junction",
    primaryKeys = ["addonGroupId", "addonId"],
    foreignKeys = [
        ForeignKey(
            entity = AddonGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["addonGroupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AddonEntity::class,
            parentColumns = ["id"],
            childColumns = ["addonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["addonGroupId"]),
        Index(value = ["addonId"])
    ]
)
data class AddonGroupAddonJunctionEntity(
    val addonGroupId: String,
    val addonId: String,
    val sortOrder: Int = 0
)

