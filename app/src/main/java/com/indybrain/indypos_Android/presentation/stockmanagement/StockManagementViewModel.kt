package com.indybrain.indypos_Android.presentation.stockmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Stock Management Screen
 */
@HiltViewModel
class StockManagementViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StockManagementUiState())
    val uiState: StateFlow<StockManagementUiState> = _uiState.asStateFlow()
    
    init {
        observeProducts()
        loadProducts()
    }
    
    /**
     * Observe products with stock enabled from Room database
     */
    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.getAllProductsForManagement().collect { allProducts ->
                // Filter products with stock enabled
                val stockEnabledProducts = allProducts.filter { it.isStockEnabled == true }
                
                // Sort: unsynced first, then by name
                val sortedProducts = stockEnabledProducts.sortedWith(
                    compareBy<ProductEntity> { if (it.isSynced == true) 1 else 0 }
                        .thenBy { it.name }
                )
                
                _uiState.update { current ->
                    current.copy(
                        products = sortedProducts,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Load products - sync from API first if network available
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
     * Show stock update dialog for a product
     */
    fun showStockUpdateDialog(product: ProductEntity) {
        // Check network connectivity
        if (!networkConnectivityChecker.isConnected()) {
            _uiState.update { it.copy(showNoInternetDialog = true) }
            return
        }
        
        _uiState.update {
            it.copy(
                showStockUpdateDialog = true,
                selectedProduct = product,
                stockUpdateQuantity = ""
            )
        }
    }
    
    /**
     * Dismiss stock update dialog
     */
    fun dismissStockUpdateDialog() {
        _uiState.update {
            it.copy(
                showStockUpdateDialog = false,
                selectedProduct = null,
                stockUpdateQuantity = ""
            )
        }
    }
    
    /**
     * Update stock quantity input
     */
    fun updateStockQuantityInput(quantity: String) {
        _uiState.update { it.copy(stockUpdateQuantity = quantity) }
    }
    
    /**
     * Update product stock
     */
    fun updateStock() {
        val product = _uiState.value.selectedProduct ?: return
        val quantityText = _uiState.value.stockUpdateQuantity.trim()
        
        if (quantityText.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "กรุณากรอกจำนวนที่ต้องการอัปเดต") }
            return
        }
        
        val quantityChange = quantityText.toIntOrNull()
        if (quantityChange == null) {
            _uiState.update { it.copy(errorMessage = "กรุณากรอกจำนวนเป็นตัวเลข") }
            return
        }
        
        if (quantityChange == 0) {
            _uiState.update { it.copy(errorMessage = "กรุณากรอกจำนวนที่ไม่ใช่ 0") }
            return
        }
        
        // Check network connectivity
        if (!networkConnectivityChecker.isConnected()) {
            _uiState.update { it.copy(showNoInternetDialog = true) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStock = true, errorMessage = null) }
            
            val oldQuantity = product.stockQuantity ?: 0
            val newQuantity = oldQuantity + quantityChange
            
            // Update stock in repository
            val result = productRepository.updateProductStock(product.id, quantityChange)
            
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isUpdatingStock = false,
                        showStockUpdateDialog = false,
                        selectedProduct = null,
                        stockUpdateQuantity = "",
                        updateSuccessMessage = "${product.name}: ${NumberFormat.getNumberInstance(Locale.US).format(oldQuantity)} → ${NumberFormat.getNumberInstance(Locale.US).format(newQuantity)} ชิ้น"
                    )
                }
                // Reload products to reflect changes
                loadProducts()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUpdatingStock = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการอัปเดตสต็อก"
                    )
                }
            }
        }
    }
    
    /**
     * Dismiss no internet dialog
     */
    fun dismissNoInternetDialog() {
        _uiState.update { it.copy(showNoInternetDialog = false) }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(updateSuccessMessage = null) }
    }
}

