package com.indybrain.indypos_Android.presentation.productmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Product Management screen
 */
@HiltViewModel
class ProductManagementViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductManagementUiState())
    val uiState: StateFlow<ProductManagementUiState> = _uiState.asStateFlow()
    
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    
    init {
        // Observe products and categories from Room database
        observeProducts()
        observeCategories()
        // Load products when ViewModel is created
        loadProducts()
    }
    
    /**
     * Load products - sync from API first, then load from Room
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Check internet connectivity
            if (networkConnectivityChecker.isConnected()) {
                // Has internet - sync from API first
                val result = productRepository.syncAllProductData()
                result.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
                // On success, data will be updated via observeProducts() Flow
            } else {
                // No internet - data will be loaded from Room via Flow
                // isLoading will be set to false by observeProducts() when data arrives
            }
        }
    }
    
    /**
     * Observe products from Room database
     */
    private fun observeProducts() {
        viewModelScope.launch {
            combine(
                productRepository.getAllProductsForManagement(),
                searchQueryFlow,
                selectedCategoryFlow
            ) { products, query, categoryId ->
                // Filter products
                val filtered = products.filter { product ->
                    val matchesSearch = query.isBlank() || 
                        product.name?.contains(query, ignoreCase = true) == true
                    val matchesCategory = categoryId == null || product.categoryId == categoryId
                    matchesSearch && matchesCategory
                }
                // Sort: unsynced first, then by name
                val sorted = filtered.sortedWith(
                    compareBy<ProductEntity> { if (it.isSynced == true) 1 else 0 }
                        .thenBy { it.name ?: "" }
                )
                Triple(products, sorted, query)
            }.collect { (allProducts, filteredProducts, query) ->
                _uiState.update { current ->
                    // ซ่อนสินค้าใน UI ทันทีถ้ากำลังถูกลบหลายรายการอยู่
                    val pendingDeleteIds = current.pendingDeleteProductIds
                    val visibleAllProducts = allProducts.filter { product ->
                        val id = product.id
                        id == null || !pendingDeleteIds.contains(id)
                    }
                    val visibleFilteredProducts = filteredProducts.filter { product ->
                        val id = product.id
                        id == null || !pendingDeleteIds.contains(id)
                    }

                    current.copy(
                        products = visibleAllProducts,
                        filteredProducts = visibleFilteredProducts,
                        searchQuery = query,
                        isLoading = false // Clear loading state once we have data from Room
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
            productRepository.getAllCategoriesFlow().collect { categories ->
                _uiState.update { current ->
                    current.copy(
                        categories = categories.sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                    )
                }
            }
        }
    }
    
    /**
     * Refresh products
     */
    fun refreshProducts() {
        loadProducts()
    }
    
    /**
     * Search products
     */
    fun searchProducts(query: String) {
        searchQueryFlow.value = query
        _uiState.update { current ->
            if (query.isBlank()) {
                // Clear selection when clearing search
                current.copy(selectedProductIds = emptySet())
            } else {
                current.copy(selectedProductIds = emptySet())
            }
        }
    }
    
    /**
     * Select category filter
     */
    fun selectCategory(categoryId: String?) {
        selectedCategoryFlow.value = categoryId
        _uiState.update { current ->
            current.copy(
                selectedCategoryId = categoryId,
                selectedProductIds = emptySet() // Clear selection when changing category
            )
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
                    selectedProductIds = emptySet()
                )
            } else {
                // Enter selection mode
                current.copy(isSelectionMode = true)
            }
        }
    }
    
    /**
     * Toggle product selection
     */
    fun toggleProductSelection(productId: String) {
        _uiState.update { current ->
            val newSelection = if (current.selectedProductIds.contains(productId)) {
                current.selectedProductIds - productId
            } else {
                current.selectedProductIds + productId
            }
            current.copy(selectedProductIds = newSelection)
        }
    }
    
    /**
     * Select all products
     */
    fun selectAllProducts() {
        _uiState.update { current ->
            val products = current.filteredProducts ?: emptyList()
            val allProductIds = products.mapNotNull { it.id }.toSet()
            current.copy(selectedProductIds = allProductIds)
        }
    }
    
    /**
     * Deselect all products
     */
    fun deselectAllProducts() {
        _uiState.update { it.copy(selectedProductIds = emptySet()) }
    }
    
    /**
     * Toggle product status (activate/deactivate)
     */
    fun toggleProductStatus(productId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, toggleSuccessMessage = null) }
            
            val newStatus = !currentStatus
            val result = productRepository.toggleProductStatus(productId, newStatus)
            
            result.onSuccess { product ->
                val productName = product.name ?: "สินค้า"
                val statusText = if (newStatus) "เปิดใช้งาน" else "ปิดใช้งาน"
                val successMessage = "$productName ได้รับการ $statusText"
                
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
     * Delete product
     */
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Get product name before deleting
            val product = productRepository.getProductById(productId)
            val productName = product?.name ?: "สินค้า"
            
            val result = productRepository.deleteProduct(productId)
            
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
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการลบสินค้า"
                    )
                }
            }
        }
    }
    
    /**
     * Delete selected products
     */
    fun deleteSelectedProducts() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedProductIds.toList()
            if (selectedIds.isEmpty()) {
                _uiState.update { 
                    it.copy(errorMessage = "กรุณาเลือกสินค้าที่ต้องการลบ")
                }
                return@launch
            }
            
            // ตั้งสถานะให้รู้ว่ารายการเหล่านี้กำลังถูกลบ และซ่อนออกจาก UI เลย
            _uiState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                    deleteSuccessMessage = null,
                    pendingDeleteProductIds = selectedIds.toSet(),
                    // ออกจากโหมดเลือกและล้าง selection ทันที
                    selectedProductIds = emptySet(),
                    isSelectionMode = false
                )
            }
            
            val result = productRepository.deleteMultipleProducts(selectedIds)
            
            result.onSuccess {
                val count = selectedIds.size
                val successMessage = if (count == 1) {
                    "ลบสินค้าสำเร็จ"
                } else {
                    "ลบสินค้า $count รายการสำเร็จ"
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        errorMessage = null,
                        pendingDeleteProductIds = emptySet()
                    )
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการลบสินค้า",
                        deleteSuccessMessage = null,
                        pendingDeleteProductIds = emptySet()
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
            val stats = productRepository.getSyncStatistics()
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

