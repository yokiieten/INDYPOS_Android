package com.indybrain.indypos_Android.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import com.indybrain.indypos_Android.data.local.dao.*
import com.indybrain.indypos_Android.data.local.entity.*

@Database(
    entities = [
        // Order entities
        OrderEntity::class, 
        OrderItemEntity::class, 
        OrderAddonEntity::class,
        // Category and Product entities
        CategoryEntity::class,
        ProductEntity::class,
        // Addon entities
        AddonGroupEntity::class,
        AddonEntity::class,
        // Cart entities
        CartItemEntity::class,
        CartAddonEntity::class,
        SelectedAddonEntity::class,
        // Junction tables for Many-to-Many relationships
        ProductAddonGroupJunctionEntity::class,
        AddonGroupAddonJunctionEntity::class,
        SelectedAddonJunctionEntity::class,
        // Store and Settings entities
        StoreEntity::class,
        ReceiptSettingsEntity::class
    ],
    version = 1, // Reset to version 1 for fresh start (not in production yet)
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class IndyPosDatabase : RoomDatabase() {
    // Order DAOs
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun orderAddonDao(): OrderAddonDao
    // Category and Product DAOs
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    // Addon DAOs
    abstract fun addonGroupDao(): AddonGroupDao
    abstract fun addonDao(): AddonDao
    // Cart DAOs
    abstract fun cartDao(): CartDao
    abstract fun selectedAddonDao(): SelectedAddonDao
    // Junction table DAOs
    abstract fun productAddonGroupJunctionDao(): ProductAddonGroupJunctionDao
    abstract fun addonGroupAddonJunctionDao(): AddonGroupAddonJunctionDao
    abstract fun selectedAddonJunctionDao(): SelectedAddonJunctionDao
    // Store and Settings DAOs
    abstract fun storeDao(): StoreDao
    abstract fun receiptSettingsDao(): ReceiptSettingsDao
}

