package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for Many-to-Many relationship between SelectedAddonEntity and AddonEntity
 */
@Entity(
    tableName = "selected_addon_junction",
    primaryKeys = ["selectedAddonId", "addonId"],
    foreignKeys = [
        ForeignKey(
            entity = SelectedAddonEntity::class,
            parentColumns = ["id"],
            childColumns = ["selectedAddonId"],
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
        Index(value = ["selectedAddonId"]),
        Index(value = ["addonId"])
    ]
)
data class SelectedAddonJunctionEntity(
    val selectedAddonId: String,
    val addonId: String
)


