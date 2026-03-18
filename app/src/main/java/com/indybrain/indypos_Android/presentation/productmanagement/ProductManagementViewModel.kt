package com.indybrain.indypos_Android.presentation.productmanagement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Product Management screen
 */
@HiltViewModel
class ProductManagementViewModel @Inject constructor(
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
    
    private val _uiState = MutableStateFlow(ProductManagementUiState())
    val uiState: StateFlow<ProductManagementUiState> = _uiState.asStateFlow()
    
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    
    private var searchJob: Job? = null
    private val pageSize = 20
    
    init {
        observeCategories()
        // Sync categories when online (for dropdown) - load products on ON_RESUME
        viewModelScope.launch {
            if (networkConnectivityChecker.isConnected()) {
                productRepository.fetchAndSyncCategories()
            }
        }
    }
    
    /**
     * Load products - uses paginated API when online, Room when offline
     * @param clearError if true, clears errorMessage when starting (default). Set false when refreshing after delete fail to preserve error popup.
     * @param page page to load (1 = first page, resets list)
     */
    private fun loadProducts(clearError: Boolean = true, page: Int = 1) {
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
                // Online - use paginated API
                val result = productRepository.getProductsPaginated(
                    page = page,
                    limit = pageSize,
                    search = searchQueryFlow.value.takeIf { it.isNotBlank() },
                    categoryId = selectedCategoryFlow.value
                )
                
                result.onSuccess { paginatedResult ->
                    _uiState.update { current ->
                        val pendingDeleteIds = current.pendingDeleteProductIds
                        val visibleProducts = paginatedResult.products.filter { 
                            it.id == null || !pendingDeleteIds.contains(it.id) 
                        }
                        val existingProducts = if (isFirstPage) emptyList() else (current.filteredProducts ?: emptyList())
                        val newProducts = if (isFirstPage) visibleProducts else existingProducts + visibleProducts
                        
                        current.copy(
                            products = if (isFirstPage) newProducts else (current.products ?: emptyList()) + paginatedResult.products,
                            filteredProducts = newProducts,
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
                // Offline - fall back to Room
                loadProductsFromRoomAndUpdateState()
            }
        }
    }
    
    /**
     * Load products from Room and update state - used when offline
     */
    private suspend fun loadProductsFromRoomAndUpdateState() {
        val products = productRepository.getAllProductsForManagement().first()
        val query = searchQueryFlow.value
        val categoryId = selectedCategoryFlow.value
        val filtered = products.filter { product ->
            val matchesSearch = query.isBlank() || product.name?.contains(query, ignoreCase = true) == true
            val matchesCategory = categoryId == null || product.categoryId == categoryId
            matchesSearch && matchesCategory
        }
        val sorted = filtered.sortedWith(
            compareBy<ProductEntity> { if (it.isSynced == true) 1 else 0 }.thenBy { it.name ?: "" }
        )
        _uiState.update { current ->
            val pendingDeleteIds = current.pendingDeleteProductIds
            val visibleAll = products.filter { it.id == null || !pendingDeleteIds.contains(it.id) }
            val visibleFiltered = sorted.filter { it.id == null || !pendingDeleteIds.contains(it.id) }
            current.copy(
                products = visibleAll,
                filteredProducts = visibleFiltered,
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
     * Refresh products (load page 1)
     * @param preserveErrorMessage if true, keeps current errorMessage (e.g. when refreshing after delete fail so error popup can show)
     */
    fun refreshProducts(preserveErrorMessage: Boolean = false) {
        loadProducts(clearError = !preserveErrorMessage, page = 1)
    }
    
    /**
     * Load more products (next page)
     */
    fun loadMoreProducts() {
        val state = _uiState.value
        if (!state.hasNextPage || state.isLoadingMore || state.isLoading) return
        loadProducts(clearError = true, page = state.currentPage + 1)
    }
    
    /**
     * Search products - debounced, resets to page 1
     */
    fun searchProducts(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(selectedProductIds = emptySet()) }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            loadProducts(clearError = true, page = 1)
        }
    }
    
    /**
     * Select category filter - resets to page 1
     */
    fun selectCategory(categoryId: String?) {
        selectedCategoryFlow.value = categoryId
        _uiState.update { current ->
            current.copy(
                selectedCategoryId = categoryId,
                selectedProductIds = emptySet()
            )
        }
        loadProducts(clearError = true, page = 1)
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
                        errorMessage = null,
                        deleteSuccessMessage = getLocalizedString(R.string.product_delete_success_single)
                    )
                }
                refreshProducts()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_generic),
                        deleteSuccessMessage = null
                    )
                }
                refreshProducts(preserveErrorMessage = true)
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
                    it.copy(errorMessage = getLocalizedString(R.string.product_delete_select_required))
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
            
            result.onSuccess { deleteResult ->
                val (deletedCount, failedCount, errors) = deleteResult
                val successMessage: String?
                val errorMessage: String?
                
                when {
                    failedCount == 0 && deletedCount > 0 -> {
                        successMessage = if (deletedCount == 1) {
                            getLocalizedString(R.string.product_delete_success_single)
                        } else {
                            getLocalizedString(R.string.product_delete_success_multiple, deletedCount)
                        }
                        errorMessage = null
                    }
                    failedCount >= 1 -> {
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_product_not_found)
                    }
                    else -> {
                        successMessage = null
                        errorMessage = getLocalizedString(R.string.api_error_delete_product_not_found)
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        deleteSuccessMessage = successMessage,
                        errorMessage = errorMessage,
                        pendingDeleteProductIds = emptySet()
                    )
                }
                refreshProducts(preserveErrorMessage = errorMessage != null)
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: getLocalizedString(R.string.api_error_delete_generic),
                        deleteSuccessMessage = null,
                        pendingDeleteProductIds = emptySet()
                    )
                }
                refreshProducts(preserveErrorMessage = true)
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
     * Clear error message and refresh page
     */
    fun clearError() {
        refreshProducts()
    }
}

