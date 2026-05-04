package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.model.GroupedCartItem
import com.indybrain.indypos_Android.domain.model.configurationKey
import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetGroupedCartItemsUseCase @Inject constructor(
    private val repository: CartRepository
) {
    operator fun invoke(): Flow<List<GroupedCartItem>> {
        return repository.getCartItemsDomain()
            .map { cartItems ->
                // Group cart items by productId and configuration (addons, specialRequest)
                // Items with same productId, addons, and specialRequest are grouped together
                val grouped = cartItems.groupBy { item ->
                    item.configurationKey(includeProductId = true)
                }
                
                // Convert to GroupedCartItem
                grouped.map { (key, items) ->
                    val originalIndices = items.mapNotNull { item ->
                        cartItems.indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
                    }
                    
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

