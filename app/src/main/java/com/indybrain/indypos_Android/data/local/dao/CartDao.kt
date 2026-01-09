package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemWithAddons
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
    
    // Product Edit Screen queries
    @Transaction
    @Query("""
        SELECT * FROM cart_items 
        WHERE productId = :productId 
        ORDER BY createdAt ASC
    """)
    fun getCartItemsByProduct(productId: String): Flow<List<CartItemWithAddons>>
    
    @Transaction
    @Query("SELECT * FROM cart_items ORDER BY createdAt ASC")
    fun getAllCartItemsWithAddons(): Flow<List<CartItemWithAddons>>
    
    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Int)
    
    @Transaction
    @Query("SELECT * FROM cart_items WHERE id = :id LIMIT 1")
    suspend fun getCartItemById(id: String): CartItemWithAddons?
    
    @Query("DELETE FROM cart_addons WHERE cartItemId = :cartItemId")
    suspend fun deleteSelectedAddons(cartItemId: String)
    
    @Query("DELETE FROM cart_addons WHERE cartItemId IN (:cartItemIds)")
    suspend fun deleteSelectedAddonsByCartItemIds(cartItemIds: List<String>)
    
    @Transaction
    suspend fun deleteCartItemGroup(cartItemIds: List<String>) {
        // Delete selected addons first
        deleteSelectedAddonsByCartItemIds(cartItemIds)
        // Then delete cart items
        cartItemIds.forEach { cartItemId ->
            deleteCartItem(cartItemId)
        }
    }

    @Query("""
        UPDATE cart_items 
        SET specialRequest = :specialRequest, unitPrice = :unitPrice 
        WHERE id = :id
    """)
    suspend fun updateCartItemConfiguration(
        id: String,
        specialRequest: String?,
        unitPrice: Double
    )
}

