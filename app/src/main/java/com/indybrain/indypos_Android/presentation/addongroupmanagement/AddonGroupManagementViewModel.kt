package com.indybrain.indypos_Android.presentation.addongroupmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Addon Group Management screen
 */
@HiltViewModel
class AddonGroupManagementViewModel @Inject constructor(
    private val addonGroupRepository: AddonGroupRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddonGroupManagementUiState())
    val uiState: StateFlow<AddonGroupManagementUiState> = _uiState.asStateFlow()
    
    init {
        // Observe addon groups from Room database
        observeAddonGroups()
        // Load addon groups when ViewModel is created
        loadAddonGroups()
    }
    
    /**
     * Load addon groups - check internet and fetch from API or load from Room
     */
    fun loadAddonGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Check internet connectivity
            if (networkConnectivityChecker.isConnected()) {
                // Has internet - fetch from API and sync with Room
                val result = addonGroupRepository.fetchAndSyncAddonGroups()
                result.onSuccess {
                    // Data will be updated via observeAddonGroups() Flow
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
            } else {
                // No internet - data will be loaded from Room via Flow
                // isLoading will be set to false by observeAddonGroups() when data arrives
            }
        }
    }
    
    /**
     * Observe addon groups from Room database
     */
    private fun observeAddonGroups() {
        viewModelScope.launch {
            addonGroupRepository.getAllAddonGroupsWithCountFlow().collect { groupsWithCount ->
                val sortedGroups = groupsWithCount.sortedBy { it.addonGroup.sortOrder ?: 0 }
                val addonGroups = sortedGroups.map { it.addonGroup }
                val counts = sortedGroups.associate { it.addonGroup.id to it.addonCount }
                
                _uiState.update { current ->
                    // ถ้ามีกลุ่มที่กำลังถูกลบหลายรายการอยู่ ให้ซ่อนออกจาก UI เลย
                    val pendingDeleteIds = current.pendingDeleteAddonGroupIds
                    val visibleAddonGroups = addonGroups.filterNot { pendingDeleteIds.contains(it.id) }
                    
                    // Re-apply search filter if there's an active search query
                    val filteredAddonGroups = if (current.searchQuery.isNotBlank()) {
                        visibleAddonGroups.filter { 
                            it.name.contains(current.searchQuery, ignoreCase = true) 
                        }
                    } else {
                        null // Clear filter when query is blank
                    }
                    
                    current.copy(
                        addonGroups = visibleAddonGroups,
                        filteredAddonGroups = filteredAddonGroups,
                        addonCounts = counts,
                        isLoading = false // Clear loading state once we have data from Room
                    )
                }
            }
        }
    }
    
    /**
     * Refresh addon groups
     */
    fun refreshAddonGroups() {
        loadAddonGroups()
    }
    
    /**
     * Search addon groups
     */
    fun searchAddonGroups(query: String) {
        _uiState.update { current ->
            val addonGroups = current.addonGroups ?: emptyList()
            val filteredAddonGroups = if (query.isBlank()) {
                null // Clear filter when query is blank
            } else {
                addonGroups.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
            }
            current.copy(searchQuery = query, filteredAddonGroups = filteredAddonGroups)
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", filteredAddonGroups = null) }
    }
    
    /**
     * Toggle addon group status (activate/deactivate)
     */
    fun toggleAddonGroupStatus(addonGroupId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val newStatus = !currentStatus
            val result = addonGroupRepository.toggleAddonGroupStatus(addonGroupId, newStatus)
            
            result.onSuccess { addonGroup ->
                // Get addon group name for success message
                val addonGroupName = addonGroup.name
                val statusText = if (newStatus) "เปิดใช้งาน" else "ปิดใช้งาน"
                val successMessage = "อัปเดตสถานะกลุ่ม Addon '$addonGroupName' เป็น '$statusText' เรียบร้อยแล้ว"
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        toggleSuccessMessage = successMessage
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะ"
                    )
                }
            }
        }
    }
    
    /**
     * Dismiss toggle success message
     */
    fun dismissToggleSuccess() {
        _uiState.update { it.copy(toggleSuccessMessage = null) }
    }
    
    /**
     * Delete addon group
     */
    fun deleteAddonGroup(addonGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Get addon group name before deleting
            val addonGroup = addonGroupRepository.getAddonGroupById(addonGroupId)
            val addonGroupName = addonGroup?.name ?: "กลุ่ม Addon"
            
            val result = addonGroupRepository.deleteAddonGroup(addonGroupId)
            
            result.onSuccess {
                val successMessage = "ลบกลุ่ม Addon '$addonGroupName' เรียบร้อยแล้ว"
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
                    )
                }
            }
        }
    }
    
    /**
     * Dismiss delete success message
     */
    fun dismissDeleteSuccess() {
        _uiState.update { it.copy(deleteSuccessMessage = null) }
    }
    
    /**
     * Toggle edit mode
     */
    fun toggleEditMode() {
        _uiState.update { current ->
            if (current.isEditMode) {
                // Exit edit mode - clear selections
                current.copy(
                    isEditMode = false,
                    selectedAddonGroupIds = emptySet()
                )
            } else {
                // Enter edit mode
                current.copy(isEditMode = true)
            }
        }
    }
    
    /**
     * Toggle addon group selection
     */
    fun toggleAddonGroupSelection(addonGroupId: String) {
        _uiState.update { current ->
            val newSelection = if (current.selectedAddonGroupIds.contains(addonGroupId)) {
                current.selectedAddonGroupIds - addonGroupId
            } else {
                current.selectedAddonGroupIds + addonGroupId
            }
            current.copy(selectedAddonGroupIds = newSelection)
        }
    }
    
    /**
     * Select all addon groups
     */
    fun selectAllAddonGroups() {
        _uiState.update { current ->
            val addonGroups = current.addonGroups ?: emptyList()
            val allAddonGroupIds = addonGroups.map { it.id }.toSet()
            current.copy(selectedAddonGroupIds = allAddonGroupIds)
        }
    }
    
    /**
     * Select specific addon groups by IDs
     */
    fun selectAddonGroups(addonGroupIds: Set<String>) {
        _uiState.update { current ->
            current.copy(selectedAddonGroupIds = addonGroupIds)
        }
    }
    
    /**
     * Deselect all addon groups
     */
    fun deselectAllAddonGroups() {
        _uiState.update { current ->
            current.copy(selectedAddonGroupIds = emptySet())
        }
    }
    
    /**
     * Delete selected addon groups
     */
    fun deleteSelectedAddonGroups() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedAddonGroupIds.toList()
            if (selectedIds.isEmpty()) return@launch
            
            // ตั้งสถานะให้รู้ว่ารายการเหล่านี้กำลังถูกลบ และซ่อนออกจาก UI เลย
            _uiState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                    pendingDeleteAddonGroupIds = selectedIds.toSet(),
                    // ออกจากโหมดแก้ไขและล้าง selection ทันที
                    selectedAddonGroupIds = emptySet(),
                    isEditMode = false
                )
            }
            
            var successCount = 0
            var failureMessage: String? = null
            
            selectedIds.forEach { addonGroupId ->
                val result = addonGroupRepository.deleteAddonGroup(addonGroupId)
                result.onSuccess {
                    successCount++
                }.onFailure { error ->
                    // เก็บข้อความ error ไว้ แต่ยังพยายามลบตัวถัดไปต่อ
                    failureMessage = error.message ?: "เกิดข้อผิดพลาดในการลบกลุ่ม Addon"
                }
            }
            
            if (failureMessage != null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = failureMessage,
                        pendingDeleteAddonGroupIds = emptySet()
                    )
                }
            } else {
                val successMessage = if (successCount == 1) {
                    "ลบกลุ่ม Addon เรียบร้อยแล้ว"
                } else {
                    "ลบกลุ่ม Addon $successCount รายการเรียบร้อยแล้ว"
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        pendingDeleteAddonGroupIds = emptySet()
                    )
                }
            }
        }
    }
    
    /**
     * Sync addon groups to server
     */
    fun syncAddonGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, syncSuccessMessage = null) }
            
            val result = addonGroupRepository.syncAddonGroups()
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        syncSuccessMessage = "Sync กลุ่มตัวเลือกเพิ่มเติมสำเร็จ"
                    )
                }
                // Refresh addon groups after sync
                loadAddonGroups()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการ sync กลุ่มตัวเลือกเพิ่มเติม"
                    )
                }
            }
        }
    }
    
    /**
     * Dismiss sync success message
     */
    fun dismissSyncSuccess() {
        _uiState.update { it.copy(syncSuccessMessage = null) }
    }
    
    /**
     * Load sync statistics
     */
    fun loadSyncStatistics() {
        viewModelScope.launch {
            val stats = addonGroupRepository.getSyncStatistics()
            _uiState.update { it.copy(syncStatistics = stats) }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

