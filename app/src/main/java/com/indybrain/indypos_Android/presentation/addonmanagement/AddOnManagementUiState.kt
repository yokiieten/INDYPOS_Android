package com.indybrain.indypos_Android.presentation.addonmanagement

import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.domain.repository.AddonSyncStatistics

/**
 * UI state for AddOn Management Screen
 */
data class AddOnManagementUiState(
    val isLoading: Boolean = true, // Start with loading = true
    val isLoadingMore: Boolean = false,
    val addons: List<AddonEntity>? = null, // null means data hasn't been loaded yet
    val filteredAddons: List<AddonEntity>? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val hasNextPage: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedAddonIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val isDeleteError: Boolean = false, // true when error came from delete (single or multi)
    val toggleSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val syncSuccessMessage: String? = null,
    val syncStatistics: AddonSyncStatistics? = null,
    // IDs ที่กำลังอยู่ระหว่างการลบแบบหลายรายการ เพื่อไม่ให้ UI แสดงไล่ลบทีละอัน
    val pendingDeleteAddonIds: Set<String> = emptySet()
)

