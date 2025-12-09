package com.indybrain.indypos_Android.presentation.addonmanagement

import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.domain.repository.AddonSyncStatistics

/**
 * UI state for AddOn Management Screen
 */
data class AddOnManagementUiState(
    val isLoading: Boolean = false,
    val addons: List<AddonEntity> = emptyList(),
    val filteredAddons: List<AddonEntity> = emptyList(),
    val searchQuery: String = "",
    val isSelectionMode: Boolean = false,
    val selectedAddonIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val toggleSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val syncSuccessMessage: String? = null,
    val syncStatistics: AddonSyncStatistics? = null
)

