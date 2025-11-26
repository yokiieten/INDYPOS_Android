package com.indybrain.indypos_Android.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.indybrain.indypos_Android.data.local.converter.DateConverter
import com.indybrain.indypos_Android.data.local.dao.*
import com.indybrain.indypos_Android.data.local.entity.*

@Database(
    entities = [
        OrderEntity::class, 
        OrderItemEntity::class, 
        OrderAddonEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        AddonGroupEntity::class,
        AddonEntity::class,
        CartItemEntity::class,
        CartAddonEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class IndyPosDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun orderAddonDao(): OrderAddonDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun addonGroupDao(): AddonGroupDao
    abstract fun addonDao(): AddonDao
    abstract fun cartDao(): CartDao
}

