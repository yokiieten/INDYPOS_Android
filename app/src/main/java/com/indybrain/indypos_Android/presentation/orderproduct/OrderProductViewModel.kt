package com.indybrain.indypos_Android.presentation.orderproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderProductViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderProductUiState())
    val uiState: StateFlow<OrderProductUiState> = _uiState.asStateFlow()
    
    private val _cartAddonsMap = MutableStateFlow<Map<Long, List<CartAddonEntity>>>(emptyMap())
    
    init {
        observeCartItems()
    }
    
    private fun observeCartItems() {
        viewModelScope.launch {
            cartRepository.getCartItems().collect { cartItems ->
                _uiState.update { it.copy(cartItems = cartItems) }
                
                // Load addons for each cart item
                val addonsMap = mutableMapOf<Long, List<CartAddonEntity>>()
                cartItems.forEach { item ->
                    val addons = cartRepository.getCartAddonsByItemId(item.id)
                    addonsMap[item.id] = addons
                }
                _cartAddonsMap.value = addonsMap
            }
        }
    }
    
    fun selectPaymentType(paymentType: PaymentType) {
        _uiState.update { it.copy(selectedPaymentType = paymentType) }
    }
    
    fun setDiscountAmount(amount: Double) {
        _uiState.update { it.copy(discountAmount = amount) }
    }
    
    fun getCartAddons(itemId: Long): List<CartAddonEntity> {
        return _cartAddonsMap.value[itemId] ?: emptyList()
    }
    
    fun calculateSubtotal(): Double {
        return _uiState.value.cartItems.sumOf { item ->
            val itemPrice = item.unitPrice * item.quantity
            val addonsPrice = (_cartAddonsMap.value[item.id] ?: emptyList())
                .sumOf { it.addonPrice } * item.quantity
            itemPrice + addonsPrice
        }
    }
    
    fun calculateTotal(): Double {
        val subtotal = calculateSubtotal()
        val discount = _uiState.value.discountAmount
        return (subtotal - discount).coerceAtLeast(0.0)
    }
    
    fun deleteCartItem(itemId: Long) {
        viewModelScope.launch {
            cartRepository.deleteCartItem(itemId)
        }
    }
    
    fun placeOrder() {
        // TODO: Implement order placement logic
        viewModelScope.launch {
            // Place order logic here
        }
    }
}

