package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(): Flow<List<CartItemEntity>>
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
}

