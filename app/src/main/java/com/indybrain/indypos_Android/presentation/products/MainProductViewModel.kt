package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainProductUiState())
    val uiState: StateFlow<MainProductUiState> = _uiState.asStateFlow()
    
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    
    init {
        observeCategories()
        observeProducts()
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
                result.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
            } else {
                // No internet, data will be loaded from Room via Flow
                _uiState.update { it.copy(isLoading = false) }
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
                        isLoading = false,
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
     */
    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.getAllActiveProducts().collect { allProducts ->
                _uiState.update { current ->
                    current.copy(
                        allProducts = allProducts,
                        products = allProducts // Always show all products
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

