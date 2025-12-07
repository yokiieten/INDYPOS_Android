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
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Check internet connectivity
            if (networkConnectivityChecker.isConnected()) {
                // Has internet - fetch from API and sync with Room
                val result = productRepository.fetchAndSyncCategories()
                result.onSuccess {
                    // Data will be updated via observeCategories() Flow
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
        }
    }
    
    /**
     * Observe categories from Room database
     */
    private fun observeCategories() {
        viewModelScope.launch {
            productRepository.getAllCategoriesFlow().collect { categories ->
                _uiState.update { current ->
                    current.copy(
                        categories = categories.sortedBy { it.sortOrder },
                        isLoading = false // Clear loading state once we have data from Room
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
            val filteredCategories = if (query.isBlank()) {
                current.categories
            } else {
                current.categories.filter { 
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
        _uiState.update { it.copy(searchQuery = "", filteredCategories = emptyList()) }
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
            val allCategoryIds = current.categories.map { it.id }.toSet()
            current.copy(selectedCategoryIds = allCategoryIds)
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
}





