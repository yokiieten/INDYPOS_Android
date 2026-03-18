package com.indybrain.indypos_Android.presentation.addongroupmanagement

import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity

/**
 * UI state for Addon Group Management screen
 */
data class AddonGroupManagementUiState(
    val addonGroups: List<AddonGroupEntity>? = null, // null means data hasn't been loaded yet
    val filteredAddonGroups: List<AddonGroupEntity>? = null,
    val addonCounts: Map<String, Int> = emptyMap(),
    val searchQuery: String = "",
    val isLoading: Boolean = true, // Start with loading = true
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val toggleSuccessMessage: String? = null,
    val deleteSuccessMessage: String? = null,
    val syncSuccessMessage: String? = null,
    val isEditMode: Boolean = false,
    val selectedAddonGroupIds: Set<String> = emptySet(),
    val syncStatistics: com.indybrain.indypos_Android.domain.repository.AddonGroupSyncStatistics? = null,
    // Pagination
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val hasNextPage: Boolean = false,
    // IDs ที่กำลังอยู่ระหว่างการลบแบบหลายรายการ เพื่อไม่ให้ UI แสดงไล่ลบทีละอัน
    val pendingDeleteAddonGroupIds: Set<String> = emptySet()
)

