package com.indybrain.indypos_Android.presentation.stockmanagement

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
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    @ApplicationContext private val context: Context,
    private val languageLocalDataSource: LanguageLocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StockManagementUiState())
    val uiState: StateFlow<StockManagementUiState> = _uiState.asStateFlow()
    
    init {
        loadProducts()
    }
    
    /**
     * Load stock products from API (paginated until exhausted). Offline shows no-internet dialog state via [loadProducts].
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (!networkConnectivityChecker.isConnected()) {
                _uiState.update {
                    it.copy(isLoading = false, showNoInternetDialog = true)
                }
                return@launch
            }
            productRepository.syncAllProductData().onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                    )
                }
                return@launch
            }
            val accumulated = mutableListOf<ProductEntity>()
            var page = 1
            val pageSize = 100
            while (true) {
                val result = productRepository.getProductsPaginated(
                    page = page,
                    limit = pageSize,
                    search = null,
                    categoryId = null
                )
                val data = result.getOrElse { err ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                    return@launch
                }
                accumulated.addAll(data.products)
                if (!data.hasNext) break
                page++
            }
            val stockEnabledProducts = accumulated
                .filter { it.isStockEnabled == true }
                .sortedBy { it.name }
            _uiState.update { current ->
                current.copy(
                    products = stockEnabledProducts,
                    isLoading = false
                )
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
                // Get localized context with current locale
                val localeCode = languageLocalDataSource.getLanguageLocale()
                val localizedContext = LocaleHelper.setLocale(context, localeCode)
                val unitText = localizedContext.getString(R.string.stock_unit_piece)
                _uiState.update {
                    it.copy(
                        isUpdatingStock = false,
                        showStockUpdateDialog = false,
                        selectedProduct = null,
                        stockUpdateQuantity = "",
                        updateSuccessMessage = "${product.name}: ${NumberFormat.getNumberInstance(Locale.US).format(oldQuantity)} → ${NumberFormat.getNumberInstance(Locale.US).format(newQuantity)} $unitText"
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

