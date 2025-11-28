package com.indybrain.indypos_Android.presentation.categorymanagement

import com.indybrain.indypos_Android.data.local.entity.CategoryEntity

/**
 * UI state for Category Management screen
 */
data class CategoryManagementUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val filteredCategories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

