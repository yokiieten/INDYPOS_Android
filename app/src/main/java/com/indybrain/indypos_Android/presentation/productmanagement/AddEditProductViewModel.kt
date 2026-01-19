package com.indybrain.indypos_Android.presentation.productmanagement

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.ProductAddonGroupJunctionDao
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.presentation.productmanagement.ProductConstants.SELECTED_UNIT_COLOR
import com.indybrain.indypos_Android.presentation.productmanagement.ProductConstants.SELECTED_UNIT_IMAGE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Product screen
 */
@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val addonGroupRepository: AddonGroupRepository,
    private val productAddonGroupJunctionDao: ProductAddonGroupJunctionDao,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _uiState.asStateFlow()
    
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()
    
    private var productId: String? = null
    private var loadedProduct: ProductEntity? = null
    
    init {
        loadCategories()
        loadAddonGroups()
    }
    
    /**
     * Load categories
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                productRepository.getAllCategoriesFlow().collect { categories ->
                    if (categories != null) {
                        _categories.value = categories.sortedBy { it.sortOrder ?: 0 }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently or log it
                // Categories will remain empty if there's an error
            }
        }
    }
    
    /**
     * Load addon groups
     */
    private fun loadAddonGroups() {
        viewModelScope.launch {
            try {
                addonGroupRepository.getAllAddonGroupsFlow().collect { addonGroups ->
                    if (addonGroups != null) {
                        _uiState.update { it.copy(availableAddonGroups = addonGroups.sortedBy { it.sortOrder ?: 0 }) }
                    }
                }
            } catch (e: Exception) {
                // Handle error silently or log it
                // Addon groups will remain empty if there's an error
            }
        }
    }
    
    /**
     * Load product data for editing
     */
    fun loadProduct(productId: String) {
        this.productId = productId
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val product = productRepository.getProductById(productId)
                if (product != null) {
                    loadedProduct = product
                    // Normalize imageUrl: treat blank string as null
                    val normalizedImageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
                    // Decide initial mode: image vs color
                    val isColorMode = (product.selectedUnit == SELECTED_UNIT_COLOR) ||
                        (normalizedImageUrl == null && !product.selectedColorHex.isNullOrBlank())
                    val isImageMode = !isColorMode
                    // Load selected addon group IDs
                    val selectedAddonGroupIds = try {
                        productAddonGroupJunctionDao.getAddonGroupIdsByProductIdSync(productId)
                    } catch (e: Exception) {
                        emptyList() // Fallback to empty list if there's an error
                    }
                    
                    // Format prices for display when loading
                    val formattedSellingPrice = product.price?.let { 
                        formatPriceForDisplay(it.toString()) 
                    } ?: "0"
                    val formattedCostPrice = product.costPrice?.let { 
                        formatPriceForDisplay(it.toString()) 
                    } ?: ""
                    
                    _uiState.update { 
                        it.copy(
                            productName = product.name ?: "",
                            productCode = product.productCode ?: "",
                            sellingPrice = formattedSellingPrice,
                            costPrice = formattedCostPrice,
                            unit = product.unit ?: "",
                            imageUrl = normalizedImageUrl,
                            selectedColorHex = product.selectedColorHex,
                            isImageSelected = isImageMode,
                            categoryId = product.categoryId,
                            isSkuEnabled = product.isSkuEnabled ?: false,
                            skuCode = product.skuCode ?: "",
                            isStockEnabled = product.isStockEnabled ?: false,
                            stockQuantity = product.stockQuantity?.toString() ?: "",
                            addonGroupIds = selectedAddonGroupIds,
                            // ใช้ค่า product.hasAdditionalOptions เป็นหลักในการเปิด/ปิด Switch
                            hasAdditionalOptions = product.hasAdditionalOptions ?: false,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "ไม่พบสินค้าที่ต้องการแก้ไข"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "เกิดข้อผิดพลาดในการโหลดข้อมูล: ${e.message ?: "ไม่ทราบสาเหตุ"}"
                    )
                }
            }
        }
    }
    
    /**
     * Update product name
     */
    fun updateProductName(name: String) {
        _uiState.update { it.copy(productName = name, errorMessage = null) }
    }
    
    /**
     * Update product code
     */
    fun updateProductCode(code: String) {
        _uiState.update { it.copy(productCode = code, errorMessage = null) }
    }
    
    /**
     * Format price when user finishes input (on unfocus): Round to 2 decimal places
     * Example: 5000.533555 -> 5000.53, 5000.535555 -> 5000.54
     */
    private fun formatPriceOnUnfocus(input: String): String {
        if (input.isBlank()) return input
        
        try {
            // Use BigDecimal for precise rounding (round half up)
            val bigDecimal = BigDecimal(input.trim())
            val rounded = bigDecimal.setScale(2, RoundingMode.HALF_UP)
            // Format to 2 decimal places
            return String.format("%.2f", rounded.toDouble())
        } catch (e: Exception) {
            // If parsing fails, return as is
            return input
        }
    }
    
    /**
     * Format price for display: Hide .00, show 2 decimal places otherwise
     * Example: 5000.00 -> 5000, 5000.50 -> 5000.50
     */
    fun formatPriceForDisplay(price: String): String {
        if (price.isBlank()) return price
        
        val parsed = price.trim().toDoubleOrNull()
        return if (parsed != null) {
            // If decimal is .00, don't show decimals
            if (parsed % 1.0 == 0.0) {
                parsed.toInt().toString()
            } else {
                // Show 2 decimal places
                String.format("%.2f", parsed)
            }
        } else {
            price
        }
    }
    
    /**
     * Update selling price (allow free typing, no formatting during input)
     */
    fun updateSellingPrice(price: String) {
        _uiState.update { it.copy(sellingPrice = price, errorMessage = null) }
    }
    
    /**
     * Format selling price when user finishes input
     */
    fun formatSellingPriceOnUnfocus() {
        val currentPrice = _uiState.value.sellingPrice
        val formatted = formatPriceOnUnfocus(currentPrice)
        _uiState.update { it.copy(sellingPrice = formatted, errorMessage = null) }
    }
    
    /**
     * Update cost price (allow free typing, no formatting during input)
     */
    fun updateCostPrice(price: String) {
        _uiState.update { it.copy(costPrice = price, errorMessage = null) }
    }
    
    /**
     * Format cost price when user finishes input
     */
    fun formatCostPriceOnUnfocus() {
        val currentPrice = _uiState.value.costPrice
        val formatted = formatPriceOnUnfocus(currentPrice)
        _uiState.update { it.copy(costPrice = formatted, errorMessage = null) }
    }
    
    /**
     * Update unit
     */
    fun updateUnit(unit: String) {
        _uiState.update { it.copy(unit = unit, errorMessage = null) }
    }
    
    /**
     * Update category
     */
    fun updateCategory(categoryId: String?) {
        _uiState.update { it.copy(categoryId = categoryId, errorMessage = null) }
    }
    
    /**
     * Update SKU enabled
     */
    fun updateSkuEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isSkuEnabled = enabled, errorMessage = null) }
    }
    
    /**
     * Update SKU code
     */
    fun updateSkuCode(code: String) {
        _uiState.update { it.copy(skuCode = code, errorMessage = null) }
    }
    
    /**
     * Update stock enabled
     */
    fun updateStockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isStockEnabled = enabled, errorMessage = null) }
    }
    
    /**
     * Update stock quantity
     */
    fun updateStockQuantity(quantity: String) {
        _uiState.update { it.copy(stockQuantity = quantity, errorMessage = null) }
    }
    
    /**
     * Update additional options enabled
     */
    fun updateAdditionalOptionsEnabled(enabled: Boolean) {
        _uiState.update { 
            it.copy(
                hasAdditionalOptions = enabled, 
                errorMessage = null,
                // Clear selected addon groups if disabled
                addonGroupIds = if (!enabled) emptyList() else it.addonGroupIds
            ) 
        }
    }
    
    /**
     * Toggle addon group selection
     */
    fun toggleAddonGroupSelection(addonGroupId: String) {
        val currentIds = _uiState.value.addonGroupIds
        val newIds = if (currentIds.contains(addonGroupId)) {
            currentIds.filter { it != addonGroupId }
        } else {
            currentIds + addonGroupId
        }
        _uiState.update { it.copy(addonGroupIds = newIds, errorMessage = null) }
    }
    
    /**
     * Update image URL (from URI string)
     */
    fun updateImageUrl(imageUrl: String?) {
        _uiState.update { 
            it.copy(
                imageUrl = imageUrl,
                selectedColorHex = null,
                isImageSelected = true,
                errorMessage = null,
                showNoInternetDialog = false
            ) 
        }
    }
    
    /**
     * Update selected color hex
     */
    fun updateSelectedColorHex(colorHex: String?) {
        _uiState.update { 
            it.copy(
                selectedColorHex = colorHex,
                imageUrl = null,
                isImageSelected = false,
                errorMessage = null,
                showNoInternetDialog = false
            ) 
        }
    }
    
    /**
     * Dismiss no internet dialog
     */
    fun dismissNoInternetDialog() {
        _uiState.update { it.copy(showNoInternetDialog = false) }
    }
    
    /**
     * Dismiss image upload error dialog
     */
    fun dismissImageUploadErrorDialog() {
        _uiState.update { it.copy(showImageUploadErrorDialog = false) }
    }
    
    /**
     * Save product without image (when image upload fails)
     */
    fun saveProductWithoutImage() {
        val state = _uiState.value
        // Update state to remove image
        _uiState.update { 
            it.copy(
                imageUrl = null,
                isImageSelected = false,
                showImageUploadErrorDialog = false,
                loadingMessage = "กำลังบันทึกสินค้า..."
            )
        }
        // Retry save product
        saveProduct { }
    }
    
    /**
     * Update addon group IDs
     */
    fun updateAddonGroupIds(ids: List<String>) {
        _uiState.update { it.copy(addonGroupIds = ids, errorMessage = null) }
    }
    
    /**
     * Dismiss success dialog and reset success state
     */
    fun dismissSuccessDialog() {
        _uiState.update { it.copy(isSuccess = false) }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Show add category dialog
     */
    fun showAddCategoryDialog() {
        _uiState.update { 
            it.copy(
                showAddCategoryDialog = true,
                categoryName = "",
                categoryError = null,
                categorySuccess = null
            ) 
        }
    }
    
    /**
     * Dismiss add category dialog
     */
    fun dismissAddCategoryDialog() {
        _uiState.update { 
            it.copy(
                showAddCategoryDialog = false,
                categoryName = "",
                categoryError = null,
                categorySuccess = null
            ) 
        }
    }
    
    /**
     * Clear category success message
     */
    fun clearCategorySuccess() {
        _uiState.update { 
            it.copy(categorySuccess = null) 
        }
    }
    
    /**
     * Update category name in dialog
     */
    fun updateCategoryName(name: String) {
        _uiState.update { 
            it.copy(
                categoryName = name,
                categoryError = null
            ) 
        }
    }
    
    /**
     * Create category
     */
    fun createCategory() {
        val categoryName = _uiState.value.categoryName.trim()
        
        if (categoryName.isBlank()) {
            // Validation error - close dialog and show as popup
            _uiState.update { 
                it.copy(
                    showAddCategoryDialog = false,
                    categoryName = "",
                    categoryError = null,
                    errorMessage = "กรุณาใส่ชื่อหมวดหมู่"
                )
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isCreatingCategory = true,
                    categoryError = null,
                    categorySuccess = null
                ) 
            }
            
            val maxSortOrder = productRepository.getAllCategories()
                .mapNotNull { it.sortOrder }
                .maxOrNull() ?: 0
            
            val result = productRepository.createCategory(
                name = categoryName,
                sortOrder = maxSortOrder + 1,
                isActive = true
            )
            
            result.onSuccess { category ->
                // Update selected category to the newly created one
                _uiState.update { 
                    it.copy(
                        categoryId = category.id,
                        isCreatingCategory = false,
                        categorySuccess = "เพิ่มหมวดหมู่ '$categoryName' เรียบร้อยแล้ว",
                        showAddCategoryDialog = false,
                        categoryName = "",
                        categoryError = null
                    ) 
                }
                // Reload categories to include the new one
                loadCategories()
            }.onFailure { error ->
                // API error - close dialog and show as popup
                _uiState.update { 
                    it.copy(
                        isCreatingCategory = false,
                        showAddCategoryDialog = false,
                        categoryName = "",
                        categoryError = null,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการเพิ่มหมวดหมู่"
                    ) 
                }
            }
        }
    }
    
    /**
     * Save product (add new or update existing)
     * For create mode: calls API if network available, otherwise saves to Room with isFromServer=false, isSynced=false
     */
    fun saveProduct(onSuccess: () -> Unit) {
        val state = _uiState.value
        
        // Validation
        if (state.productName.trim().isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณากรอกชื่อสินค้า")
            }
            return
        }
        
        if (state.productCode.trim().isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณากรอกรหัสสินค้า")
            }
            return
        }
        
        val sellingPrice = try {
            state.sellingPrice.trim().toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
        
        if (sellingPrice <= 0) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณากรอกราคาขาย")
            }
            return
        }
        
        // Validate that cost price (if provided) is not greater than selling price
        val tempCostPrice = try {
            state.costPrice.trim().toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
        if (tempCostPrice != null && tempCostPrice > sellingPrice) {
            _uiState.update {
                it.copy(errorMessage = "ราคาต้นทุนต้องไม่มากกว่าราคาขาย")
            }
            return
        }
        
        if (state.unit.trim().isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณากรอกหน่วยนับ")
            }
            return
        }
        
        if (state.categoryId == null) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณาเลือกหมวดหมู่")
            }
            return
        }
        
        // Check if image/color is selected
        if (state.isImageSelected && state.imageUrl == null && state.selectedColorHex == null) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณาเลือกรูปภาพหรือสี")
            }
            return
        }
        
        if (!state.isImageSelected && state.selectedColorHex == null) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณาเลือกสี")
            }
            return
        }
        
        // Validate AddOn Groups if hasAdditionalOptions is enabled
        if (state.hasAdditionalOptions && state.addonGroupIds.isEmpty()) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณาเลือก AddOn Groups อย่างน้อย 1 รายการ")
            }
            return
        }
        
        // Validate SKU Code if SKU is enabled
        if (state.isSkuEnabled && state.skuCode.trim().isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณากรอกรหัส SKU")
            }
            return
        }
        
        // Validate Stock Quantity if Stock is enabled
        if (state.isStockEnabled) {
            val stockQuantity = try {
                state.stockQuantity.trim().toIntOrNull()
            } catch (e: Exception) {
                null
            }
            if (stockQuantity == null || stockQuantity < 0) {
                _uiState.update { 
                    it.copy(errorMessage = "กรุณากรอกจำนวนสินค้า")
                }
                return
            }
        }
        
        // Check network connectivity
        val hasNetwork = networkConnectivityChecker.isConnected()
        
        // If no network and image is selected, show dialog
        if (!hasNetwork && state.isImageSelected && state.imageUrl != null) {
            // Check if imageUrl is a local URI (content:// or file://)
            val isLocalUri = state.imageUrl.startsWith("content://") || state.imageUrl.startsWith("file://")
            if (isLocalUri) {
                _uiState.update { 
                    it.copy(showNoInternetDialog = true)
                }
                return
            }
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    loadingMessage = null,
                    showImageUploadErrorDialog = false
                )
            }
            
            val userId = productRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        errorMessage = "ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่"
                    )
                }
                return@launch
            }
            
            // For now, only handle create mode (edit mode will be implemented later)
            if (productId == null) {
                var finalImageUrl = state.imageUrl
                
                // If has network and image is selected, upload image first
                if (hasNetwork && state.isImageSelected && state.imageUrl != null) {
                    // Check if imageUrl is a local URI (needs upload)
                    val isLocalUri = state.imageUrl.startsWith("content://") || state.imageUrl.startsWith("file://")
                    if (isLocalUri) {
                        // Show uploading image message
                        _uiState.update { 
                            it.copy(loadingMessage = "กำลังอัปโหลดรูปภาพ...")
                        }
                        
                        try {
                            val uri = android.net.Uri.parse(state.imageUrl)
                            val uploadResult = productRepository.uploadProductImage(uri)
                            // Handle the result - uploadProductImage is suspend so it's already awaited
                            val uploadedUrl = uploadResult.getOrNull()
                            if (uploadedUrl == null) {
                                val error = uploadResult.exceptionOrNull()
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        loadingMessage = null,
                                        showImageUploadErrorDialog = true,
                                        errorMessage = error?.message ?: "เกิดข้อผิดพลาดในการอัปโหลดรูปภาพ"
                                    )
                                }
                                return@launch
                            }
                            finalImageUrl = uploadedUrl
                            // Change loading message to saving product
                            _uiState.update { 
                                it.copy(loadingMessage = "กำลังบันทึกสินค้า...")
                            }
                        } catch (e: Exception) {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    loadingMessage = null,
                                    showImageUploadErrorDialog = true,
                                    errorMessage = "ไม่สามารถอัปโหลดรูปภาพได้: ${e.message}"
                                )
                            }
                            return@launch
                        }
                    } else {
                        // Image URL is already uploaded, show saving message
                        _uiState.update { 
                            it.copy(loadingMessage = "กำลังบันทึกสินค้า...")
                        }
                    }
                } else {
                    // No image or offline, show saving message
                    if (hasNetwork) {
                        _uiState.update { 
                            it.copy(loadingMessage = "กำลังบันทึกสินค้า...")
                        }
                    }
                }
                
                // Create new product
                val costPrice = try {
                    state.costPrice.trim().toDoubleOrNull()
                } catch (e: Exception) {
                    null
                }
                
                val stockQuantity = if (state.isStockEnabled) {
                    try {
                        state.stockQuantity.trim().toIntOrNull()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                val addonGroupIds = if (state.hasAdditionalOptions && state.addonGroupIds.isNotEmpty()) {
                    state.addonGroupIds
                } else {
                    null
                }
                
                val result = productRepository.createProduct(
                    name = state.productName.trim(),
                    productCode = state.productCode.trim(),
                    price = sellingPrice,
                    costPrice = costPrice,
                    unit = state.unit.trim(),
                    imageUrl = if (state.isImageSelected) finalImageUrl else null,
                    selectedColorHex = if (!state.isImageSelected) state.selectedColorHex else null,
                    categoryId = state.categoryId,
                    skuCode = if (state.isSkuEnabled) state.skuCode.trim().takeIf { it.isNotBlank() } else null,
                    stockQuantity = stockQuantity,
                    isSkuEnabled = if (state.isSkuEnabled) true else null,
                    isStockEnabled = if (state.isStockEnabled) true else null,
                    hasAdditionalOptions = if (state.hasAdditionalOptions) true else null,
                    addonGroupIds = addonGroupIds
                )
                
                result.onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isSuccess = true
                        )
                    }
                    // Don't call onSuccess() here - let the dialog handle navigation
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการบันทึก"
                        )
                    }
                }
            } else {
                // Edit mode - to be implemented later
                var finalImageUrl = state.imageUrl
                
                // If has network and image is selected, upload image first
                if (hasNetwork && state.isImageSelected && state.imageUrl != null) {
                    val isLocalUri = state.imageUrl.startsWith("content://") || state.imageUrl.startsWith("file://")
                    if (isLocalUri) {
                        _uiState.update { 
                            it.copy(loadingMessage = "กำลังอัปโหลดรูปภาพ...")
                        }
                        
                        try {
                            val uri = android.net.Uri.parse(state.imageUrl)
                            val uploadResult = productRepository.uploadProductImage(uri)
                            // Handle the result - uploadProductImage is suspend so it's already awaited
                            val uploadedUrl = uploadResult.getOrNull()
                            if (uploadedUrl == null) {
                                val error = uploadResult.exceptionOrNull()
                                _uiState.update { 
                                    it.copy(
                                        isLoading = false,
                                        loadingMessage = null,
                                        showImageUploadErrorDialog = true,
                                        errorMessage = error?.message ?: "เกิดข้อผิดพลาดในการอัปโหลดรูปภาพ"
                                    )
                                }
                                return@launch
                            }
                            finalImageUrl = uploadedUrl
                            _uiState.update { 
                                it.copy(loadingMessage = "กำลังบันทึกสินค้า...")
                            }
                        } catch (e: Exception) {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    loadingMessage = null,
                                    showImageUploadErrorDialog = true,
                                    errorMessage = "ไม่สามารถอัปโหลดรูปภาพได้: ${e.message}"
                                )
                            }
                            return@launch
                        }
                    } else {
                        _uiState.update { 
                            it.copy(loadingMessage = "กำลังบันทึกสินค้า...")
                        }
                    }
                } else {
                    if (hasNetwork) {
                        _uiState.update { 
                            it.copy(loadingMessage = "กำลังบันทึกสินค้า...")
                        }
                    }
                }
                
                val costPrice = try {
                    state.costPrice.trim().toDoubleOrNull()
                } catch (e: Exception) {
                    null
                }
                
                val stockQuantity = if (state.isStockEnabled) {
                    try {
                        state.stockQuantity.trim().toIntOrNull()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                val addonGroupIds = if (state.hasAdditionalOptions && state.addonGroupIds.isNotEmpty()) {
                    state.addonGroupIds
                } else {
                    null
                }
                
                val result = productRepository.updateProduct(
                    productId = productId ?: return@launch,
                    name = state.productName.trim(),
                    productCode = state.productCode.trim(),
                    price = sellingPrice,
                    costPrice = costPrice,
                    unit = state.unit.trim(),
                    imageUrl = if (state.isImageSelected) finalImageUrl else null,
                    selectedColorHex = if (!state.isImageSelected) state.selectedColorHex else null,
                    categoryId = state.categoryId,
                    skuCode = if (state.isSkuEnabled) state.skuCode.trim().takeIf { it.isNotBlank() } else null,
                    stockQuantity = stockQuantity,
                    isSkuEnabled = state.isSkuEnabled,
                    isStockEnabled = state.isStockEnabled,
                    hasAdditionalOptions = state.hasAdditionalOptions,
                    addonGroupIds = addonGroupIds
                )
                
                result.onSuccess { updated ->
                    loadedProduct = updated
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            loadingMessage = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการบันทึก"
                        )
                    }
                }
            }
        }
    }
}

