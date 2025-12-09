package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DecreaseCartItemQuantityUseCase @Inject constructor(
    private val repository: CartRepository
) {
    operator fun invoke(cartItemId: String): Flow<Result<Unit>> = flow {
        val cartItem = repository.getCartItemById(cartItemId)
            ?: return@flow emit(Result.failure(IllegalStateException("Cart item not found")))
        
        if (cartItem.quantity > 1) {
            // Decrease quantity
            val result = repository.updateCartItemQuantity(cartItemId, cartItem.quantity - 1)
            emit(result)
        } else {
            // Delete item when quantity is 1
            val result = repository.deleteCartItems(listOf(cartItemId))
            emit(result)
        }
    }
}

