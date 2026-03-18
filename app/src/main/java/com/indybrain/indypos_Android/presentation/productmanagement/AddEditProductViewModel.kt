package com.indybrain.indypos_Android.presentation.productmanagement

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.dao.ProductAddonGroupJunctionDao
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.presentation.productmanagement.ProductConstants.SELECTED_UNIT_COLOR
import com.indybrain.indypos_Android.presentation.productmanagement.ProductConstants.SELECTED_UNIT_IMAGE
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val languageLocalDataSource: LanguageLocalDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Returns string in the user's selected language (respects language change in Settings) */
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
    
    private val _uiState = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _uiState.asStateFlow()
    
    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()
    
    private var productId: String? = null
    private var loadedProduct: ProductEntity? = null
    
    init {
        loadCategories()
        loadAddonGroups()
        // Fetch categories and addon groups from API when online
        viewModelScope.launch {
            if (networkConnectivityChecker.isConnected()) {
                productRepository.fetchAndSyncCategories()
                addonGroupRepository.fetchAndSyncAddonGroups()
            }
            // Flow in loadCategories() and loadAddonGroups() will auto-emit when Room is updated
        }
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
     * Tries Room first, then API (for products from paginated list that may not be in Room)
     */
    fun loadProduct(productId: String) {
        this.productId = productId
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                var product = productRepository.getProductById(productId)
                var addonGroupIdsFromApi: List<String>? = null
                
                // If not in Room, try API (products from paginated list may not be synced to Room)
                if (product == null && networkConnectivityChecker.isConnected()) {
                    val apiResult = productRepository.getProductDetailFromApi(productId)
                    apiResult.onSuccess { detailData ->
                        product = detailData.product
                        addonGroupIdsFromApi = detailData.addonGroups.map { it.id }
                        // Save to Room for future use
                        productRepository.ensureProductExists(detailData.product, detailData.category)
                    }
                }
                
                if (product != null) {
                    val p = product!!
                    loadedProduct = p
                    // Normalize imageUrl: treat blank string as null
                    val normalizedImageUrl = p.imageUrl?.takeIf { it.isNotBlank() }
                    // Decide initial mode: image vs color
                    val isColorMode = (p.selectedUnit == SELECTED_UNIT_COLOR) ||
                        (normalizedImageUrl == null && !p.selectedColorHex.isNullOrBlank())
                    val isImageMode = !isColorMode
                    // Load selected addon group IDs (from API response if loaded from API, else from Room)
                    val selectedAddonGroupIds = addonGroupIdsFromApi ?: try {
                        productAddonGroupJunctionDao.getAddonGroupIdsByProductIdSync(productId)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    
                    // Format prices for display when loading
                    val formattedSellingPrice = p.price?.let { formatPriceForDisplay(it.toString()) } ?: "0"
                    val formattedCostPrice = p.costPrice?.let { formatPriceForDisplay(it.toString()) } ?: ""
                    
                    _uiState.update { 
                        it.copy(
                            productName = p.name ?: "",
                            productCode = p.productCode ?: "",
                            sellingPrice = formattedSellingPrice,
                            costPrice = formattedCostPrice,
                            unit = p.unit ?: "",
                            imageUrl = normalizedImageUrl,
                            selectedColorHex = p.selectedColorHex,
                            isImageSelected = isImageMode,
                            categoryId = p.categoryId,
                            isSkuEnabled = p.isSkuEnabled ?: false,
                            skuCode = p.skuCode ?: "",
                            isStockEnabled = p.isStockEnabled ?: false,
                            stockQuantity = p.stockQuantity?.toString() ?: "",
                            addonGroupIds = selectedAddonGroupIds,
                            hasAdditionalOptions = p.hasAdditionalOptions ?: false,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = getLocalizedString(R.string.product_form_error_load_not_found)
                        )
                    }
                }
            } catch (e: Exception) {
                val reason = e.message ?: getLocalizedString(R.string.product_form_error_unknown_reason)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = getLocalizedString(R.string.product_form_error_load_with_reason, reason)
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
     * Filter price input to only allow numbers and decimal point
     * No limit on decimal places while typing - user can type unlimited digits
     * Rounding to 2 decimal places happens when user finishes input (on unfocus or Done)
     * Example: User can type 5000.533555, rounding (5000.533555 -> 5000.53, 5000.535555 -> 5000.54) happens on unfocus
     */
    private fun filterPriceInput(input: String): String {
        if (input.isBlank()) return input
        
        // Allow only digits and one decimal point
        val filtered = input.filter { it.isDigit() || it == '.' }
        
        // If empty after filtering, return empty
        if (filtered.isEmpty()) return ""
        
        // Check for multiple decimal points - keep only the first one
        val parts = filtered.split('.')
        val result = if (parts.size > 2) {
            // Multiple decimal points - keep first part + first decimal point + second part
            parts[0] + "." + parts[1]
        } else {
            filtered
        }
        
        // No limit on decimal places while typing - allow unlimited digits
        // Rounding will happen in formatPriceOnUnfocus() when user finishes input
        return result
    }
    
    /**
     * Update selling price with input filtering and automatic rounding
     */
    fun updateSellingPrice(price: String) {
        val filtered = filterPriceInput(price)
        _uiState.update { it.copy(sellingPrice = filtered, errorMessage = null) }
    }
    
    /**
     * Format selling price when user finishes input
     * Rounds to 2 decimal places and applies display formatting (hides .00 in edit mode)
     */
    fun formatSellingPriceOnUnfocus() {
        val currentPrice = _uiState.value.sellingPrice
        if (currentPrice.isBlank()) return
        
        // First round to 2 decimal places
        val rounded = formatPriceOnUnfocus(currentPrice)
        // Then apply display formatting (hide .00 if applicable)
        val displayFormatted = formatPriceForDisplay(rounded)
        _uiState.update { it.copy(sellingPrice = displayFormatted, errorMessage = null) }
    }
    
    /**
     * Update cost price with input filtering and automatic rounding
     */
    fun updateCostPrice(price: String) {
        val filtered = filterPriceInput(price)
        _uiState.update { it.copy(costPrice = filtered, errorMessage = null) }
    }
    
    /**
     * Format cost price when user finishes input
     * Rounds to 2 decimal places and applies display formatting (hides .00 in edit mode)
     */
    fun formatCostPriceOnUnfocus() {
        val currentPrice = _uiState.value.costPrice
        if (currentPrice.isBlank()) return
        
        // First round to 2 decimal places
        val rounded = formatPriceOnUnfocus(currentPrice)
        // Then apply display formatting (hide .00 if applicable)
        val displayFormatted = formatPriceForDisplay(rounded)
        _uiState.update { it.copy(costPrice = displayFormatted, errorMessage = null) }
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
        // Update state to remove image
        _uiState.update { 
            it.copy(
                imageUrl = null,
                isImageSelected = false,
                showImageUploadErrorDialog = false,
                loadingMessage = getLocalizedString(R.string.product_form_loading_saving_product)
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
     * Clear category error message
     */
    fun clearCategoryError() {
        _uiState.update { 
            it.copy(categoryError = null) 
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
            // Validation error - keep dialog open and show error message
            _uiState.update { 
                it.copy(
                    categoryError = getLocalizedString(R.string.product_form_add_category_message)
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
                        categorySuccess = getLocalizedString(R.string.product_form_add_category_success, categoryName),
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
                        errorMessage = error.message ?: getLocalizedString(R.string.product_form_error_add_category)
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
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_name_required))
            }
            return
        }
        
        if (state.productCode.trim().isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_code_required))
            }
            return
        }
        
        val sellingPrice = try {
            state.sellingPrice.trim().toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
        
        if (sellingPrice < 0) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_selling_price_required))
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
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_cost_price_greater_than_selling))
            }
            return
        }
        
        if (state.unit.trim().isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_unit_required))
            }
            return
        }
        
        if (state.categoryId == null) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_category_required))
            }
            return
        }
        
        // Check if image/color is selected
        if (state.isImageSelected && state.imageUrl == null && state.selectedColorHex == null) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_image_or_color_required))
            }
            return
        }
        
        if (!state.isImageSelected && state.selectedColorHex == null) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_color_required))
            }
            return
        }
        
        // Validate AddOn Groups if hasAdditionalOptions is enabled
        if (state.hasAdditionalOptions && state.addonGroupIds.isEmpty()) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_addon_groups_required))
            }
            return
        }
        
        // Validate SKU Code if SKU is enabled
        if (state.isSkuEnabled && state.skuCode.trim().isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_sku_required))
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
                    it.copy(errorMessage = getLocalizedString(R.string.product_form_validation_stock_quantity_required))
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
                        errorMessage = getLocalizedString(R.string.product_form_error_user_not_found)
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
                            it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_uploading_image))
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
                                        errorMessage = error?.message ?: getLocalizedString(R.string.product_form_image_upload_error_message)
                                    )
                                }
                                return@launch
                            }
                            finalImageUrl = uploadedUrl
                            // Change loading message to saving product
                            _uiState.update { 
                                it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_saving_product))
                            }
                        } catch (e: Exception) {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    loadingMessage = null,
                                    showImageUploadErrorDialog = true,
                                    errorMessage = getLocalizedString(
                                        R.string.product_form_image_upload_error_message
                                    )
                                )
                            }
                            return@launch
                        }
                    } else {
                        // Image URL is already uploaded, show saving message
                        _uiState.update { 
                            it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_saving_product))
                        }
                    }
                } else {
                    // No image or offline, show saving message
                    if (hasNetwork) {
                        _uiState.update { 
                            it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_saving_product))
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
                            errorMessage = error.message ?: getLocalizedString(R.string.product_form_error_save_generic)
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
                            it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_uploading_image))
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
                                        errorMessage = error?.message ?: getLocalizedString(R.string.product_form_image_upload_error_message)
                                    )
                                }
                                return@launch
                            }
                            finalImageUrl = uploadedUrl
                            _uiState.update { 
                                it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_saving_product))
                            }
                        } catch (e: Exception) {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    loadingMessage = null,
                                    showImageUploadErrorDialog = true,
                                    errorMessage = getLocalizedString(
                                        R.string.product_form_image_upload_error_message
                                    )
                                )
                            }
                            return@launch
                        }
                    } else {
                        _uiState.update { 
                            it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_saving_product))
                        }
                    }
                } else {
                    if (hasNetwork) {
                        _uiState.update { 
                            it.copy(loadingMessage = getLocalizedString(R.string.product_form_loading_saving_product))
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
                            errorMessage = error.message ?: getLocalizedString(R.string.product_form_error_save_generic)
                        )
                    }
                }
            }
        }
    }
}

