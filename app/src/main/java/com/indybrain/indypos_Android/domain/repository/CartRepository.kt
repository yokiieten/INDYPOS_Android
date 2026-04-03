package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun getCartItems(): Flow<List<CartItemEntity>>
    suspend fun getCartItemsSync(): List<CartItemEntity>
    suspend fun getCartAddonsByItemId(itemId: String): List<CartAddonEntity>
    fun getCartItemCount(): Flow<Int>
    
    /**
     * Get total quantity for a product in cart (for badge display in product list).
     * Uses productId reference - matches iOS logic.
     */
    suspend fun getQuantityForProduct(productId: String): Int
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

    /**
     * Ensures [ProductEntity] and optional [CategoryEntity] rows exist in Room so [CartItemEntity]
     * foreign keys succeed when adding from API-loaded product lists.
     */
    suspend fun ensureProductForCart(product: ProductEntity, category: CategoryEntity? = null)
    
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

    /**
     * After the POS main list API returns, upsert categories/products that appear in the cart into Room,
     * refresh selected addon rows from the product detail API (name/price/group),
     * then refresh denormalized cart line snapshots (unit price = product base + addon totals).
     */
    suspend fun syncCartRelatedCatalogFromPosList(
        categories: List<CategoryEntity>,
        products: List<ProductEntity>
    )
}

