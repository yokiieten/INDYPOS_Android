package com.indybrain.indypos_Android.data.local.entity

/**
 * Parsed addon line (e.g. from JSON on [OrderItemEntity.addons]); not a Room table.
 */
data class OrderAddonEntity(
    val id: Long = 0L,
    val orderItemId: String,
    val addonId: String,
    val addonName: String,
    val addonPrice: Double,
    val quantity: Int
)
