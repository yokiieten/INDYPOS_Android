package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    private var hideAdjusterJob: Job? = null
    
    // Track if API call is in progress
    private var isApiCallInProgress = false
    
    init {
        // Set loading state immediately to show skeleton
        _uiState.update { it.copy(isLoading = true) }
        // Start observing local Room data immediately
        observeCategories()
        observeProducts()
        // Load latest products from API once when ViewModel is created
        // NOTE: Cart items are automatically observed via cartItems Flow
        // Cart is persisted in Room database and will NOT be cleared when opening this screen
        // Cart is only cleared when user logs out (see AuthRepositoryImpl.logout())
        loadProducts()
    }
    
    /**
     * Load products when screen opens
     */
    fun loadProducts() {
        viewModelScope.launch {
            // Set loading state immediately
            isApiCallInProgress = true
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Clear all products and categories from Room before fetching fresh data
            productRepository.clearAllProductsAndCategories()
            
            // Try to fetch from API if connected, otherwise use local data
            if (networkConnectivityChecker.isConnected()) {
                val result = productRepository.fetchAndSaveProducts()
                result.onSuccess {
                    // After successfully fetching and saving products, restore productId for cart items
                    // that may have been set to null due to previous deleteAll() calls
                    val products = productRepository.getAllActiveProducts().first()
                    cartRepository.restoreProductIdsForCartItems(products)
                    // API call completed, mark as not in progress
                    isApiCallInProgress = false
                    // Wait a bit for Room to process and emit new data
                    delay(100)
                    // Force update products and stop loading (in case Room doesn't emit immediately)
                    val productsWithCategory = products.filter { 
                        it.categoryId != null && it.categoryId.isNotBlank() 
                    }
                    _uiState.update { current ->
                        current.copy(
                            allProducts = productsWithCategory,
                            products = productsWithCategory,
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    // API call failed, stop loading and show error
                    isApiCallInProgress = false
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
            } else {
                // No internet, use local data
                // Mark as not in progress so observeProducts() can stop loading when data arrives
                isApiCallInProgress = false
                // Wait a bit for Room to emit initial data
                delay(100)
                // If no data after delay, stop loading anyway
                val products = productRepository.getAllActiveProducts().first()
                val productsWithCategory = products.filter { 
                    it.categoryId != null && it.categoryId.isNotBlank() 
                }
                _uiState.update { current ->
                    current.copy(
                        allProducts = productsWithCategory,
                        products = productsWithCategory,
                        isLoading = false
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
            productRepository.getAllActiveCategories().collect { categories ->
                // Sort categories by sortOrder (handle null values)
                val sortedCategories = categories.sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                val firstCategoryId = sortedCategories.firstOrNull()?.id
                
                _uiState.update { current ->
                    // Set initial focused category to first category if not set
                    val newFocusedCategoryId = current.focusedCategoryId ?: firstCategoryId
                    current.copy(
                        categories = sortedCategories,
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
                    // If API call is in progress, don't update products yet (to prevent showing old data)
                    // Only update products after API call completes
                    if (isApiCallInProgress) {
                        // Keep loading state and don't update products (to prevent showing old data)
                        current.copy(
                            isLoading = true
                        )
                    } else {
                        // API call completed, update products and stop loading
                        // This will be called when Room emits data after API completes
                        current.copy(
                            allProducts = productsWithCategory,
                            products = productsWithCategory, // Always show all products with category
                            isLoading = false
                        )
                    }
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
     * Find product by barcode (productCode or skuCode)
     */
    suspend fun findProductByCode(code: String): ProductEntity? {
        // 1) Find active product by code
        val product = productRepository.getProductByCode(code) ?: return null

        // 2) Product must have a category
        val categoryId = product.categoryId?.takeIf { it.isNotBlank() } ?: return null

        // 3) Category must still be active (and not deleted locally)
        val category = productRepository.getCategoryById(categoryId)
        val isCategoryActive = category?.let { it.isActive && !it.isDeletedLocally } ?: false

        // If category is not active, treat as "product not found" for scanning/search,
        // to match iOS behaviour and the SearchProductViewModel filters.
        return if (isCategoryActive) product else null
    }
    
    /**
     * Add product to cart directly (for products without additional options)
     */
    fun addQuickToCart(product: ProductEntity) {
        viewModelScope.launch {
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
            
            // Add to cart
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
            
            // Add to cart
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
}

