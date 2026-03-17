package com.indybrain.indypos_Android.presentation.products

import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: ProductEntity? = null,
    val category: CategoryEntity? = null,
    val addonGroups: List<AddonGroupEntity> = emptyList(),
    val addonsByGroup: Map<String, List<AddonEntity>> = emptyMap(),
    val selectedAddons: Map<String, Set<String>> = emptyMap(), // Map<AddonGroupId, Set<AddonId>>
    val quantity: Int = 1,
    val specialRequest: String = "",
    val errorMessage: String? = null,
    val isAddToCartSuccess: Boolean = false,
    // If not null, we are editing an existing cart item (from Order / ProductEdit)
    val editingCartItemId: String? = null
)


