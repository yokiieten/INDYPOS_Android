package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Data class for AddonGroup with its related Addons (Many-to-Many relationship)
 */
data class AddonGroupWithAddons(
    @Embedded
    val addonGroup: AddonGroupEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AddonGroupAddonJunctionEntity::class,
            parentColumn = "addonGroupId",
            entityColumn = "addonId"
        )
    )
    val addons: List<AddonEntity>
)

