package com.indybrain.indypos_Android.presentation.productmanagement

import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.repository.ProductSyncStatistics

/**
 * UI state for Product Management Screen
 */
data class ProductManagementUiState(
    val isLoading: Boolean = false,
    val products: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedProductIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val toggleSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val syncStatistics: ProductSyncStatistics? = null
)

