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
    val errorMessage: String? = null,
    val toggleSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val syncSuccessMessage: String? = null,
    val isEditMode: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet(),
    val syncStatistics: CategorySyncStatistics? = null
)

/**
 * Sync statistics for categories
 */
data class CategorySyncStatistics(
    val total: Int,
    val synced: Int,
    val unsynced: Int,
    val deleted: Int
)






