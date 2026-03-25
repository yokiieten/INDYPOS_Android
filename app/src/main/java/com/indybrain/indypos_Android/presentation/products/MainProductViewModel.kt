package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.model.CartItem
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class MainProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {
    
    val cartItemCount = cartRepository.getCartItemCount()
    val cartItems = cartRepository.getCartItems()
    
    private val _uiState = MutableStateFlow(MainProductUiState())
    val uiState: StateFlow<MainProductUiState> = _uiState.asStateFlow()
    
    private val selectedCategoryFlow = MutableStateFlow<String?>(null)
    
    private var hideAdjusterJob: Job? = null

    init {
        // Set loading state immediately to show skeleton
        _uiState.update { it.copy(isLoading = true) }
    }

    /**
     * Load products when screen opens (called from onResume).
     * Fetches from Main Product List API only ([ProductRepository.getProductListFromApi]); no Room cache for listing.
     */
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            productRepository.getProductListFromApi().onSuccess { data ->
                val sortedCategories = data.categories.sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                val firstCategoryId = sortedCategories.firstOrNull()?.id
                _uiState.update { current ->
                    current.copy(
                        categories = sortedCategories,
                        allProducts = data.products,
                        products = data.products,
                        focusedCategoryId = current.focusedCategoryId ?: firstCategoryId,
                        selectedCategoryId = current.selectedCategoryId ?: firstCategoryId,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
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
    
    /**
     * Find product by barcode (productCode or skuCode) in the current API-loaded list only.
     */
    fun findProductByCode(code: String): ProductEntity? {
        val normalized = code.trim()
        if (normalized.isEmpty()) return null

        val product = _uiState.value.allProducts.firstOrNull { p ->
            p.productCode?.equals(normalized, ignoreCase = true) == true ||
                p.skuCode?.equals(normalized, ignoreCase = true) == true
        } ?: return null

        val categoryId = product.categoryId?.takeIf { it.isNotBlank() } ?: return null
        val category = _uiState.value.categories.find { it.id == categoryId }
        val isCategoryActive = category?.let { it.isActive && !it.isDeletedLocally } ?: false

        return if (isCategoryActive) product else null
    }
    
    /**
     * Add product to cart directly (for products without additional options)
     */
    fun addQuickToCart(product: ProductEntity) {
        viewModelScope.launch {
            // Ensure product exists in Room (for FK) when coming from API list
            val category = _uiState.value.categories.find { it.id == product.categoryId }
            productRepository.ensureProductExists(product, category)

            // Check stock if stock management is enabled
            if (product.isStockEnabled == true && product.stockQuantity != null) {
                // Get existing cart items for this product
                val existingCartItems = cartRepository.getCartItemsByProduct(product.id).first()
                val totalQuantityInCart = existingCartItems.sumOf { it.quantity }
                val newTotalQuantity = totalQuantityInCart + 1
                
                // Check stock availability
                val hasStock = cartRepository.checkStockAvailability(product.id, newTotalQuantity)
                
                if (!hasStock) {
                    // Show stock error message
                    val stockQuantity = product.stockQuantity
                    val availableStock = stockQuantity - totalQuantityInCart
                    val errorMessage = if (availableStock > 0) {
                        "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                    } else {
                        "สินค้าหมดสต็อก"
                    }
                    _uiState.update { it.copy(stockErrorMessage = errorMessage) }
                    return@launch
                }
            }
            
            addOneQuickCartUnit(product)
        }
    }
    
    fun clearStockErrorMessage() {
        _uiState.update { it.copy(stockErrorMessage = null) }
    }
    
    /**
     * Show quantity adjuster for a product and auto-hide after 2 seconds
     */
    fun showQuantityAdjuster(productId: String) {
        // Cancel previous timer
        hideAdjusterJob?.cancel()
        
        // Show adjuster
        _uiState.update { it.copy(expandedProductId = productId) }
        
        // Auto-hide after 2 seconds
        hideAdjusterJob = viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(expandedProductId = null) }
        }
    }
    
    /**
     * Hide quantity adjuster immediately
     */
    fun hideQuantityAdjuster() {
        hideAdjusterJob?.cancel()
        _uiState.update { it.copy(expandedProductId = null) }
    }
    
    /**
     * Increase product quantity in cart
     */
    fun increaseQuantity(product: ProductEntity) {
        viewModelScope.launch {
            // Ensure product exists in Room (for FK) when coming from API list
            val category = _uiState.value.categories.find { it.id == product.categoryId }
            productRepository.ensureProductExists(product, category)

            // Reset timer
            showQuantityAdjuster(product.id)
            
            // Check stock if enabled
            if (product.isStockEnabled == true && product.stockQuantity != null) {
                val existingCartItems = cartRepository.getCartItemsByProduct(product.id).first()
                val totalQuantityInCart = existingCartItems.sumOf { it.quantity }
                val newTotalQuantity = totalQuantityInCart + 1
                
                val hasStock = cartRepository.checkStockAvailability(product.id, newTotalQuantity)
                
                if (!hasStock) {
                    val stockQuantity = product.stockQuantity
                    val availableStock = stockQuantity - totalQuantityInCart
                    val errorMessage = if (availableStock > 0) {
                        "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                    } else {
                        "สินค้าหมดสต็อก"
                    }
                    _uiState.update { it.copy(stockErrorMessage = errorMessage) }
                    return@launch
                }
            }
            
            addOneQuickCartUnit(product)
        }
    }
    
    /**
     * Decrease product quantity in cart
     */
    fun decreaseQuantity(product: ProductEntity) {
        viewModelScope.launch {
            // Reset timer
            showQuantityAdjuster(product.id)
            
            val existingCartItems = cartRepository.getCartItemsByProduct(product.id).first()
            if (existingCartItems.isEmpty()) return@launch
            
            // Sort by creation date (oldest first) to remove FIFO
            val sortedItems = existingCartItems.sortedBy { it.createdAt }
            val oldestItem = sortedItems.first()
            
            if (oldestItem.quantity > 1) {
                // Decrease quantity by 1
                cartRepository.updateCartItemQuantity(oldestItem.id, oldestItem.quantity - 1)
            } else {
                // Remove item from cart
                cartRepository.deleteCartItem(oldestItem.id)
            }
        }
    }

    /**
     * One tap on quick add / + : merge into an existing "plain" line (no addons, no special note)
     * so receipt/kitchen printers show a single row with the total qty instead of many "1 x" lines.
     */
    private suspend fun addOneQuickCartUnit(product: ProductEntity) {
        val existing = cartRepository.getCartItemsByProduct(product.id).first()
        val mergeTarget = existing.firstOrNull { it.isMergeableQuickLine() }
        if (mergeTarget != null) {
            cartRepository.updateCartItemQuantity(mergeTarget.id, mergeTarget.quantity + 1)
        } else {
            cartRepository.addToCart(
                productId = product.id,
                productName = product.name,
                productImageUrl = product.imageUrl,
                productColorHex = product.selectedColorHex,
                unitPrice = product.price,
                quantity = 1,
                specialRequest = null,
                addons = emptyList()
            )
        }
    }
}

/** Plain cart line from main list quick +/- — safe to stack quantity without merging option/detail rows. */
private fun CartItem.isMergeableQuickLine(): Boolean {
    if (!specialRequest.isNullOrBlank()) return false
    if (selectedAddons.isEmpty()) return true
    return selectedAddons.values.all { it.isEmpty() }
}

