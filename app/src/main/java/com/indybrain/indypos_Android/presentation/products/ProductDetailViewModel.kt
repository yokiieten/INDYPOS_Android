package com.indybrain.indypos_Android.presentation.products

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.model.CartItem
import com.indybrain.indypos_Android.domain.model.addonsSelectionConfigurationKey
import com.indybrain.indypos_Android.domain.model.configurationKey
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.domain.usecase.CartInsufficientStockErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val productRepository: ProductRepository,
    private val languageLocalDataSource: LanguageLocalDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** String in the language selected in Settings (not only system default). */
    private fun getLocalizedString(resId: Int, vararg formatArgs: Any): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return if (formatArgs.isEmpty()) localizedContext.getString(resId)
        else localizedContext.getString(resId, *formatArgs)
    }

    companion object {
        const val ERROR_MAX_SELECTION_ZERO = "MAX_SELECTION_ZERO"
        const val ERROR_INSUFFICIENT_STOCK = CartInsufficientStockErrors.TOKEN_PLAIN
        private const val ERROR_MAX_SELECTION_REACHED_PREFIX = "MAX_SELECTION_REACHED"
        private const val ERROR_PARAM_SEPARATOR = '\u001E'

        fun buildMaxSelectionReachedError(maxSelection: Int, groupName: String): String =
            "$ERROR_MAX_SELECTION_REACHED_PREFIX$ERROR_PARAM_SEPARATOR$maxSelection$ERROR_PARAM_SEPARATOR$groupName"

        fun parseMaxSelectionReachedError(message: String): Pair<Int, String>? {
            val prefix = "$ERROR_MAX_SELECTION_REACHED_PREFIX$ERROR_PARAM_SEPARATOR"
            if (!message.startsWith(prefix)) return null
            val parts = message.split(ERROR_PARAM_SEPARATOR, limit = 3)
            if (parts.size != 3) return null
            val max = parts[1].toIntOrNull() ?: return null
            return max to parts[2]
        }
    }
    
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    
    fun loadProduct(productId: String, cartItemId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val apiResult = productRepository.getProductDetailFromApi(productId)
                if (apiResult.isFailure) {
                    val err = apiResult.exceptionOrNull()
                    val raw = err?.message?.takeIf { it.isNotBlank() }.orEmpty()
                    val message =
                        if (raw.contains("No network connection", ignoreCase = true)) {
                            context.getString(R.string.logout_no_internet_title)
                        } else {
                            raw.takeIf { it.isNotBlank() } ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = null,
                            category = null,
                            addonGroups = emptyList(),
                            addonsByGroup = emptyMap(),
                            selectedAddons = emptyMap(),
                            quantity = 1,
                            specialRequest = "",
                            editingCartItemId = null,
                            errorMessage = message
                        )
                    }
                    return@launch
                }

                val data = apiResult.getOrNull()!!
                val product = data.product
                // POS แสดงเฉพาะกลุ่ม/ตัวเลือกที่ active — ข้อมูลดิบจาก API ยังเต็มสำหรับหน้าอื่น (เช่นแก้สินค้า, sync ตะกร้า)
                val addonGroups = data.addonGroups.filter { it.isActive }
                val addonsByGroup = addonGroups.associate { group ->
                    group.id to (data.addonsByGroup[group.id].orEmpty().filter { it.isActive })
                }
                val category = data.category

                // Check if product is already in cart and load existing data
                val existingCartItems = cartRepository.getCartItemsByProduct(productId).first()
                val isExplicitNew = cartItemId == "new"
                val existingCartItem = when {
                    isExplicitNew -> null // "เพิ่มอีก" ต้องไม่ติดค่าจากตะกร้าเลย
                    cartItemId != null -> {
                        // Try to load the specific cart item we are editing
                        cartRepository.getCartItemById(cartItemId)
                    }
                    else -> existingCartItems.firstOrNull()
                }
                
                // Load existing cart data if available
                // กรณีแก้ไขจากตะกร้า ให้จำนวนเริ่มต้นตรงกับ "จำนวนรวม" ของ group เดียวกัน
                // ไม่ใช่แค่จำนวนของ CartItem ตัวเดียว (เช่น กด + จาก ProductEditScreen / OrderProductScreen)
                val existingQuantity = when {
                    // โหมดเพิ่มใหม่แบบบังคับ ("เพิ่มอีก") → เริ่มที่ 1 เสมอ
                    isExplicitNew || existingCartItem == null -> 1
                    
                    // โหมดแก้ไข: รวมจำนวนของ cart items ทุกตัวที่ config เหมือนกัน (specialRequest + addons)
                    else -> {
                        val targetKey = createGroupKeyForCartItem(existingCartItem)
                        val groupedTotal = existingCartItems
                            .filter { createGroupKeyForCartItem(it) == targetKey }
                            .sumOf { it.quantity }
                        
                        // กันกรณีผิดปกติ (เผื่อ grouping พลาด) ให้ fallback เป็น quantity เดิม
                        if (groupedTotal > 0) groupedTotal else existingCartItem.quantity
                    }
                }
                val existingSpecialRequest = existingCartItem?.specialRequest ?: ""
                
                // Load existing addons from cart
                val existingSelectedAddons = if (existingCartItem != null) {
                    // Use domain model addons to restore selected state
                    existingCartItem.selectedAddons.mapValues { (_, addons) ->
                        addons.map { it.id }.toSet()
                    }
                } else {
                    emptyMap()
                }

                val hasAddons = product.hasAdditionalOptions == true
                val allowedGroupIds = addonGroups.map { it.id }.toSet()
                val selectedAddonsForUi = if (!hasAddons) {
                    emptyMap()
                } else {
                    existingSelectedAddons
                        .filterKeys { it in allowedGroupIds }
                        .mapValues { (groupId, addonIds) ->
                            val allowedAddonIds =
                                addonsByGroup[groupId].orEmpty().map { it.id }.toSet()
                            addonIds.filter { it in allowedAddonIds }.toSet()
                        }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        product = product,
                        category = category,
                        addonGroups = if (hasAddons) addonGroups else emptyList(),
                        addonsByGroup = if (hasAddons) addonsByGroup else emptyMap(),
                        selectedAddons = selectedAddonsForUi,
                        quantity = existingQuantity,
                        specialRequest = existingSpecialRequest,
                        editingCartItemId = if (!isExplicitNew && existingCartItem != null) existingCartItem.id else null,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        product = null,
                        category = null,
                        addonGroups = emptyList(),
                        addonsByGroup = emptyMap(),
                        selectedAddons = emptyMap(),
                        quantity = 1,
                        specialRequest = "",
                        editingCartItemId = null,
                        errorMessage = e.message?.takeIf { it.isNotBlank() }
                            ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                    )
                }
            }
        }
    }

    fun toggleAddon(addonGroupId: String, addonId: String) {
        _uiState.update { currentState ->
            val addonGroup = currentState.addonGroups.find { it.id == addonGroupId }
                ?: return@update currentState
            
            val currentSelected = currentState.selectedAddons[addonGroupId] ?: emptySet()
            
            // Check if maxSelection is 0 - if so, prevent selection and show error
            val maxSelection = addonGroup.maxSelection
            if (maxSelection != null && maxSelection == 0) {
                // Max selection is 0, cannot select any addon
                // Error message will be localized in the UI layer using string resource
                return@update currentState.copy(
                    errorMessage = ERROR_MAX_SELECTION_ZERO
                )
            }
            
            val newSelected = if (currentSelected.contains(addonId)) {
                // Deselect
                currentSelected - addonId
            } else {
                // Select
                if (addonGroup.isSingleSelection || maxSelection == 1) {
                    // Single choice: replace selection (max 1 or explicit single-select)
                    setOf(addonId)
                } else if (maxSelection != null && currentSelected.size >= maxSelection) {
                    return@update currentState.copy(
                        errorMessage = buildMaxSelectionReachedError(
                            maxSelection,
                            addonGroup.name
                        )
                    )
                } else {
                    // Add to selection
                    currentSelected + addonId
                }
            }
            
            val updatedSelectedAddons = if (newSelected.isEmpty()) {
                currentState.selectedAddons - addonGroupId
            } else {
                currentState.selectedAddons + (addonGroupId to newSelected)
            }
            
            currentState.copy(selectedAddons = updatedSelectedAddons)
        }
    }
    
    fun increaseQuantity() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val product = currentState.product ?: return@launch
            
            // Check if stock is enabled
            if (product.isStockEnabled != true || product.stockQuantity == null) {
                // Stock not enabled, allow increase
                _uiState.update { 
                    it.copy(
                        quantity = currentState.quantity + 1, 
                        errorMessage = null
                    ) 
                }
                return@launch
            }
            
            val newQuantity = currentState.quantity + 1
            
            // Get existing cart items for this product
            val existingCartItems = cartRepository.getCartItemsByProduct(product.id).first()
            val totalQuantityAfterChange = projectedTotalProductQuantityForDetail(
                editingCartItemId = currentState.editingCartItemId,
                existingCartItems = existingCartItems,
                newDetailQuantity = newQuantity
            )

            // Use API snapshot only — Room may not have this product yet right after login
            // (ensureProductForCart is only called from addToCart, not on +/-).
            val hasStock = fitsApiStockCeiling(product, totalQuantityAfterChange)

            if (!hasStock) {
                _uiState.update {
                    it.copy(
                        errorMessage = ERROR_INSUFFICIENT_STOCK
                    )
                }
            } else {
                // Only update quantity if stock is available
                _uiState.update { 
                    it.copy(
                        quantity = newQuantity, 
                        errorMessage = null
                    ) 
                }
            }
        }
    }
    
    fun decreaseQuantity() {
        val currentState = _uiState.value
        if (currentState.quantity > 1) {
            _uiState.update { 
                it.copy(
                    quantity = currentState.quantity - 1,
                    errorMessage = null
                ) 
            }
        }
    }
    
    fun updateQuantity(newQuantity: Int) {
        if (newQuantity >= 1) {
            viewModelScope.launch {
                val currentState = _uiState.value
                val product = currentState.product ?: return@launch
                
                if (product.isStockEnabled != true || product.stockQuantity == null) {
                    _uiState.update {
                        it.copy(quantity = newQuantity, errorMessage = null)
                    }
                    return@launch
                }
                
                val existingCartItems = cartRepository.getCartItemsByProduct(product.id).first()
                val totalQuantityAfterChange = projectedTotalProductQuantityForDetail(
                    editingCartItemId = currentState.editingCartItemId,
                    existingCartItems = existingCartItems,
                    newDetailQuantity = newQuantity
                )
                // Use API snapshot only — see comment in increaseQuantity()
                val hasStock = fitsApiStockCeiling(product, totalQuantityAfterChange)

                if (hasStock) {
                    _uiState.update {
                        it.copy(
                            quantity = newQuantity,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = ERROR_INSUFFICIENT_STOCK)
                    }
                }
            }
        }
    }
    
    fun updateSpecialRequest(request: String) {
        _uiState.update { it.copy(specialRequest = request) }
    }
    
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearAddToCartSuccess() {
        _uiState.update { it.copy(isAddToCartSuccess = false) }
    }
    
    /**
     * Validates that all required addon groups have at least one addon selected.
     * Non-required groups are allowed to be empty and will not block adding to cart.
     * 
     * @return Error message if validation fails, null if validation passes
     */
    private fun validateRequiredAddonGroups(): String? {
        val currentState = _uiState.value
        if (currentState.product?.hasAdditionalOptions != true) return null
        val requiredGroups = currentState.addonGroups.filter { it.isRequired }
        
        // Check if any required group has no selected addons
        val missingGroups = requiredGroups.filter { group ->
            val selectedAddons = currentState.selectedAddons[group.id] ?: emptySet()
            selectedAddons.isEmpty()
        }
        
        // Return error only if required groups are missing
        // Non-required groups are intentionally allowed to be empty
        return if (missingGroups.isNotEmpty()) {
            val groupNames = missingGroups.joinToString(", ") { it.name }
            getLocalizedString(R.string.product_detail_required_addon_error, groupNames)
        } else {
            null
        }
    }
    
    fun addToCart() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val product = currentState.product ?: return@launch

            // Ensure product exists in Room (for FK) when coming from API
            cartRepository.ensureProductForCart(product, currentState.category)
            
            // Validate required addon groups
            val validationError = validateRequiredAddonGroups()
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError) }
                return@launch
            }
            
            // Clear any previous error
            _uiState.update { it.copy(errorMessage = null) }
            
            // Get existing cart items for this product
            val existingCartItems = cartRepository.getCartItemsByProduct(product.id).first()
            
            // Create key for current selection ([CartItem.configurationKey])
            val currentKey = addonsSelectionConfigurationKey(
                productId = product.id,
                specialRequest = currentState.specialRequest,
                selectedAddons = currentState.selectedAddons,
                includeProductId = true
            )
            
            // Helper to calculate addon price for new item
            fun calculateAddonPrice(): Double {
                return currentState.selectedAddons.values.flatten().sumOf { addonId ->
                    currentState.addonsByGroup.values.flatten().find { it.id == addonId }?.price ?: 0.0
                }
            }
            
            val editingCartItemId = currentState.editingCartItemId
            
            if (editingCartItemId != null) {
                // EDIT MODE: อัปเดตทั้งกรุ๊ป (ไม่ใช่แค่ cart item เดียว)
                val originalItem = existingCartItems.find { it.id == editingCartItemId }
                
                if (originalItem == null) {
                    // ถ้าไม่เจอ item เดิม ให้ fallback เป็นโหมดปกติ
                    _uiState.update { it.copy(editingCartItemId = null) }
                } else {
                    // หา cart items ทั้งหมดที่อยู่ในกรุ๊ปเดียวกันกับ originalItem (ใช้ key เดิม)
                    val originalKey = createGroupKeyForCartItem(originalItem)
                    val itemsInOriginalGroup = existingCartItems.filter { cartItem ->
                        createGroupKeyForCartItem(cartItem) == originalKey
                    }
                    
                    // คำนวณจำนวนรวมของกรุ๊ปเดิม
                    val totalQuantityFromOriginalGroup = itemsInOriginalGroup.sumOf { it.quantity }
                    
                    // หา item อื่นที่ config ใหม่เหมือนกับของใหม่ (ไว้ merge)
                    val matchingOtherItem = existingCartItems.find { cartItem ->
                        // ไม่นับ items ที่อยู่ในกรุ๊ปเดิม (จะลบทิ้งอยู่แล้ว)
                        if (itemsInOriginalGroup.any { it.id == cartItem.id }) return@find false
                        cartItem.configurationKey(includeProductId = true) == currentKey
                    }
                    
                    // จำนวนรวมในตะกร้าที่ยกเว้นกรุ๊ปเดิมทั้งหมด
                    val totalQuantityExcludingOriginalGroup = existingCartItems
                        .filter { !itemsInOriginalGroup.any { original -> original.id == it.id } }
                        .sumOf { it.quantity }
                    
                    // เตรียม config ใหม่ (price + addons)
                    val addonPrice = calculateAddonPrice()
                    val unitPrice = product.price + addonPrice
                    
                    val cartAddons = mutableListOf<CartAddonEntity>()
                    currentState.selectedAddons.forEach { (groupId, addonIds) ->
                        val addonGroup = currentState.addonGroups.find { it.id == groupId }
                        addonIds.forEach { addonId ->
                            val addon = currentState.addonsByGroup[groupId]?.find { it.id == addonId }
                            if (addon != null && addonGroup != null) {
                                cartAddons.add(
                                    CartAddonEntity(
                                        cartItemId = "", // Will be set when creating new item
                                        addonId = addon.id,
                                        addonName = addon.name,
                                        addonPrice = addon.price,
                                        addonGroupId = addonGroup.id,
                                        addonGroupName = addonGroup.name
                                    )
                                )
                            }
                        }
                    }
                    
                    // ใช้จำนวนที่ผู้ใช้แก้ไข (currentState.quantity) แทนจำนวนเดิม
                    val newQuantity = currentState.quantity

                    // หลังบันทึก: ลบกรุ๊ปเดิมทั้งหมด แล้ว add/merge newQuantity เข้าไป
                    // จำนวนรวมของสินค้านี้ในตะกร้าจะเป็น (cart - กรุ๊ปเดิม) + newQuantity เสมอ
                    // (matching branch จะเพียงแค่ย้ายจำนวนไปอยู่บนแถวเดิมที่ตรงกัน ไม่เพิ่ม total ของสินค้านี้)
                    val totalQuantityAfterChange = totalQuantityExcludingOriginalGroup + newQuantity
                    val hasStock = fitsApiStockCeiling(product, totalQuantityAfterChange) &&
                        cartRepository.checkStockAvailability(product.id, totalQuantityAfterChange)
                    if (!hasStock) {
                        _uiState.update { it.copy(errorMessage = ERROR_INSUFFICIENT_STOCK) }
                        return@launch
                    }

                    if (matchingOtherItem != null) {
                        // กรณี config ใหม่เหมือนกับ item อื่น -> merge จำนวนใหม่เข้าไปบนแถวเดิม
                        val newQuantityForMatching = matchingOtherItem.quantity + newQuantity
                        cartRepository.updateCartItemQuantity(matchingOtherItem.id, newQuantityForMatching)
                        val originalGroupItemIds = itemsInOriginalGroup.map { it.id }
                        cartRepository.deleteCartItems(originalGroupItemIds)
                    } else {
                        // กรณี config ใหม่ไม่ตรงกับ item ไหนเลย -> ลบกรุ๊ปเดิมทั้งหมด แล้วสร้าง cart item ใหม่
                        val originalGroupItemIds = itemsInOriginalGroup.map { it.id }
                        cartRepository.deleteCartItems(originalGroupItemIds)

                        cartRepository.addToCart(
                            productId = product.id,
                            productName = product.name,
                            productImageUrl = product.imageUrl,
                            productColorHex = product.selectedColorHex,
                            unitPrice = unitPrice,
                            quantity = newQuantity,
                            specialRequest = currentState.specialRequest.takeIf { it.isNotBlank() },
                            addons = cartAddons
                        )
                    }
                }
                
                // Mark as success (and clear edit state)
                _uiState.update { 
                    it.copy(
                        isAddToCartSuccess = true, 
                        errorMessage = null,
                        editingCartItemId = null
                    ) 
                }
            } else {
                // NORMAL MODE: existing behavior (add / merge)
                // Find matching cart item (same product, addons, and special request)
                val matchingCartItem = existingCartItems.find { cartItem ->
                    cartItem.configurationKey(includeProductId = true) == currentKey
                }
                
                if (matchingCartItem != null) {
                    // If matching item exists, increase quantity instead of adding new item
                    val newQuantity = matchingCartItem.quantity + currentState.quantity
                    val totalQtyInCart = existingCartItems.sumOf { it.quantity }
                    val totalAfterMerge = totalQtyInCart - matchingCartItem.quantity + newQuantity

                    val hasStock = fitsApiStockCeiling(product, totalAfterMerge) &&
                        cartRepository.checkStockAvailability(product.id, totalAfterMerge)
                    if (!hasStock) {
                        _uiState.update { it.copy(errorMessage = ERROR_INSUFFICIENT_STOCK) }
                        return@launch
                    }

                    cartRepository.updateCartItemQuantity(matchingCartItem.id, newQuantity)
                } else {
                    val totalQtyInCart = existingCartItems.sumOf { it.quantity }
                    val totalAfterAdd = totalProductUnitsAfterDetailQuantityChange(
                        totalQuantityInCart = totalQtyInCart,
                        matchingCartItem = null,
                        newDetailQuantity = currentState.quantity
                    )

                    val hasStock = fitsApiStockCeiling(product, totalAfterAdd) &&
                        cartRepository.checkStockAvailability(product.id, totalAfterAdd)
                    if (!hasStock) {
                        _uiState.update { it.copy(errorMessage = ERROR_INSUFFICIENT_STOCK) }
                        return@launch
                    }
                    
                    // No matching item found, add new item to cart
                    val addonPrice = calculateAddonPrice()
                    val unitPrice = product.price + addonPrice
                    
                    val cartAddons = mutableListOf<CartAddonEntity>()
                    currentState.selectedAddons.forEach { (groupId, addonIds) ->
                        val addonGroup = currentState.addonGroups.find { it.id == groupId }
                        addonIds.forEach { addonId ->
                            val addon = currentState.addonsByGroup[groupId]?.find { it.id == addonId }
                            if (addon != null && addonGroup != null) {
                                cartAddons.add(
                                    CartAddonEntity(
                                        cartItemId = "", // Will be set by repository
                                        addonId = addon.id,
                                        addonName = addon.name,
                                        addonPrice = addon.price,
                                        addonGroupId = addonGroup.id,
                                        addonGroupName = addonGroup.name
                                    )
                                )
                            }
                        }
                    }
                    
                    cartRepository.addToCart(
                        productId = product.id,
                        productName = product.name,
                        productImageUrl = product.imageUrl,
                        productColorHex = product.selectedColorHex,
                        unitPrice = unitPrice,
                        quantity = currentState.quantity,
                        specialRequest = currentState.specialRequest.takeIf { it.isNotBlank() },
                        addons = cartAddons
                    )
                }
                
                // Mark as success
                _uiState.update { 
                    it.copy(
                        isAddToCartSuccess = true, 
                        errorMessage = null
                    ) 
                }
            }
        }
    }
    
    /**
     * คำนวณยอดรวมของสินค้านี้ในตะกร้าที่จะเป็นหลัง "บันทึก" detail screen
     *
     * - โหมด edit (editingCartItemId != null): จะแทนที่ทั้งกรุ๊ปเดิมด้วย detail.quantity ใหม่
     *   → totalAfter = totalCart − originalGroupTotal + newDetailQuantity
     * - โหมดเพิ่มใหม่: detail.quantity จะถูก add/merge เพิ่มเข้าไปบน cart เดิม
     *   → totalAfter = totalCart + newDetailQuantity
     */
    private fun projectedTotalProductQuantityForDetail(
        editingCartItemId: String?,
        existingCartItems: List<CartItem>,
        newDetailQuantity: Int
    ): Int {
        val totalQuantityInCart = existingCartItems.sumOf { it.quantity }
        val replacedFromCart = if (editingCartItemId != null) {
            val originalItem = existingCartItems.find { it.id == editingCartItemId }
            if (originalItem != null) {
                val key = createGroupKeyForCartItem(originalItem)
                existingCartItems
                    .filter { createGroupKeyForCartItem(it) == key }
                    .sumOf { it.quantity }
            } else 0
        } else 0
        return totalQuantityInCart - replacedFromCart + newDetailQuantity
    }

    private fun totalProductUnitsAfterDetailQuantityChange(
        totalQuantityInCart: Int,
        matchingCartItem: CartItem?,
        newDetailQuantity: Int
    ): Int {
        return if (matchingCartItem != null) {
            totalQuantityInCart - matchingCartItem.quantity + newDetailQuantity
        } else {
            totalQuantityInCart + newDetailQuantity
        }
    }

    /** Stock ceiling from product detail (API snapshot in uiState); does not read Room. */
    private fun fitsApiStockCeiling(product: ProductEntity, totalUnitsOfThisProductAfter: Int): Boolean {
        if (product.isStockEnabled != true || product.stockQuantity == null) return true
        return totalUnitsOfThisProductAfter <= product.stockQuantity
    }

    /** Same string as order/cart grouping ([CartItem.configurationKey]). */
    private fun createGroupKeyForCartItem(item: CartItem): String =
        item.configurationKey(includeProductId = true)
}


