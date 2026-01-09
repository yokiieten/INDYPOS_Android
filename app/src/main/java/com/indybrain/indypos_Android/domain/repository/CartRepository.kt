package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(): Flow<List<CartItemEntity>>
    suspend fun getCartItemsSync(): List<CartItemEntity>
    suspend fun getCartAddonsByItemId(itemId: String): List<CartAddonEntity>
    fun getCartItemCount(): Flow<Int>
    suspend fun addToCart(
        productId: String?,
        productName: String,
        productImageUrl: String?,
        productColorHex: String?,
        unitPrice: Double,
        quantity: Int,
        specialRequest: String?,
        addons: List<CartAddonEntity>
    )
    suspend fun deleteCartItem(itemId: String)
    suspend fun clearCart()
    suspend fun restoreProductIdsForCartItems(products: List<com.indybrain.indypos_Android.data.local.entity.ProductEntity>)
    suspend fun clearCartItemsByProduct(productId: String)
    
    // Product Edit Screen methods
    fun getCartItemsByProduct(productId: String): Flow<List<CartItem>>
    fun getCartItemsDomain(): Flow<List<CartItem>>
    suspend fun getCartItemById(cartItemId: String): CartItem?
    suspend fun updateCartItemQuantity(cartItemId: String, quantity: Int): Result<Unit>
    suspend fun updateCartItemConfiguration(
        cartItemId: String,
        specialRequest: String?,
        unitPrice: Double,
        addons: List<CartAddonEntity>
    ): Result<Unit>
    suspend fun deleteCartItems(cartItemIds: List<String>): Result<Unit>
    suspend fun checkStockAvailability(productId: String, quantity: Int): Boolean
}

