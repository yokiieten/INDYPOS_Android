package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartAddons(addons: List<CartAddonEntity>)
    
    @Query("SELECT * FROM cart_items ORDER BY createdAt DESC")
    fun getAllCartItems(): Flow<List<CartItemEntity>>
    
    @Query("SELECT * FROM cart_items ORDER BY createdAt DESC")
    suspend fun getAllCartItemsSync(): List<CartItemEntity>
    
    @Query("SELECT COUNT(*) FROM cart_items")
    fun getCartItemCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM cart_items")
    suspend fun getCartItemCountSync(): Int
    
    @Query("SELECT * FROM cart_addons WHERE cartItemId = :cartItemId")
    suspend fun getCartAddonsByItemId(cartItemId: String): List<CartAddonEntity>
    
    @Query("DELETE FROM cart_items WHERE id = :itemId")
    suspend fun deleteCartItem(itemId: String)
    
    @Query("DELETE FROM cart_items")
    suspend fun deleteAllCartItems()
    
    @Query("DELETE FROM cart_addons")
    suspend fun deleteAllCartAddons()
    
    @Query("UPDATE cart_items SET productId = :productId WHERE id = :itemId")
    suspend fun updateCartItemProductId(itemId: String, productId: String?)
    
    @Query("SELECT * FROM cart_items WHERE productId IS NULL")
    suspend fun getCartItemsWithNullProductId(): List<CartItemEntity>
    
    @Transaction
    suspend fun deleteCartItemWithAddons(itemId: String) {
        deleteCartItem(itemId)
        // Addons will be deleted automatically due to CASCADE
    }
    
    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItemsByProductId(productId: String)
}

