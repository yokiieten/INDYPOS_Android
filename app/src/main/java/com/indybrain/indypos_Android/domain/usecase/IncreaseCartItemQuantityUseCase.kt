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

        
        // Check stock availability
        val hasStock = repository.checkStockAvailability(
            cartItem.product.id,
            newQuantity
        )
//
//        if (!hasStock) {
//            emit(Result.failure(InsufficientStockException("Insufficient stock")))
//            return@flow
//        }
        
        // Update quantity
        val result = repository.updateCartItemQuantity(cartItemId, newQuantity)
        emit(result)
    }
}

