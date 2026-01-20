package com.indybrain.indypos_Android.presentation.categorymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()
    
    init {
        // Observe categories from Room database
        observeCategories()
        // Load categories when ViewModel is created
        loadCategories()
    }
    
    /**
     * Load categories - check internet and fetch from API or load from Room
     */
    fun loadCategories() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                
                // Check internet connectivity
                if (networkConnectivityChecker.isConnected()) {
                    // Has internet - fetch from API and sync with Room
                    val result = productRepository.fetchAndSyncCategories()
                    result.onSuccess {
                        // Data will be updated via observeCategories() Flow
                        // isLoading will be set to false when Flow emits data
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
                    // isLoading will be set to false by observeCategories() when data arrives
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = "เกิดข้อผิดพลาด: ${e.message ?: "ไม่ทราบสาเหตุ"}"
                    )
                }
            }
        }
    }
    
    /**
     * Observe categories from Room database
     */
    private fun observeCategories() {
        viewModelScope.launch {
            try {
                productRepository.getAllCategoriesFlow().collect { categories ->
                    // Always keep the visual order of categories stable and
                    // independent from server-side sort changes (e.g. when
                    // toggling active/inactive status). We therefore rely on
                    // createdAt instead of sortOrder so that enabling/disabling
                    // a category does not move it to the bottom of the list.
                    _uiState.update { current ->
                        current.copy(
                            categories = categories.sortedBy { it.createdAt },
                            isLoading = false // Clear loading state once we have data from Room
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle any exceptions during collection
                _uiState.update { current ->
                    current.copy(
                        categories = emptyList(),
                        isLoading = false,
                        errorMessage = "เกิดข้อผิดพลาดในการโหลดข้อมูล: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Refresh categories
     */
    fun refreshCategories() {
        loadCategories()
    }
    
    /**
     * Search categories
     */
    fun searchCategories(query: String) {
        _uiState.update { current ->
            val categories = current.categories ?: emptyList()
            val filteredCategories = if (query.isBlank()) {
                null // Clear filter when query is blank
            } else {
                categories.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
            }
            current.copy(searchQuery = query, filteredCategories = filteredCategories)
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
                val statusText = if (newStatus) "เปิดใช้งาน" else "ปิดใช้งาน"
                val successMessage = "อัปเดตสถานะหมวดหมู่ '$categoryName' เป็น '$statusText' เรียบร้อยแล้ว"
                
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
     * Delete category
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Get category name before deleting
            val category = productRepository.getCategoryById(categoryId)
            val categoryName = category?.name ?: "หมวดหมู่"
            
            val result = productRepository.deleteCategory(categoryId)
            
            result.onSuccess {
                val successMessage = "ลบหมวดหมู่ '$categoryName' เรียบร้อยแล้ว"
                
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
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการลบหมวดหมู่"
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
     * Delete selected categories
     */
    fun deleteSelectedCategories() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedCategoryIds.toList()
            if (selectedIds.isEmpty()) return@launch
            
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            var successCount = 0
            var failureMessage: String? = null
            
            selectedIds.forEach { categoryId ->
                val result = productRepository.deleteCategory(categoryId)
                result.onSuccess {
                    successCount++
                }.onFailure { error ->
                    failureMessage = error.message ?: "เกิดข้อผิดพลาดในการลบหมวดหมู่"
                }
            }
            
            if (failureMessage != null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = failureMessage
                    )
                }
            } else {
                val successMessage = if (successCount == 1) {
                    "ลบหมวดหมู่เรียบร้อยแล้ว"
                } else {
                    "ลบหมวดหมู่ $successCount รายการเรียบร้อยแล้ว"
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        selectedCategoryIds = emptySet(),
                        isEditMode = false
                    )
                }
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
                        syncSuccessMessage = "Sync หมวดหมู่สำเร็จ"
                    )
                }
                // Refresh categories after sync
                loadCategories()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการ sync หมวดหมู่"
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





