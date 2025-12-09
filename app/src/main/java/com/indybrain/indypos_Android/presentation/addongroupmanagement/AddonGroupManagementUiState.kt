package com.indybrain.indypos_Android.presentation.addongroupmanagement

import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity

/**
 * UI state for Addon Group Management screen
 */
data class AddonGroupManagementUiState(
    val addonGroups: List<AddonGroupEntity> = emptyList(),
    val filteredAddonGroups: List<AddonGroupEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val toggleSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val isEditMode: Boolean = false,
    val selectedAddonGroupIds: Set<String> = emptySet()
)

