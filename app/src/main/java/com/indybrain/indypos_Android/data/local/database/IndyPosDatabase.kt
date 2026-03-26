package com.indybrain.indypos_Android.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Transaction
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
        // Junction tables for Many-to-Many relationships
        ProductAddonGroupJunctionEntity::class,
        AddonGroupAddonJunctionEntity::class,
        // Settings entities
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
    // Junction table DAOs
    abstract fun productAddonGroupJunctionDao(): ProductAddonGroupJunctionDao
    abstract fun addonGroupAddonJunctionDao(): AddonGroupAddonJunctionDao
    // Settings DAOs
    abstract fun receiptSettingsDao(): ReceiptSettingsDao
    
    /**
     * Clear all data from Room database
     * This should be called when user logs out to ensure no data persists
     */
    @Transaction
    suspend fun clearAllData() {
        // Clear Cart data
        cartDao().deleteAllCartItems()
        cartDao().deleteAllCartAddons()
        
        // Clear Order data
        orderAddonDao().deleteAllOrderAddons()
        orderItemDao().deleteAllOrderItems()
        orderDao().deleteAllOrders()
        
        // Clear Product and Category data
        productAddonGroupJunctionDao().deleteAll()
        productDao().deleteAll()
        categoryDao().deleteAll()
        
        // Clear Addon data
        addonGroupAddonJunctionDao().deleteAll()
        addonDao().deleteAll()
        addonGroupDao().deleteAll()
        
        // Clear Settings data
        receiptSettingsDao().deleteAll()
    }
}

