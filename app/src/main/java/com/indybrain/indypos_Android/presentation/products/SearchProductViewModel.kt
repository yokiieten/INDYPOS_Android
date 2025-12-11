package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
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
class SearchProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {
    
    val cartItemCount = cartRepository.getCartItemCount()
    val cartItems = cartRepository.getCartItems()
    
    private val _uiState = MutableStateFlow(SearchProductUiState())
    val uiState: StateFlow<SearchProductUiState> = _uiState.asStateFlow()
    
    init {
        loadProducts()
    }
    
    /**
     * Load products that are active and have active categories
     */
    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Get all active products and active categories
            val productsFlow = productRepository.getAllActiveProducts()
            val categoriesFlow = productRepository.getAllActiveCategories()
            
            combine(productsFlow, categoriesFlow) { products, categories ->
                // Create a set of active category IDs for quick lookup
                val activeCategoryIds = categories.map { it.id }.toSet()
                
                // Filter products that:
                // 1. Are active (already filtered by getAllActiveProducts)
                // 2. Have a categoryId that is not null/blank
                // 3. The category is active
                val validProducts = products.filter { product ->
                    val hasCategoryId = product.categoryId != null && product.categoryId.isNotBlank()
                    val categoryIsActive = product.categoryId?.let { activeCategoryIds.contains(it) } ?: false
                    hasCategoryId && categoryIsActive
                }
                
                validProducts
            }.collect { validProducts ->
                _uiState.update { current ->
                    current.copy(
                        allProducts = validProducts,
                        filteredProducts = if (current.searchQuery.isBlank()) {
                            validProducts
                        } else {
                            filterProducts(validProducts, current.searchQuery)
                        },
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Update search query and filter products
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = if (query.isBlank()) {
                current.allProducts
            } else {
                filterProducts(current.allProducts, query)
            }
            current.copy(
                searchQuery = query,
                filteredProducts = filtered
            )
        }
    }
    
    /**
     * Filter products by name (case-insensitive)
     */
    private fun filterProducts(products: List<ProductEntity>, query: String): List<ProductEntity> {
        val lowerQuery = query.lowercase().trim()
        return products.filter { product ->
            product.name.lowercase().contains(lowerQuery)
        }
    }
}


