package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val cartRepository: CartRepository
) : ViewModel() {
    
    val cartItemCount = cartRepository.getCartItemCount()
    val cartItems = cartRepository.getCartItems()
    
    private val _uiState = MutableStateFlow(MainProductUiState())
    val uiState: StateFlow<MainProductUiState> = _uiState.asStateFlow()
    
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    
    init {
        // Start observing local Room data immediately
        observeCategories()
        observeProducts()
        // Load latest products from API once when ViewModel is created
        loadProducts()
    }
    
    /**
     * Load products when screen opens
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Try to fetch from API if connected, otherwise use local data
            if (networkConnectivityChecker.isConnected()) {
                val result = productRepository.fetchAndSaveProducts()
                result.onSuccess {
                    // After successfully fetching and saving products, restore productId for cart items
                    // that may have been set to null due to previous deleteAll() calls
                    val products = productRepository.getAllActiveProducts().first()
                    cartRepository.restoreProductIdsForCartItems(products)
                    // isLoading will be set to false by observeProducts() when data arrives
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
            } else {
                // No internet, data will be loaded from Room via Flow
                // isLoading will be set to false by observeProducts() when data arrives
            }
        }
    }
    
    /**
     * Observe categories from Room database
     */
    private fun observeCategories() {
        viewModelScope.launch {
            productRepository.getAllActiveCategories().collect { categories ->
                _uiState.update { current ->
                    val sortedCategories = categories.sortedBy { it.sortOrder }
                    val firstCategoryId = sortedCategories.firstOrNull()?.id
                    // Set initial focused category to first category if not set
                    val newFocusedCategoryId = current.focusedCategoryId ?: firstCategoryId
                    current.copy(
                        categories = categories,
                        // Don't update isLoading here - let it be managed by loadProducts()
                        focusedCategoryId = newFocusedCategoryId,
                        selectedCategoryId = current.selectedCategoryId ?: firstCategoryId
                    )
                }
            }
        }
    }
    
    /**
     * Observe products from Room database
     * Always show all products - category selection is for scrolling, not filtering
     * Filter out products without categoryId (null or empty string)
     */
    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.getAllActiveProducts().collect { allProducts ->
                // Filter out products without categoryId (null or empty/blank string)
                val productsWithCategory = allProducts.filter { 
                    it.categoryId != null && it.categoryId.isNotBlank() 
                }
                _uiState.update { current ->
                    // Set isLoading to false when we receive data from Room (even if empty)
                    // This ensures loading state is cleared after initial data load
                    current.copy(
                        allProducts = productsWithCategory,
                        products = productsWithCategory, // Always show all products with category
                        isLoading = false // Clear loading state once we have data from Room
                    )
                }
            }
        }
    }
    
    /**
     * Select a category filter
     */
    fun selectCategory(categoryId: String?) {
        selectedCategoryFlow.value = categoryId
        _uiState.update { current ->
            current.copy(
                selectedCategoryId = categoryId,
                focusedCategoryId = categoryId // Also update focused category when manually selected
            )
        }
    }
    
    /**
     * Update focused category when scrolling
     */
    fun updateFocusedCategory(categoryId: String?) {
        _uiState.update { it.copy(focusedCategoryId = categoryId) }
    }
}

