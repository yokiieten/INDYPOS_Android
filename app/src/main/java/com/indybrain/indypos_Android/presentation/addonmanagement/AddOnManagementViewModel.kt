package com.indybrain.indypos_Android.presentation.addonmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AddOn Management screen
 */
@HiltViewModel
class AddOnManagementViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddOnManagementUiState())
    val uiState: StateFlow<AddOnManagementUiState> = _uiState.asStateFlow()
    
    private val searchQueryFlow = MutableStateFlow("")
    
    init {
        // Observe addons from Room database
        observeAddons()
        // Load addons when ViewModel is created
        loadAddons()
    }
    
    /**
     * Load addons - sync from API first, then load from Room
     */
    fun loadAddons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Check internet connectivity
            if (networkConnectivityChecker.isConnected()) {
                // Has internet - sync from API first
                val result = addonRepository.fetchAndSyncAddons()
                result.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
                // On success, data will be updated via observeAddons() Flow
            } else {
                // No internet - data will be loaded from Room via Flow
                // isLoading will be set to false by observeAddons() when data arrives
            }
        }
    }
    
    /**
     * Observe addons from Room database
     */
    private fun observeAddons() {
        viewModelScope.launch {
            combine(
                addonRepository.getAllAddonsForManagementFlow(),
                searchQueryFlow
            ) { addons, query ->
                // Filter addons
                val filtered = addons.filter { addon ->
                    query.isBlank() || 
                    addon.name.contains(query, ignoreCase = true)
                }
                // Sort: unsynced first, then by name
                val sorted = filtered.sortedWith(
                    compareBy<AddonEntity> { if (it.isSynced) 1 else 0 }
                        .thenBy { it.name }
                )
                Pair(addons, sorted)
            }.collect { (allAddons, filteredAddons) ->
                _uiState.update { current ->
                    // ซ่อน Addon ใน UI ทันทีถ้ากำลังถูกลบหลายรายการอยู่
                    val pendingDeleteIds = current.pendingDeleteAddonIds
                    val visibleAllAddons = allAddons.filter { addon ->
                        !pendingDeleteIds.contains(addon.id)
                    }
                    val visibleFilteredAddons = filteredAddons.filter { addon ->
                        !pendingDeleteIds.contains(addon.id)
                    }

                    current.copy(
                        addons = visibleAllAddons,
                        filteredAddons = visibleFilteredAddons,
                        searchQuery = searchQueryFlow.value,
                        isLoading = false // Clear loading state once we have data from Room
                    )
                }
            }
        }
    }
    
    /**
     * Refresh addons
     */
    fun refreshAddons() {
        loadAddons()
    }
    
    /**
     * Search addons
     */
    fun searchAddons(query: String) {
        searchQueryFlow.value = query
        _uiState.update { current ->
            if (query.isBlank()) {
                // Clear selection when clearing search
                current.copy(selectedAddonIds = emptySet())
            } else {
                current.copy(selectedAddonIds = emptySet())
            }
        }
    }
    
    /**
     * Toggle selection mode
     */
    fun toggleSelectionMode() {
        _uiState.update { current ->
            if (current.isSelectionMode) {
                // Exit selection mode - clear selections
                current.copy(
                    isSelectionMode = false,
                    selectedAddonIds = emptySet()
                )
            } else {
                // Enter selection mode
                current.copy(isSelectionMode = true)
            }
        }
    }
    
    /**
     * Toggle addon selection
     */
    fun toggleAddonSelection(addonId: String) {
        _uiState.update { current ->
            val newSelection = if (current.selectedAddonIds.contains(addonId)) {
                current.selectedAddonIds - addonId
            } else {
                current.selectedAddonIds + addonId
            }
            current.copy(selectedAddonIds = newSelection)
        }
    }
    
    /**
     * Select all addons
     */
    fun selectAllAddons() {
        _uiState.update { current ->
            val addons = current.filteredAddons ?: emptyList()
            val allAddonIds = addons.map { it.id }.toSet()
            current.copy(selectedAddonIds = allAddonIds)
        }
    }
    
    /**
     * Deselect all addons
     */
    fun deselectAllAddons() {
        _uiState.update { it.copy(selectedAddonIds = emptySet()) }
    }
    
    /**
     * Toggle addon status (activate/deactivate)
     */
    fun toggleAddonStatus(addonId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, toggleSuccessMessage = null) }
            
            val newStatus = !currentStatus
            val result = addonRepository.toggleAddonStatus(addonId, newStatus)
            
            result.onSuccess { addon ->
                val addonName = addon.name
                val statusText = if (newStatus) "เปิดใช้งาน" else "ปิดใช้งาน"
                val successMessage = "$addonName ได้รับการ $statusText"
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        toggleSuccessMessage = successMessage
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะ",
                        toggleSuccessMessage = null
                    )
                }
            }
        }
    }
    
    /**
     * Clear toggle success message
     */
    fun clearToggleSuccessMessage() {
        _uiState.update { it.copy(toggleSuccessMessage = null) }
    }
    
    /**
     * Delete addon
     */
    fun deleteAddon(addonId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Get addon name before deleting
            val addon = addonRepository.getAddonById(addonId)
            val addonName = addon?.name ?: "Addon"
            
            val result = addonRepository.deleteAddon(addonId)
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการลบ Addon"
                    )
                }
            }
        }
    }
    
    /**
     * Delete selected addons
     */
    fun deleteSelectedAddons() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedAddonIds.toList()
            if (selectedIds.isEmpty()) {
                _uiState.update { 
                    it.copy(errorMessage = "กรุณาเลือก Addon ที่ต้องการลบ")
                }
                return@launch
            }
            
            // ตั้งสถานะให้รู้ว่ารายการเหล่านี้กำลังถูกลบ และซ่อนออกจาก UI เลย
            _uiState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                    deleteSuccessMessage = null,
                    pendingDeleteAddonIds = selectedIds.toSet(),
                    // ออกจากโหมดเลือกและล้าง selection ทันที
                    selectedAddonIds = emptySet(),
                    isSelectionMode = false
                )
            }
            
            val result = addonRepository.deleteMultipleAddons(selectedIds)
            
            result.onSuccess {
                val count = selectedIds.size
                val successMessage = if (count == 1) {
                    "ลบ Addon สำเร็จ"
                } else {
                    "ลบ Addon $count รายการสำเร็จ"
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        errorMessage = null,
                        pendingDeleteAddonIds = emptySet()
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการลบ Addon",
                        deleteSuccessMessage = null,
                        pendingDeleteAddonIds = emptySet()
                    )
                }
            }
        }
    }
    
    /**
     * Clear delete success message
     */
    fun clearDeleteSuccessMessage() {
        _uiState.update { it.copy(deleteSuccessMessage = null) }
    }
    
    /**
     * Get sync statistics
     */
    fun loadSyncStatistics() {
        viewModelScope.launch {
            val stats = addonRepository.getSyncStatistics()
            _uiState.update { it.copy(syncStatistics = stats) }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Sync addons to server
     */
    fun syncAddons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, syncSuccessMessage = null) }
            
            val result = addonRepository.syncPendingAddons()
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        syncSuccessMessage = "Sync ตัวเลือกเพิ่มเติมสำเร็จ"
                    )
                }
                // Refresh addons after sync
                loadAddons()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการ sync ตัวเลือกเพิ่มเติม"
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
}

