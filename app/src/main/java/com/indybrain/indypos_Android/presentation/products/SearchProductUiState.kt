package com.indybrain.indypos_Android.presentation.products

import com.indybrain.indypos_Android.data.local.entity.ProductEntity

/**
 * UI state for SearchProductScreen
 */
data class SearchProductUiState(
    val searchQuery: String = "",
    val products: List<ProductEntity> = emptyList(),
    val currentPage: Int = 1,
    val hasNext: Boolean = false,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)
