package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
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
    private val cartRepository: CartRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get product
                val product = productDao.getAllActiveProducts().find { it.id == productId }
                
                if (product == null) {
                    _uiState.update { it.copy(isLoading = false, product = null) }
                    return@launch
                }
                
                // Get all addon groups (for now, we'll show all if product has additional options)
                // In a real app, you might have a product-addon-group relationship table
                val addonGroups = if (product.hasAdditionalOptions == true) {
                    addonGroupDao.getAllActiveAddonGroups()
                } else {
                    emptyList()
                }
                
                // Get addons for each group
                val addonsByGroup = addonGroups.associate { group ->
                    group.id to addonDao.getAddonsByGroup(group.id)
                }
                
                // Check if product is already in cart and load existing data
                val existingCartItems = cartRepository.getCartItemsByProduct(productId).first()
                val existingCartItem = existingCartItems.firstOrNull()
                
                // Load existing cart data if available
                val existingQuantity = existingCartItem?.quantity ?: 1
                val existingSpecialRequest = existingCartItem?.specialRequest ?: ""
                
                // Load existing addons from cart
                val existingSelectedAddons = if (existingCartItem != null) {
                    val cartAddons = cartRepository.getCartAddonsByItemId(existingCartItem.id)
                    cartAddons.groupBy { it.addonGroupId }
                        .mapValues { (_, addons) ->
                            addons.map { it.addonId }.toSet()
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
                        specialRequest = existingSpecialRequest
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
            val newSelected = if (currentSelected.contains(addonId)) {
                // Deselect
                currentSelected - addonId
            } else {
                // Select
                val maxSelection = addonGroup.maxSelection
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
    
    fun updateQuantity(newQuantity: Int) {
        if (newQuantity >= 1) {
            _uiState.update { it.copy(quantity = newQuantity) }
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
    
    private fun validateRequiredAddonGroups(): String? {
        val currentState = _uiState.value
        val requiredGroups = currentState.addonGroups.filter { it.isRequired }
        
        val missingGroups = requiredGroups.filter { group ->
            val selectedAddons = currentState.selectedAddons[group.id] ?: emptySet()
            selectedAddons.isEmpty()
        }
        
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
                cartRepository.updateCartItemQuantity(matchingCartItem.id, newQuantity)
            } else {
                // No matching item found, add new item to cart
                // Calculate addon price
                val addonPrice = currentState.selectedAddons.values.flatten().sumOf { addonId ->
                    currentState.addonsByGroup.values.flatten().find { it.id == addonId }?.price ?: 0.0
                }
                val unitPrice = product.price + addonPrice
                
                // Prepare addons
                val cartAddons = mutableListOf<CartAddonEntity>()
                currentState.selectedAddons.forEach { (groupId, addonIds) ->
                    val addonGroup = currentState.addonGroups.find { it.id == groupId }
                    addonIds.forEach { addonId ->
                        val addon = currentState.addonsByGroup[groupId]?.find { it.id == addonId }
                        if (addon != null && addonGroup != null) {
                            cartAddons.add(
                                CartAddonEntity(
                                    cartItemId = "", // Will be set by repository (temporary empty string)
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
                
                // Add to cart
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
            _uiState.update { it.copy(isAddToCartSuccess = true, errorMessage = null) }
        }
    }
}


