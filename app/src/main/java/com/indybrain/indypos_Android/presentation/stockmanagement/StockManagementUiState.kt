package com.indybrain.indypos_Android.presentation.stockmanagement

import com.indybrain.indypos_Android.data.local.entity.ProductEntity

/**
 * UI state for Stock Management Screen
 */
data class StockManagementUiState(
    val isLoading: Boolean = false,
    val products: List<ProductEntity> = emptyList(),
    val errorMessage: String? = null,
    val showStockUpdateDialog: Boolean = false,
    val selectedProduct: ProductEntity? = null,
    val stockUpdateQuantity: String = "",
    val isUpdatingStock: Boolean = false,
    val updateSuccessMessage: String? = null,
    val showNoInternetDialog: Boolean = false
)

