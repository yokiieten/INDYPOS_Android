package com.indybrain.indypos_Android.presentation.productmanagement

/**
 * UI state for Add/Edit Product screen
 */
data class AddEditProductUiState(
    val productName: String = "",
    val productCode: String = "",
    val sellingPrice: String = "",
    val costPrice: String = "",
    val unit: String = "",
    val imageUrl: String? = null,
    val selectedColorHex: String? = null,
    val isImageSelected: Boolean = true, // true for image, false for color
    val categoryId: String? = null,
    val isSkuEnabled: Boolean = false,
    val skuCode: String = "",
    val isStockEnabled: Boolean = false,
    val stockQuantity: String = "",
    val hasAdditionalOptions: Boolean = false,
    val addonGroupIds: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val loadingMessage: String? = null, // Custom loading message
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    // Add category dialog state
    val showAddCategoryDialog: Boolean = false,
    val categoryName: String = "",
    val isCreatingCategory: Boolean = false,
    val categoryError: String? = null,
    val categorySuccess: String? = null,
    // Network error dialog
    val showNoInternetDialog: Boolean = false,
    // Image upload error dialog
    val showImageUploadErrorDialog: Boolean = false
)

