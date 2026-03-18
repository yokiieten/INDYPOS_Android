package com.indybrain.indypos_Android.presentation.categorymanagement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.repository.CategoriesPaginatedResult
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Category Management screen
 */
@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val productRepository: ProductRepository,
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
    
    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()
    
    init {
        // Load categories will be called from screen's ON_RESUME lifecycle
        // When online: uses paginated API. When offline: uses Room.
    }
    
    /**
     * Load categories - when online uses paginated API, when offline loads from Room
     * @param clearError if true, clears errorMessage when starting (default). Set false when refreshing after delete fail to preserve error popup.
     */
    private fun loadCategories(clearError: Boolean = true) {
        viewModelScope.launch {
            try {
                _uiState.update { 
                    it.copy(
                        isLoading = true,
                        isLoadingMore = false,
                        errorMessage = if (clearError) null else it.errorMessage
                    )
                }
                
                val searchQuery = _uiState.value.searchQuery
                
                if (networkConnectivityChecker.isConnected()) {
                    // Online - use paginated API
                    val result = productRepository.getCategoriesPaginated(
                        page = 1,
                        limit = 20,
                        search = searchQuery.takeIf { it.isNotBlank() }
                    )
                    result.onSuccess { paginatedResult ->
                        _uiState.update { current ->
                            val pendingDeleteIds = current.pendingDeleteCategoryIds
                            val visibleCategories = paginatedResult.categories
                                .filterNot { pendingDeleteIds.contains(it.id) }
                            current.copy(
                                categories = visibleCategories,
                                filteredCategories = null, // API already filters by search
                                isLoading = false,
                                currentPage = paginatedResult.currentPage,
                                hasNextPage = paginatedResult.hasNext,
                                isLoadingMore = false
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                errorMessage = error.message ?: context.getString(R.string.category_management_error_loading)
                            )
                        }
                    }
                } else {
                    // Offline - load from Room
                    val categories = productRepository.getAllCategories()
                        .sortedBy { it.createdAt }
                    _uiState.update { current ->
                        val pendingDeleteIds = current.pendingDeleteCategoryIds
                        val visibleCategories = categories.filterNot { pendingDeleteIds.contains(it.id) }
                        val filtered = if (searchQuery.isNotBlank()) {
                            visibleCategories.filter { it.name.contains(searchQuery, ignoreCase = true) }
                        } else null
                        current.copy(
                            categories = visibleCategories,
                            filteredCategories = filtered,
                            isLoading = false,
                            hasNextPage = false,
                            currentPage = 1
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = context.getString(
                            R.string.category_management_error_loading_with_reason,
                            e.message ?: context.getString(R.string.category_management_error_unknown)
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Refresh categories
     * @param preserveErrorMessage if true, keeps current errorMessage (e.g. when refreshing after delete fail so error popup can show)
     */
    fun refreshCategories(preserveErrorMessage: Boolean = false) {
        loadCategories(clearError = !preserveErrorMessage)
    }
    
    /**
     * Load more categories (next page) - only when online and hasNextPage
     */
    fun loadMoreCategories() {
        val state = _uiState.value
        if (!networkConnectivityChecker.isConnected() || !state.hasNextPage || state.isLoadingMore) return
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingMore = true) }
                val nextPage = state.currentPage + 1
                val result = productRepository.getCategoriesPaginated(
                    page = nextPage,
                    limit = 20,
                    search = state.searchQuery.takeIf { it.isNotBlank() }
                )
                result.onSuccess { paginatedResult ->
                    _uiState.update { current ->
                        val existing = current.categories ?: emptyList()
                        val pendingDeleteIds = current.pendingDeleteCategoryIds
                        val newCategories = paginatedResult.categories
                            .filterNot { pendingDeleteIds.contains(it.id) }
                        val combined = existing + newCategories
                        current.copy(
                            categories = combined,
                            currentPage = paginatedResult.currentPage,
                            hasNextPage = paginatedResult.hasNext,
                            isLoadingMore = false
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
    
    /**
     * Search categories - when online reloads from API with search param, when offline filters client-side.
     * Skips API call when query is "" on initial load (avoids duplicate with ON_RESUME refreshCategories).
     */
    fun searchCategories(query: String) {
        val previousQuery = _uiState.value.searchQuery
        _uiState.update { it.copy(searchQuery = query) }
        if (networkConnectivityChecker.isConnected()) {
            // Skip load when both previous and current are "" - initial load already done by ON_RESUME
            if (query.isNotBlank() || previousQuery.isNotBlank()) {
                loadCategories(clearError = true)
            }
        } else {
            _uiState.update { current ->
                val categories = current.categories ?: emptyList()
                val filteredCategories = if (query.isBlank()) null
                else categories.filter { it.name.contains(query, ignoreCase = true) }
                current.copy(filteredCategories = filteredCategories)
            }
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", filteredCategories = null) }
    }
    
    /**
     * Toggle category status (activate/deactivate)
     */
    fun toggleCategoryStatus(categoryId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val newStatus = !currentStatus
            val result = productRepository.toggleCategoryStatus(categoryId, newStatus)
            
            result.onSuccess { category ->
                // Get category name for success message
                val categoryName = category.name
                val statusText = if (newStatus) {
                    context.getString(R.string.category_management_status_activate)
                } else {
                    context.getString(R.string.category_management_status_deactivate)
                }
                val successMessage = context.getString(
                    R.string.category_management_status_update_success,
                    categoryName,
                    statusText
                )
                
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
                        errorMessage = error.message ?: context.getString(R.string.category_management_error_updating_status)
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
     * Delete category
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Get category name before deleting
            val category = productRepository.getCategoryById(categoryId)
            val categoryName = category?.name ?: context.getString(R.string.category_management_default_name)
            
            val result = productRepository.deleteCategory(categoryId)
            
            result.onSuccess {
                val successMessage = getLocalizedString(
                    R.string.category_management_delete_success_with_name,
                    categoryName
                )
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage
                    )
                }
                refreshCategories()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_category_generic)
                    )
                }
                refreshCategories(preserveErrorMessage = true)
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
                    selectedCategoryIds = emptySet()
                )
            } else {
                // Enter edit mode
                current.copy(isEditMode = true)
            }
        }
    }
    
    /**
     * Toggle category selection
     */
    fun toggleCategorySelection(categoryId: String) {
        _uiState.update { current ->
            val newSelection = if (current.selectedCategoryIds.contains(categoryId)) {
                current.selectedCategoryIds - categoryId
            } else {
                current.selectedCategoryIds + categoryId
            }
            current.copy(selectedCategoryIds = newSelection)
        }
    }
    
    /**
     * Select all categories
     */
    fun selectAllCategories() {
        _uiState.update { current ->
            val categories = current.categories ?: emptyList()
            val allCategoryIds = categories.map { it.id }.toSet()
            current.copy(selectedCategoryIds = allCategoryIds)
        }
    }
    
    /**
     * Select specific categories by IDs
     */
    fun selectCategories(categoryIds: Set<String>) {
        _uiState.update { current ->
            current.copy(selectedCategoryIds = categoryIds)
        }
    }
    
    /**
     * Deselect all categories
     */
    fun deselectAllCategories() {
        _uiState.update { current ->
            current.copy(selectedCategoryIds = emptySet())
        }
    }
    
    /**
     * Delete selected categories (batch)
     * Uses batch API, shows same error as multi fail when has failed items,
     * always refreshes from API after success or fail
     */
    fun deleteSelectedCategories() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedCategoryIds.toList()
            if (selectedIds.isEmpty()) return@launch
            
            _uiState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                    pendingDeleteCategoryIds = selectedIds.toSet(),
                    selectedCategoryIds = emptySet(),
                    isEditMode = false
                )
            }
            
            val result = productRepository.deleteMultipleCategories(selectedIds)
            
            result.onSuccess { deleteResult ->
                val (deletedCount, failedCount, _) = deleteResult
                val successMessage: String?
                val errorMessage: String?
                
                when {
                    failedCount == 0 && deletedCount > 0 -> {
                        successMessage = if (deletedCount == 1) {
                            getLocalizedString(R.string.category_management_delete_success_single)
                        } else {
                            getLocalizedString(R.string.category_management_delete_success_multiple, deletedCount)
                        }
                        errorMessage = null
                    }
                    failedCount >= 1 -> {
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_category_not_found)
                    }
                    else -> {
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_category_not_found)
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        errorMessage = errorMessage,
                        pendingDeleteCategoryIds = emptySet()
                    )
                }
                refreshCategories(preserveErrorMessage = errorMessage != null)
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_category_generic),
                        pendingDeleteCategoryIds = emptySet()
                    )
                }
                refreshCategories(preserveErrorMessage = true)
            }
        }
    }
    
    /**
     * Sync categories to server
     */
    fun syncCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = productRepository.syncCategories()
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        syncSuccessMessage = context.getString(R.string.category_management_sync_success)
                    )
                }
                // Refresh categories after sync
                loadCategories()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: context.getString(R.string.category_management_error_syncing)
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
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Load sync statistics
     */
    fun loadSyncStatistics() {
        viewModelScope.launch {
            val categories = productRepository.getAllCategories()
            val total = categories.size
            val synced = categories.count { it.isSynced }
            val unsynced = categories.count { !it.isSynced }
            val deleted = categories.count { it.isDeletedLocally }
            
            _uiState.update { 
                it.copy(
                    syncStatistics = CategorySyncStatistics(
                        total = total,
                        synced = synced,
                        unsynced = unsynced,
                        deleted = deleted
                    )
                )
            }
        }
    }
}





