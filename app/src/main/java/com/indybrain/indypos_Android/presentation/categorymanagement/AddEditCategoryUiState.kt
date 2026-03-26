package com.indybrain.indypos_Android.presentation.categorymanagement

/**
 * UI state for Add/Edit Category screen
 */
data class AddEditCategoryUiState(
    val categoryName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val loadedSortOrder: Int = 0,
    val loadedIsActive: Boolean = true
)


