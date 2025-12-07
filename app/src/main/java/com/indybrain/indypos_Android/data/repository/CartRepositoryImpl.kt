package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.data.local.dao.CartDao
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    private val cartDao: CartDao
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
    
    override suspend fun clearCart() {
        cartDao.deleteAllCartItems()
        cartDao.deleteAllCartAddons()
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
}

