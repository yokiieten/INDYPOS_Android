package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.OrderAddonEntity

@Dao
interface OrderAddonDao {
    
    @Query("SELECT * FROM order_addons WHERE orderItemId = :orderItemId")
    suspend fun getOrderAddons(orderItemId: String): List<OrderAddonEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderAddon(addon: OrderAddonEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderAddons(addons: List<OrderAddonEntity>)
    
    @Query("DELETE FROM order_addons WHERE orderItemId = :orderItemId")
    suspend fun deleteOrderAddons(orderItemId: String)
    
    @Query("DELETE FROM order_addons")
    suspend fun deleteAllOrderAddons()
}

