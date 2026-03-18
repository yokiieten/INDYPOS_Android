package com.indybrain.indypos_Android.presentation.categorymanagement

import com.indybrain.indypos_Android.data.local.entity.CategoryEntity

/**
 * UI state for Category Management screen
 */
data class CategoryManagementUiState(
    val categories: List<CategoryEntity>? = null, // null means data hasn't been loaded yet
    val filteredCategories: List<CategoryEntity>? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true, // Start with loading = true
    val isLoadingMore: Boolean = false, // Loading next page
    val hasNextPage: Boolean = false,
    val currentPage: Int = 1,
    val errorMessage: String? = null,
    val toggleSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val syncSuccessMessage: String? = null,
    val isEditMode: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet(),
    val syncStatistics: CategorySyncStatistics? = null,
    // IDs ที่กำลังอยู่ระหว่างการลบแบบหลายรายการ เพื่อไม่ให้ UI แสดงไล่ลบทีละอัน
    val pendingDeleteCategoryIds: Set<String> = emptySet()
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






