package com.indybrain.indypos_Android.presentation.products

import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity

/**
 * UI state for MainProductScreen
 */
data class MainProductUiState(
    val isLoading: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val allProducts: List<ProductEntity> = emptyList(), // All products for scroll tracking
    val products: List<ProductEntity> = emptyList(), // Filtered products based on selected category
    val selectedCategoryId: String? = null,
    val focusedCategoryId: String? = null,
    val errorMessage: String? = null
)

