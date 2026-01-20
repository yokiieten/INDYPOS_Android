package com.indybrain.indypos_Android.data.local.entity

import androidx.room.Embedded

/**
 * Projection for addon group with its addon count.
 */
data class AddonGroupWithAddonCount(
    @Embedded
    val addonGroup: AddonGroupEntity,
    val addonCount: Int
)

