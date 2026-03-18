package com.indybrain.indypos_Android.presentation.addonmanagement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.domain.repository.AddonRepository
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
 * ViewModel for AddOn Management screen
 */
@HiltViewModel
class AddOnManagementViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
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

    
    private val _uiState = MutableStateFlow(AddOnManagementUiState())
    val uiState: StateFlow<AddOnManagementUiState> = _uiState.asStateFlow()
    
    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private val pageSize = 20
    
    init {
        // Load addons will be called from screen's ON_RESUME lifecycle
    }
    
    /**
     * Load addons - uses paginated API when online, Room when offline
     * @param clearError if true, clears errorMessage when starting (default).
     * @param page page to load (1 = first page, resets list)
     */
    private fun loadAddons(clearError: Boolean = true, page: Int = 1) {
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
                val result = addonRepository.getAddonsPaginated(
                    page = page,
                    limit = pageSize,
                    search = searchQueryFlow.value.takeIf { it.isNotBlank() }
                )
                
                result.onSuccess { paginatedResult ->
                    _uiState.update { current ->
                        val pendingDeleteIds = current.pendingDeleteAddonIds
                        val visibleAddons = paginatedResult.addons.filterNot { 
                            pendingDeleteIds.contains(it.id) 
                        }
                        val existingAddons = if (isFirstPage) emptyList() else (current.filteredAddons ?: current.addons ?: emptyList())
                        val newAddons = if (isFirstPage) visibleAddons else existingAddons + visibleAddons
                        
                        current.copy(
                            addons = newAddons,
                            filteredAddons = newAddons,
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
                            errorMessage = error.message ?: getLocalizedString(R.string.addon_management_error_loading),
                            isDeleteError = false
                        )
                    }
                }
            } else {
                loadAddonsFromRoomAndUpdateState()
            }
        }
    }
    
    /**
     * Load addons from Room - used when offline
     */
    private suspend fun loadAddonsFromRoomAndUpdateState() {
        val addons = addonRepository.getAllAddonsForManagementFlow().first()
        val query = searchQueryFlow.value
        val filtered = addons.filter { addon ->
            query.isBlank() || addon.name.contains(query, ignoreCase = true)
        }
        val sorted = filtered.sortedWith(
            compareBy<AddonEntity> { if (it.isSynced) 1 else 0 }.thenBy { it.name }
        )
        _uiState.update { current ->
            val pendingDeleteIds = current.pendingDeleteAddonIds
            val visibleAll = addons.filter { !pendingDeleteIds.contains(it.id) }
            val visibleFiltered = sorted.filter { !pendingDeleteIds.contains(it.id) }
            current.copy(
                addons = visibleAll,
                filteredAddons = visibleFiltered,
                searchQuery = query,
                currentPage = 1,
                totalPages = 1,
                totalCount = visibleFiltered.size,
                hasNextPage = false,
                isLoading = false,
                isLoadingMore = false
            )
        }
    }
    
    /**
     * Refresh addons (load page 1)
     * @param preserveErrorMessage if true, keeps current errorMessage (e.g. when refreshing after delete fail so error popup can show)
     */
    fun refreshAddons(preserveErrorMessage: Boolean = false) {
        loadAddons(clearError = !preserveErrorMessage, page = 1)
    }
    
    /**
     * Refresh addons and clear search - used when returning from Add/Edit Addon screen.
     * Resets search to empty and reloads page 1.
     */
    fun refreshAddonsAndClearSearch() {
        searchJob?.cancel()
        searchQueryFlow.value = ""
        _uiState.update { it.copy(searchQuery = "") }
        loadAddons(clearError = true, page = 1)
    }
    
    /**
     * Load more addons (next page)
     */
    fun loadMoreAddons() {
        val state = _uiState.value
        if (!state.hasNextPage || state.isLoadingMore || state.isLoading) return
        loadAddons(clearError = true, page = state.currentPage + 1)
    }
    
    /**
     * Search addons - debounced, resets to page 1
     */
    fun searchAddons(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(searchQuery = query, selectedAddonIds = emptySet()) }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            loadAddons(clearError = true, page = 1)
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
                        errorMessage = error.message ?: getLocalizedString(R.string.addon_management_error_updating_status),
                        toggleSuccessMessage = null,
                        isDeleteError = false
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
     * Delete addon (single)
     */
    fun deleteAddon(addonId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = addonRepository.deleteAddon(addonId)
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        deleteSuccessMessage = getLocalizedString(R.string.addon_management_delete_success_single)
                    )
                }
                refreshAddons()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_addon_generic),
                        isDeleteError = true
                    )
                }
                refreshAddons(preserveErrorMessage = true)
            }
        }
    }
    
    /**
     * Delete selected addons (batch)
     * Uses batch API, shows same error as multi fail when has failed items,
     * always refreshes from API after success or fail
     */
    fun deleteSelectedAddons() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedAddonIds.toList()
            if (selectedIds.isEmpty()) {
                _uiState.update { 
                    it.copy(errorMessage = getLocalizedString(R.string.addon_management_error_select_to_delete), isDeleteError = false)
                }
                return@launch
            }
            
            _uiState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                    pendingDeleteAddonIds = selectedIds.toSet(),
                    selectedAddonIds = emptySet(),
                    isSelectionMode = false
                )
            }
            
            val result = addonRepository.deleteMultipleAddons(selectedIds)
            
            result.onSuccess { deleteResult ->
                val (deletedCount, failedCount, _) = deleteResult
                val successMessage: String?
                val errorMessage: String?
                
                when {
                    failedCount == 0 && deletedCount > 0 -> {
                        successMessage = if (deletedCount == 1) {
                            getLocalizedString(R.string.addon_management_delete_success_single)
                        } else {
                            getLocalizedString(R.string.addon_management_delete_success_multiple, deletedCount)
                        }
                        errorMessage = null
                    }
                    failedCount >= 1 -> {
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_addon_not_found)
                    }
                    else -> {
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_addon_not_found)
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        errorMessage = errorMessage,
                        pendingDeleteAddonIds = emptySet()
                    )
                }
                refreshAddons(preserveErrorMessage = errorMessage != null)
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_addon_generic),
                        deleteSuccessMessage = null,
                        pendingDeleteAddonIds = emptySet(),
                        isDeleteError = true
                    )
                }
                refreshAddons(preserveErrorMessage = true)
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
     * Clear error message. If error was from delete operation, refresh addons.
     */
    fun clearError() {
        val wasDeleteError = _uiState.value.isDeleteError
        _uiState.update { it.copy(errorMessage = null, isDeleteError = false) }
        if (wasDeleteError) {
            refreshAddons()
        }
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
                loadAddons(page = 1)
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.addon_management_error_syncing),
                        isDeleteError = false
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

