package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.local.dao.ProductAddonGroupJunctionDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupAddonJunctionDao
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val addonGroupDao: AddonGroupDao,
    private val addonDao: AddonDao,
    private val productAddonGroupJunctionDao: ProductAddonGroupJunctionDao,
    private val addonGroupAddonJunctionDao: AddonGroupAddonJunctionDao,
    private val cartRepository: CartRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    
    fun loadProduct(productId: String, cartItemId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get product
                val product = productDao.getAllActiveProducts().find { it.id == productId }
                
                if (product == null) {
                    _uiState.update { it.copy(isLoading = false, product = null) }
                    return@launch
                }
                
                // Get addon groups that are associated with this product via junction table
                val addonGroups = if (product.hasAdditionalOptions == true) {
                    // Get addon group IDs for this product from junction table
                    val addonGroupIds = productAddonGroupJunctionDao.getAddonGroupIdsByProductIdSync(productId)
                    
                    // Get addon groups by IDs and filter to show only active groups
                    // getAddonGroupById already filters isDeletedLocally = 0, but we need to check isActive = true
                    if (addonGroupIds.isNotEmpty()) {
                        addonGroupIds.mapNotNull { groupId ->
                            addonGroupDao.getAddonGroupById(groupId)
                        }.filter { addonGroup ->
                            // Only show addon groups that are active and not deleted
                            addonGroup.isActive && !addonGroup.isDeletedLocally
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                
                // Get addons for each group using junction table
                // First get addon IDs from junction table, then get addon entities
                val addonsByGroup = addonGroups.associate { group ->
                    // Get addon IDs from junction table
                    val addonIds = addonGroupAddonJunctionDao.getAddonIdsByAddonGroupIdSync(group.id)
                    
                    // Get addon entities by IDs and filter only active ones
                    val addons = if (addonIds.isNotEmpty()) {
                        addonIds.mapNotNull { addonId ->
                            addonDao.getAddonById(addonId)
                        }.filter { addon ->
                            addon.isActive && !addon.isDeletedLocally
                        }
                    } else {
                        emptyList()
                    }
                    
                    // Debug: Log if group has no addons
                    if (addons.isEmpty()) {
                        android.util.Log.d("ProductDetailVM", "AddonGroup ${group.name} (${group.id}) has no active addons. Junction addonIds: $addonIds")
                    }
                    
                    group.id to addons
                }
                
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
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        product = product,
                        addonGroups = addonGroups,
                        addonsByGroup = addonsByGroup,
                        selectedAddons = existingSelectedAddons,
                        quantity = existingQuantity,
                        specialRequest = existingSpecialRequest,
                        editingCartItemId = if (!isExplicitNew && existingCartItem != null) existingCartItem.id else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
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
                    errorMessage = "MAX_SELECTION_ZERO" // Special error code for localization
                )
            }
            
            val newSelected = if (currentSelected.contains(addonId)) {
                // Deselect
                currentSelected - addonId
            } else {
                // Select
                if (addonGroup.isSingleSelection) {
                    // Single selection - replace current selection
                    setOf(addonId)
                } else if (maxSelection != null && currentSelected.size >= maxSelection) {
                    // Max selection reached - don't add
                    currentSelected
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
            
            // Find matching cart item (same product, addons, and special request)
            val currentSpecialRequest = currentState.specialRequest ?: ""
            val sortedGroups = currentState.selectedAddons.keys.sorted()
            val addonsKey = sortedGroups.joinToString("|") { groupId ->
                val addonIds = currentState.selectedAddons[groupId]
                    ?.sorted()
                    ?.joinToString(",") ?: ""
                "$groupId:$addonIds"
            }
            val currentKey = "${product.id}|$currentSpecialRequest|$addonsKey"
            
            val matchingCartItem = existingCartItems.find { cartItem ->
                val itemSpecialRequest = cartItem.specialRequest ?: ""
                val itemSortedGroups = cartItem.selectedAddons.keys.sorted()
                val itemAddonsKey = itemSortedGroups.joinToString("|") { groupId ->
                    val addonIds = cartItem.selectedAddons[groupId]
                        ?.map { it.id }
                        ?.sorted()
                        ?.joinToString(",") ?: ""
                    "$groupId:$addonIds"
                }
                val itemKey = "${cartItem.product.id}|$itemSpecialRequest|$itemAddonsKey"
                itemKey == currentKey
            }
            
            // Calculate total quantity that would be in cart after increase
            // If matching item exists, we need to calculate what the new total would be
            val totalQuantityInCart = existingCartItems.sumOf { it.quantity }
            val totalQuantityAfterChange = if (matchingCartItem != null) {
                // If matching item exists, replace its quantity with newQuantity
                totalQuantityInCart - matchingCartItem.quantity + newQuantity
            } else {
                // If no matching item, add newQuantity to total
                totalQuantityInCart + newQuantity
            }
            
            // Check stock availability using checkStockAvailability
            val hasStock = cartRepository.checkStockAvailability(product.id, totalQuantityAfterChange)
            
            if (!hasStock) {
                // Show error toast if stock is insufficient
                val stockQuantity = product.stockQuantity ?: 0
                val availableStock = if (matchingCartItem != null) {
                    stockQuantity - (totalQuantityInCart - matchingCartItem.quantity)
                } else {
                    stockQuantity - totalQuantityInCart
                }
                val errorMessage = if (availableStock > 0) {
                    "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                } else {
                    "สินค้าในสต็อกไม่เพียงพอ"
                }
                _uiState.update { 
                    it.copy(
                        errorMessage = errorMessage
                        // Keep current quantity, don't update
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
                
                // Check stock availability including existing cart items BEFORE updating
                val existingCartItems = cartRepository.getCartItemsByProduct(product.id).first()
                val totalQuantityInCart = existingCartItems.sumOf { it.quantity }
                // Calculate what the total quantity would be if we update to newQuantity
                val totalQuantity = totalQuantityInCart + newQuantity - currentState.quantity
                
                // Check stock availability BEFORE updating quantity
                val hasStock = cartRepository.checkStockAvailability(product.id, totalQuantity)
                
                if (hasStock) {
                    // Only update quantity if stock is available
                    _uiState.update { 
                        it.copy(
                            quantity = newQuantity, 
                            errorMessage = null
                        ) 
                    }
                } else {
                    // Show error toast if stock is insufficient - DON'T update quantity
                    val stockQuantity = product.stockQuantity ?: 0
                    val availableStock = stockQuantity - totalQuantityInCart
                    val errorMessage = if (availableStock > 0) {
                        "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                    } else {
                        "สินค้าในสต็อกไม่เพียงพอ"
                    }
                    _uiState.update { 
                        it.copy(
                            errorMessage = errorMessage
                            // Keep current quantity, don't update to newQuantity
                        ) 
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
        // Only check groups that are marked as required
        // Groups without isRequired can be skipped and won't block adding to cart
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
            "กรุณาเลือก ${groupNames}"
        } else {
            null
        }
    }
    
    fun addToCart() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val product = currentState.product ?: return@launch
            
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
            
            // Create key for current selection (same logic as GetGroupedCartItemsUseCase)
            val currentSpecialRequest = currentState.specialRequest ?: ""
            val sortedGroups = currentState.selectedAddons.keys.sorted()
            val addonsKey = sortedGroups.joinToString("|") { groupId ->
                val addonIds = currentState.selectedAddons[groupId]
                    ?.sorted()
                    ?.joinToString(",") ?: ""
                "$groupId:$addonIds"
            }
            val currentKey = "${product.id}|$currentSpecialRequest|$addonsKey"
            
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
                        
                        val itemSpecialRequest = cartItem.specialRequest ?: ""
                        val itemSortedGroups = cartItem.selectedAddons.keys.sorted()
                        val itemAddonsKey = itemSortedGroups.joinToString("|") { groupId ->
                            val addonIds = cartItem.selectedAddons[groupId]
                                ?.map { it.id }
                                ?.sorted()
                                ?.joinToString(",") ?: ""
                            "$groupId:$addonIds"
                        }
                        val itemKey = "${cartItem.product.id}|$itemSpecialRequest|$itemAddonsKey"
                        itemKey == currentKey
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
                    
                    if (matchingOtherItem != null) {
                        // กรณี config ใหม่เหมือนกับ item อื่น -> merge จำนวนใหม่เข้าไป
                        val newQuantityForMatching = matchingOtherItem.quantity + newQuantity
                        val totalQuantityAfterChange = totalQuantityExcludingOriginalGroup + newQuantityForMatching
                        
                        val hasStock = cartRepository.checkStockAvailability(product.id, totalQuantityAfterChange)
                        if (!hasStock) {
                            val stockQuantity = product.stockQuantity ?: 0
                            val availableStock = stockQuantity - totalQuantityExcludingOriginalGroup
                            val errorMessage = if (availableStock > 0) {
                                "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                            } else {
                                "สินค้าในสต็อกไม่เพียงพอ"
                            }
                            _uiState.update { it.copy(errorMessage = errorMessage) }
                            return@launch
                        }
                        
                        // อัปเดตจำนวนของ item ที่ match แล้วลบกรุ๊ปเดิมทั้งหมดทิ้ง
                        cartRepository.updateCartItemQuantity(matchingOtherItem.id, newQuantityForMatching)
                        val originalGroupItemIds = itemsInOriginalGroup.map { it.id }
                        cartRepository.deleteCartItems(originalGroupItemIds)
                    } else {
                        // กรณี config ใหม่ไม่ตรงกับ item ไหนเลย -> ลบกรุ๊ปเดิมทั้งหมด แล้วสร้าง cart item ใหม่ด้วย config ใหม่ + จำนวนใหม่
                        val totalQuantityAfterChange = totalQuantityExcludingOriginalGroup + newQuantity
                        val hasStock = cartRepository.checkStockAvailability(product.id, totalQuantityAfterChange)
                        if (!hasStock) {
                            val stockQuantity = product.stockQuantity ?: 0
                            val availableStock = stockQuantity - totalQuantityExcludingOriginalGroup
                            val errorMessage = if (availableStock > 0) {
                                "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                            } else {
                                "สินค้าในสต็อกไม่เพียงพอ"
                            }
                            _uiState.update { it.copy(errorMessage = errorMessage) }
                            return@launch
                        }
                        
                        // ลบ cart items ทั้งหมดในกรุ๊ปเดิม
                        val originalGroupItemIds = itemsInOriginalGroup.map { it.id }
                        cartRepository.deleteCartItems(originalGroupItemIds)
                        
                        // สร้าง cart item ใหม่ด้วย config ใหม่ + จำนวนใหม่ที่ผู้ใช้แก้ไข
                        cartRepository.addToCart(
                            productId = product.id,
                            productName = product.name,
                            productImageUrl = product.imageUrl,
                            productColorHex = product.selectedColorHex,
                            unitPrice = unitPrice,
                            quantity = newQuantity, // ใช้จำนวนใหม่ที่ผู้ใช้แก้ไข
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
                    val itemSpecialRequest = cartItem.specialRequest ?: ""
                    val itemSortedGroups = cartItem.selectedAddons.keys.sorted()
                    val itemAddonsKey = itemSortedGroups.joinToString("|") { groupId ->
                        val addonIds = cartItem.selectedAddons[groupId]
                            ?.map { it.id }
                            ?.sorted()
                            ?.joinToString(",") ?: ""
                        "$groupId:$addonIds"
                    }
                    val itemKey = "${cartItem.product.id}|$itemSpecialRequest|$itemAddonsKey"
                    itemKey == currentKey
                }
                
                if (matchingCartItem != null) {
                    // If matching item exists, increase quantity instead of adding new item
                    val newQuantity = matchingCartItem.quantity + currentState.quantity
                    
                    // Check stock availability before updating
                    val hasStock = cartRepository.checkStockAvailability(product.id, newQuantity)
                    if (!hasStock) {
                        val stockQuantity = product.stockQuantity ?: 0
                        val totalQuantityInCart = existingCartItems.sumOf { it.quantity }
                        val availableStock = stockQuantity - totalQuantityInCart
                        val errorMessage = if (availableStock > 0) {
                            "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                        } else {
                            "สินค้าในสต็อกไม่เพียงพอ"
                        }
                        _uiState.update { it.copy(errorMessage = errorMessage) }
                        return@launch
                    }
                    
                    cartRepository.updateCartItemQuantity(matchingCartItem.id, newQuantity)
                } else {
                    // Check stock availability before adding new item
                    val hasStock = cartRepository.checkStockAvailability(product.id, currentState.quantity)
                    if (!hasStock) {
                        val stockQuantity = product.stockQuantity ?: 0
                        val totalQuantityInCart = existingCartItems.sumOf { it.quantity }
                        val availableStock = stockQuantity - totalQuantityInCart
                        val errorMessage = if (availableStock > 0) {
                            "สินค้าในสต็อกไม่เพียงพอ เหลือเพียง $availableStock ชิ้น"
                        } else {
                            "สินค้าในสต็อกไม่เพียงพอ"
                        }
                        _uiState.update { it.copy(errorMessage = errorMessage) }
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
     * สร้าง key สำหรับ grouping CartItem ให้ตรงกับ logic ใน GetGroupedCartItemsByProductUseCase
     * รูปแบบ: "specialRequest|groupId1:addonId1,addonId2|groupId2:addonId3"
     */
    private fun createGroupKeyForCartItem(
        item: com.indybrain.indypos_Android.domain.model.CartItem
    ): String {
        val specialRequest = item.specialRequest ?: ""
        
        // Sort addon groups by groupId
        val sortedGroups = item.selectedAddons.keys.sorted()
        
        // Create addons key: "groupId1:addonId1,addonId2|groupId2:addonId3"
        val addonsKey = sortedGroups.joinToString("|") { groupId ->
            val addonIds = item.selectedAddons[groupId]
                ?.map { it.id }
                ?.sorted()
                ?.joinToString(",") ?: ""
            "$groupId:$addonIds"
        }
        
        return "$specialRequest|$addonsKey"
    }
}


