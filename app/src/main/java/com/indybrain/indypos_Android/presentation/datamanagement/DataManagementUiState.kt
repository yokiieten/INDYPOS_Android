package com.indybrain.indypos_Android.presentation.datamanagement

data class DataManagementUiState(
    val isLoading: Boolean = false,
    val productCount: Int = 0,
    val categoryCount: Int = 0,
    val addonCount: Int = 0,
    val addonGroupCount: Int = 0,
    val orderCount: Int = 0,
    val errorMessage: String? = null
)

