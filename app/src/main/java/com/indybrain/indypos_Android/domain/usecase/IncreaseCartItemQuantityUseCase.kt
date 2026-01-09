package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class IncreaseCartItemQuantityUseCase @Inject constructor(
    private val repository: CartRepository
) {
    operator fun invoke(cartItemId: String): Flow<Result<Unit>> = flow {
        val cartItem = repository.getCartItemById(cartItemId)
            ?: return@flow emit(Result.failure(IllegalStateException("Cart item not found")))
        
        val newQuantity = cartItem.quantity + 1
        
        // Get all cart items for this product to calculate total quantity
        val allCartItemsForProduct = repository.getCartItemsByProduct(cartItem.product.id).first()
        val currentTotalQuantity = allCartItemsForProduct.sumOf { it.quantity }
        
        // Calculate total quantity after increase
        // We need to replace the current cartItem.quantity with newQuantity
        val totalQuantityAfterIncrease = currentTotalQuantity - cartItem.quantity + newQuantity
        
        // Check stock availability - check if stock is enabled first
        val product = cartItem.product
        if (product.isStockEnabled == true && product.stockQuantity != null) {
            val hasStock = repository.checkStockAvailability(
                cartItem.product.id,
                totalQuantityAfterIncrease
            )
            
            if (!hasStock) {
                val stockQuantity = product.stockQuantity ?: 0
                val availableStock = stockQuantity - (currentTotalQuantity - cartItem.quantity)
                val errorMessage = if (availableStock > 0) {
                    "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                } else {
                    "สินค้าในสต็อกไม่เพียงพอ"
                }
                emit(Result.failure(Exception(errorMessage)))
                return@flow
            }
        }
        
        // Update quantity
        val result = repository.updateCartItemQuantity(cartItemId, newQuantity)
        emit(result)
    }
}

