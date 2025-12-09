package com.indybrain.indypos_Android.presentation.addonmanagement

/**
 * UI state for Add/Edit Addon screen
 */
data class AddEditAddonUiState(
    val addonName: String = "",
    val addonPrice: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isOfflineSuccess: Boolean = false // Track if success was offline
)

