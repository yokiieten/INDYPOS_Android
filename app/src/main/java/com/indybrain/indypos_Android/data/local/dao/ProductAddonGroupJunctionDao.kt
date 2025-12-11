package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.ProductAddonGroupJunctionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductAddonGroupJunctionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(junction: ProductAddonGroupJunctionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(junctions: List<ProductAddonGroupJunctionEntity>)
    
    @Query("SELECT addonGroupId FROM product_addon_group_junction WHERE productId = :productId")
    fun getAddonGroupIdsByProductId(productId: String): Flow<List<String>>
    
    @Query("SELECT addonGroupId FROM product_addon_group_junction WHERE productId = :productId")
    suspend fun getAddonGroupIdsByProductIdSync(productId: String): List<String>
    
    @Query("SELECT productId FROM product_addon_group_junction WHERE addonGroupId = :addonGroupId")
    fun getProductIdsByAddonGroupId(addonGroupId: String): Flow<List<String>>
    
    @Query("SELECT productId FROM product_addon_group_junction WHERE addonGroupId = :addonGroupId")
    suspend fun getProductIdsByAddonGroupIdSync(addonGroupId: String): List<String>
    
    @Query("DELETE FROM product_addon_group_junction WHERE productId = :productId")
    suspend fun deleteByProductId(productId: String)
    
    @Query("DELETE FROM product_addon_group_junction WHERE addonGroupId = :addonGroupId")
    suspend fun deleteByAddonGroupId(addonGroupId: String)
    
    @Query("DELETE FROM product_addon_group_junction WHERE productId = :productId AND addonGroupId = :addonGroupId")
    suspend fun delete(productId: String, addonGroupId: String)
    
    @Query("DELETE FROM product_addon_group_junction")
    suspend fun deleteAll()
}


