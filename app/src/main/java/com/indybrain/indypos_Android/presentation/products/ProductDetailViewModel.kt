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
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        product = product,
                        addonGroups = addonGroups,
                        addonsByGroup = addonsByGroup,
                        selectedAddons = emptyMap(),
                        quantity = 1,
                        specialRequest = ""
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
    
    fun addToCart() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val product = currentState.product ?: return@launch
            
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
                                cartItemId = 0, // Will be set by repository
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
    }
}


