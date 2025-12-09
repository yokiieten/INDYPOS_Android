package com.indybrain.indypos_Android.presentation.products

import com.indybrain.indypos_Android.data.local.entity.ProductEntity

/**
 * UI state for SearchProductScreen
 */
data class SearchProductUiState(
    val isLoading: Boolean = false,
    val allProducts: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

