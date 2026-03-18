package com.indybrain.indypos_Android.presentation.addongroupmanagement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
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
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val languageLocalDataSource: LanguageLocalDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private fun getLocalizedString(resId: Int): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId)
    }

    private fun getLocalizedString(resId: Int, vararg formatArgs: Any): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId, *formatArgs)
    }
    
    private val _uiState = MutableStateFlow(AddonGroupManagementUiState())
    val uiState: StateFlow<AddonGroupManagementUiState> = _uiState.asStateFlow()
    
    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private val pageSize = 20
    
    init {
        // Load addon groups on ON_RESUME
    }
    
    /**
     * Load addon groups - uses paginated API when online, Room when offline
     * @param clearError if true, clears errorMessage when starting (default).
     * @param page page to load (1 = first page, resets list)
     */
    private fun loadAddonGroups(clearError: Boolean = true, page: Int = 1) {
        viewModelScope.launch {
            val isFirstPage = page == 1
            _uiState.update { 
                it.copy(
                    isLoading = isFirstPage,
                    isLoadingMore = !isFirstPage,
                    errorMessage = if (clearError) null else it.errorMessage
                )
            }
            
            if (networkConnectivityChecker.isConnected()) {
                val result = addonGroupRepository.getAddonGroupsPaginated(
                    page = page,
                    limit = pageSize,
                    search = searchQueryFlow.value.takeIf { it.isNotBlank() }
                )
                
                result.onSuccess { paginatedResult ->
                    _uiState.update { current ->
                        val pendingDeleteIds = current.pendingDeleteAddonGroupIds
                        val visibleGroups = paginatedResult.addonGroups.filterNot { 
                            pendingDeleteIds.contains(it.id) 
                        }
                        val existingGroups = if (isFirstPage) emptyList() else (current.filteredAddonGroups ?: current.addonGroups ?: emptyList())
                        val newGroups = if (isFirstPage) visibleGroups else existingGroups + visibleGroups
                        val newCounts = if (isFirstPage) paginatedResult.addonCounts else current.addonCounts + paginatedResult.addonCounts
                        
                        current.copy(
                            addonGroups = if (isFirstPage) newGroups else (current.addonGroups ?: emptyList()) + paginatedResult.addonGroups,
                            filteredAddonGroups = newGroups,
                            addonCounts = newCounts,
                            currentPage = paginatedResult.currentPage,
                            totalPages = paginatedResult.totalPages,
                            totalCount = paginatedResult.totalCount,
                            hasNextPage = paginatedResult.hasNext,
                            isLoading = false,
                            isLoadingMore = false
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
            } else {
                loadAddonGroupsFromRoomAndUpdateState()
            }
        }
    }
    
    /**
     * Load addon groups from Room - used when offline
     */
    private suspend fun loadAddonGroupsFromRoomAndUpdateState() {
        val groupsWithCount = addonGroupRepository.getAllAddonGroupsWithCountFlow().first()
        val sortedGroups = groupsWithCount.sortedBy { it.addonGroup.sortOrder ?: 0 }
        val addonGroups = sortedGroups.map { it.addonGroup }
        val counts = sortedGroups.associate { it.addonGroup.id to it.addonCount }
        _uiState.update { current ->
            val pendingDeleteIds = current.pendingDeleteAddonGroupIds
            val visibleAddonGroups = addonGroups.filterNot { pendingDeleteIds.contains(it.id) }
            val filtered = if (current.searchQuery.isNotBlank()) {
                visibleAddonGroups.filter { it.name.contains(current.searchQuery, ignoreCase = true) }
            } else null
            current.copy(
                addonGroups = visibleAddonGroups,
                filteredAddonGroups = filtered ?: visibleAddonGroups,
                addonCounts = counts,
                currentPage = 1,
                totalPages = 1,
                totalCount = visibleAddonGroups.size,
                hasNextPage = false,
                isLoading = false,
                isLoadingMore = false
            )
        }
    }
    
    /**
     * Refresh addon groups (load page 1)
     * @param preserveErrorMessage if true, keeps current errorMessage (e.g. when refreshing after delete fail so error popup can show)
     */
    fun refreshAddonGroups(preserveErrorMessage: Boolean = false) {
        loadAddonGroups(clearError = !preserveErrorMessage, page = 1)
    }
    
    /**
     * Load more addon groups (next page)
     */
    fun loadMoreAddonGroups() {
        val state = _uiState.value
        if (!state.hasNextPage || state.isLoadingMore || state.isLoading) return
        loadAddonGroups(clearError = true, page = state.currentPage + 1)
    }
    
    /**
     * Search addon groups - debounced, resets to page 1
     */
    fun searchAddonGroups(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(searchQuery = query) }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            loadAddonGroups(clearError = true, page = 1)
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
                val statusText = if (newStatus) {
                    getLocalizedString(R.string.addon_group_status_activated)
                } else {
                    getLocalizedString(R.string.addon_group_status_deactivated)
                }
                val successMessage = getLocalizedString(R.string.addon_group_toggle_success, addonGroupName, statusText)
                
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
     * Delete addon group (single)
     */
    fun deleteAddonGroup(addonGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Get addon group name before deleting
            val addonGroup = addonGroupRepository.getAddonGroupById(addonGroupId)
            val addonGroupName = addonGroup?.name ?: "กลุ่ม Addon"
            
            val result = addonGroupRepository.deleteAddonGroup(addonGroupId)
            
            result.onSuccess {
                val successMessage = getLocalizedString(R.string.addon_group_delete_success_single_with_name, addonGroupName)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage
                    )
                }
                refreshAddonGroups()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_addon_group_generic)
                    )
                }
                refreshAddonGroups(preserveErrorMessage = true)
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
     * Select all addon groups (from visible/filtered list)
     */
    fun selectAllAddonGroups() {
        _uiState.update { current ->
            val groupsToSelect = current.filteredAddonGroups ?: current.addonGroups ?: emptyList()
            val allAddonGroupIds = groupsToSelect.map { it.id }.toSet()
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
     * Delete selected addon groups (batch)
     * Uses batch API, shows same error as product multi fail when has failed items,
     * always refreshes from API after success or fail
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
                    selectedAddonGroupIds = emptySet(),
                    isEditMode = false
                )
            }
            
            val result = addonGroupRepository.deleteMultipleAddonGroups(selectedIds)
            
            result.onSuccess { deleteResult ->
                val (deletedCount, failedCount, _) = deleteResult
                val successMessage: String?
                val errorMessage: String?
                
                when {
                    failedCount == 0 && deletedCount > 0 -> {
                        successMessage = if (deletedCount == 1) {
                            getLocalizedString(R.string.addon_group_delete_success_single)
                        } else {
                            getLocalizedString(R.string.addon_group_delete_success_multiple, deletedCount)
                        }
                        errorMessage = null
                    }
                    failedCount >= 1 -> {
                        // มี failed - แสดง error เดียวกับ product multi fail
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_addon_group_not_found)
                    }
                    else -> {
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_addon_group_not_found)
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        errorMessage = errorMessage,
                        pendingDeleteAddonGroupIds = emptySet()
                    )
                }
                refreshAddonGroups(preserveErrorMessage = errorMessage != null)
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_addon_group_generic),
                        pendingDeleteAddonGroupIds = emptySet()
                    )
                }
                refreshAddonGroups(preserveErrorMessage = true)
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

