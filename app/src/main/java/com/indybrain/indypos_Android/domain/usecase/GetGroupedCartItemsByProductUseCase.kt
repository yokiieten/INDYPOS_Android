package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.model.GroupedCartItem
import com.indybrain.indypos_Android.domain.model.configurationKey
import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetGroupedCartItemsByProductUseCase @Inject constructor(
    private val repository: CartRepository
) {
    operator fun invoke(productId: String): Flow<List<GroupedCartItem>> {
        return repository.getCartItemsByProduct(productId)
            .map { cartItems ->
                // Get all cart items to calculate original indices
                val allCartItems = repository.getCartItemsDomain().first()
                
                // Group cart items
                val grouped = cartItems.groupBy { item ->
                    item.configurationKey(includeProductId = true)
                }
                
                // Convert to GroupedCartItem
                grouped.map { (key, items) ->
                    val originalIndices = items.mapNotNull { item ->
                        allCartItems.indexOfFirst { it.id == item.id }
                    }.filter { it >= 0 }
                    
                    GroupedCartItem(
                        key = key,
                        items = items,
                        originalIndices = originalIndices,
                        totalQuantity = items.sumOf { it.quantity }
                    )
                }.sortedBy { group ->
                    group.items.firstOrNull()?.createdAt?.time ?: 0L
                }
            }
    }
}

