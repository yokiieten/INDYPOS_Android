package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.CartDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.mapper.CartItemMapper
import com.indybrain.indypos_Android.domain.model.CartItem
import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val addonDao: AddonDao,
    private val mapper: CartItemMapper
) : CartRepository {
    
    override fun getCartItems(): Flow<List<CartItemEntity>> {
        return cartDao.getAllCartItems()
    }
    
    override suspend fun getCartAddonsByItemId(itemId: String): List<CartAddonEntity> {
        return cartDao.getCartAddonsByItemId(itemId)
    }
    
    override fun getCartItemCount(): Flow<Int> {
        return cartDao.getCartItemCount()
    }
    
    override suspend fun addToCart(
        productId: String?,
        productName: String,
        productImageUrl: String?,
        productColorHex: String?,
        unitPrice: Double,
        quantity: Int,
        specialRequest: String?,
        addons: List<CartAddonEntity>
    ) {
        // Generate UUID for cart item id
        val cartItemId = UUID.randomUUID().toString()
        
        val cartItem = CartItemEntity(
            id = cartItemId,
            productId = productId,
            quantity = quantity,
            specialRequest = specialRequest,
            createdAt = Date(),
            // Extra fields (snapshot data for display)
            productName = productName,
            productImageUrl = productImageUrl,
            productColorHex = productColorHex,
            unitPrice = unitPrice
        )
        
        cartDao.insertCartItem(cartItem)
        
        // Update addons with cartItemId
        val addonsWithItemId = addons.map { it.copy(cartItemId = cartItemId) }
        cartDao.insertCartAddons(addonsWithItemId)
    }
    
    override suspend fun deleteCartItem(itemId: String) {
        cartDao.deleteCartItemWithAddons(itemId)
    }
    
    /**
     * Clear all cart items and addons
     * WARNING: This should ONLY be called when user logs out
     * Cart items are persisted in Room database and should remain
     * when navigating between screens or reopening the app
     */
    override suspend fun clearCart() {
        android.util.Log.d("CartRepository", "clearCart() called - Stack trace: ${Thread.currentThread().stackTrace.joinToString("\n")}")
        cartDao.deleteAllCartItems()
        cartDao.deleteAllCartAddons()
        android.util.Log.d("CartRepository", "clearCart() completed - Cart has been cleared")
    }
    
    override suspend fun restoreProductIdsForCartItems(products: List<com.indybrain.indypos_Android.data.local.entity.ProductEntity>) {
        // Get all cart items with null productId
        val cartItemsWithNullProductId = cartDao.getCartItemsWithNullProductId()
        
        // Create a map of product name to product id for quick lookup
        val productNameToIdMap = products.associateBy { it.name }
        
        // Restore productId for each cart item by matching product name
        cartItemsWithNullProductId.forEach { cartItem ->
            val productId = productNameToIdMap[cartItem.productName]?.id
            if (productId != null) {
                cartDao.updateCartItemProductId(cartItem.id, productId)
            }
        }
    }
    
    override suspend fun clearCartItemsByProduct(productId: String) {
        cartDao.deleteCartItemsByProductId(productId)
    }
    
    // Product Edit Screen methods
    override fun getCartItemsByProduct(productId: String): Flow<List<CartItem>> {
        return cartDao.getCartItemsByProduct(productId)
            .map { entities ->
                entities.mapNotNull { entity ->
                    // Get product
                    val product = productDao.getProductById(productId) 
                        ?: return@mapNotNull null
                    
                    // Get all addon IDs from selected addons
                    val addonIds = entity.cartAddons.map { it.addonId }
                    val addonEntities = addonDao.getAddonsByIds(addonIds)
                    
                    // Group addons by groupId
                    val addonsByGroup = entity.cartAddons
                        .groupBy { it.addonGroupId }
                        .mapValues { (_, cartAddons) ->
                            cartAddons.mapNotNull { cartAddon ->
                                addonEntities.find { it.id == cartAddon.addonId }
                            }
                        }
                    
                    mapper.toDomain(entity, product, addonsByGroup)
                }
            }
    }
    
    override fun getCartItemsDomain(): Flow<List<CartItem>> {
        return cartDao.getAllCartItemsWithAddons()
            .map { entities ->
                entities.mapNotNull { entity ->
                    val productId = entity.cartItem.productId ?: return@mapNotNull null
                    val product = productDao.getProductById(productId) ?: return@mapNotNull null
                    
                    // Get all addon IDs from selected addons
                    val addonIds = entity.cartAddons.map { it.addonId }
                    val addonEntities = addonDao.getAddonsByIds(addonIds)
                    
                    // Group addons by groupId
                    val addonsByGroup = entity.cartAddons
                        .groupBy { it.addonGroupId }
                        .mapValues { (_, cartAddons) ->
                            cartAddons.mapNotNull { cartAddon ->
                                addonEntities.find { it.id == cartAddon.addonId }
                            }
                        }
                    
                    mapper.toDomain(entity, product, addonsByGroup)
                }
            }
    }
    
    override suspend fun getCartItemById(cartItemId: String): CartItem? {
        val entity = cartDao.getCartItemById(cartItemId) ?: return null
        val productId = entity.cartItem.productId ?: return null
        val product = productDao.getProductById(productId) ?: return null
        
        // Get addons mapping
        val addonIds = entity.cartAddons.map { it.addonId }
        val addonEntities = addonDao.getAddonsByIds(addonIds)
        val addonsByGroup = entity.cartAddons
            .groupBy { it.addonGroupId }
            .mapValues { (_, cartAddons) ->
                cartAddons.mapNotNull { cartAddon ->
                    addonEntities.find { it.id == cartAddon.addonId }
                }
            }
        
        return mapper.toDomain(entity, product, addonsByGroup)
    }
    
    override suspend fun updateCartItemQuantity(
        cartItemId: String, 
        quantity: Int
    ): Result<Unit> {
        return try {
            cartDao.updateQuantity(cartItemId, quantity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteCartItems(cartItemIds: List<String>): Result<Unit> {
        return try {
            cartDao.deleteCartItemGroup(cartItemIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun checkStockAvailability(
        productId: String, 
        quantity: Int
    ): Boolean {
        val product = productDao.getProductById(productId) ?: return false
        val availableStock = product.stockQuantity ?: return true // If no stock limit, allow
        return quantity <= availableStock
    }
}

