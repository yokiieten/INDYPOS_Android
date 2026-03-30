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
        CategoryEntity::class,
        ProductEntity::class,
        AddonGroupEntity::class,
        AddonEntity::class,
        CartItemEntity::class,
        CartAddonEntity::class,
        ProductAddonGroupJunctionEntity::class,
        AddonGroupAddonJunctionEntity::class,
        ReceiptSettingsEntity::class
    ],
    version = 1, // Reset to version 1 for fresh start (not in production yet)
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class IndyPosDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun addonGroupDao(): AddonGroupDao
    abstract fun addonDao(): AddonDao
    abstract fun cartDao(): CartDao
    abstract fun productAddonGroupJunctionDao(): ProductAddonGroupJunctionDao
    abstract fun addonGroupAddonJunctionDao(): AddonGroupAddonJunctionDao
    abstract fun receiptSettingsDao(): ReceiptSettingsDao

    @Transaction
    suspend fun clearAllData() {
        cartDao().deleteAllCartItems()
        cartDao().deleteAllCartAddons()

        productAddonGroupJunctionDao().deleteAll()
        productDao().deleteAll()
        categoryDao().deleteAll()

        addonGroupAddonJunctionDao().deleteAll()
        addonDao().deleteAll()
        addonGroupDao().deleteAll()

        receiptSettingsDao().deleteAll()
    }
}
