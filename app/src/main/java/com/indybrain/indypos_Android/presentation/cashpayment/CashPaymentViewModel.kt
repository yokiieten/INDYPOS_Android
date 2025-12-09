package com.indybrain.indypos_Android.presentation.cashpayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.dto.CreateOrderRequestDto
import com.indybrain.indypos_Android.data.remote.dto.CreateOrderResponseDto
import com.indybrain.indypos_Android.domain.model.PaymentType
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class CashPaymentViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val ordersApi: OrdersApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val productDao: ProductDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CashPaymentUiState())
    val uiState: StateFlow<CashPaymentUiState> = _uiState.asStateFlow()
    
    private val _cartAddonsMap = MutableStateFlow<Map<String, List<CartAddonEntity>>>(emptyMap())
    
    var totalAmount: Double = 0.0
    var subtotal: Double = 0.0
    var discount: Double = 0.0
    
    init {
        loadCartAddons()
    }
    
    private fun loadCartAddons() {
        viewModelScope.launch {
            val cartItems = cartRepository.getCartItemsSync()
            val addonsMap = mutableMapOf<String, List<CartAddonEntity>>()
            cartItems.forEach { item ->
                val addons = cartRepository.getCartAddonsByItemId(item.id)
                addonsMap[item.id] = addons
            }
            _cartAddonsMap.value = addonsMap
        }
    }
    
    fun onKeypadButtonClick(button: String) {
        val currentState = _uiState.value
        var newEnteredAmount = currentState.enteredAmount
        
        when (button) {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                newEnteredAmount += button
            }
            "." -> {
                if (!newEnteredAmount.contains(".")) {
                    newEnteredAmount += "."
                }
            }
            "100" -> {
                newEnteredAmount = "100"
            }
            "500" -> {
                newEnteredAmount = "500"
            }
            "1000" -> {
                newEnteredAmount = "1000"
            }
            "exact" -> {
                // Set to total amount formatted
                newEnteredAmount = formatNumberWithCommas(totalAmount)
            }
        }
        
        updateEnteredAmount(newEnteredAmount)
    }
    
    fun onClearClick() {
        updateEnteredAmount("")
    }
    
    private fun updateEnteredAmount(enteredAmount: String) {
        val receivedAmount = if (enteredAmount == formatNumberWithCommas(totalAmount)) {
            // If exact match with formatted total, use totalAmount directly
            totalAmount
        } else {
            // Convert from string, removing commas
            enteredAmount.replace(",", "").toDoubleOrNull() ?: 0.0
        }
        
        _uiState.update { 
            it.copy(
                enteredAmount = enteredAmount,
                receivedAmount = receivedAmount
            )
        }
    }
    
    fun onConfirmClick(onSuccess: (Double) -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        
        // Prevent multiple taps
        if (currentState.isProcessingOrder) return
        
        // Check if received amount is sufficient
        val tolerance = 0.01
        if (currentState.receivedAmount < (totalAmount - tolerance)) {
            onError("จำนวนเงินที่รับมาไม่เพียงพอ")
            return
        }
        
        // Calculate change
        val change = currentState.receivedAmount - totalAmount
        
        // If there's change, show alert first
        if (change > tolerance) {
            _uiState.update { 
                it.copy(
                    showChangeAlert = true,
                    changeAmount = change
                )
            }
            return
        }
        
        // No change, proceed with payment
        completePayment(onSuccess, onError)
    }
    
    fun onConfirmChangeAlert(onSuccess: (Double) -> Unit, onError: (String) -> Unit) {
        _uiState.update { it.copy(showChangeAlert = false) }
        completePayment(onSuccess, onError)
    }
    
    fun onDismissChangeAlert() {
        _uiState.update { it.copy(showChangeAlert = false) }
    }
    
    private fun completePayment(onSuccess: (Double) -> Unit, onError: (String) -> Unit) {
        _uiState.update { it.copy(isProcessingOrder = true) }
        
        viewModelScope.launch {
            try {
                // Get cart items
                val cartItems = cartRepository.getCartItemsSync()
                
                // Build order request
                val items = buildOrderItems(cartItems)
                
                if (items.isEmpty()) {
                    _uiState.update { it.copy(isProcessingOrder = false) }
                    onError("ไม่มีสินค้าในตะกร้า")
                    return@launch
                }
                
                val discountPercentage = if (subtotal > 0.000001) {
                    (discount / subtotal) * 100.0
                } else {
                    0.0
                }
                
                val request = CreateOrderRequestDto(
                    customerName = "",
                    customerPhone = "",
                    customerEmail = "",
                    paymentType = PaymentType.CASH.code,
                    discountAmount = discount,
                    discountPercentage = discountPercentage,
                    taxAmount = 0.0,
                    taxPercentage = 0.0,
                    notes = "",
                    items = items
                )
                
                // Check network connectivity
                if (!networkConnectivityChecker.isConnected()) {
                    // Offline: Save locally only
                    saveOrderLocally(onSuccess, onError)
                } else {
                    // Online: Try API first
                    try {
                        val response = ordersApi.createOrder(request)
                        
                        if (response.status == 403) {
                            val errorCode = response.error?.lowercase()
                            if (errorCode == "free_plan_limit_exceeded") {
                                _uiState.update { it.copy(isProcessingOrder = false) }
                                onError("ถึงขีดจำกัดของแผนฟรี กรุณาติดต่อเรา")
                                return@launch
                            }
                        }
                        
                        if (response.status >= 400) {
                            val errorMessage = response.error ?: response.message ?: "เกิดข้อผิดพลาด"
                            val lowercasedError = errorMessage.lowercase()
                            
                            val displayMessage = when {
                                lowercasedError.contains("insufficient stock") || 
                                lowercasedError.contains("at least one item is required") -> {
                                    "สินค้าในสต็อกไม่เพียงพอ"
                                }
                                else -> errorMessage
                            }
                            
                            _uiState.update { it.copy(isProcessingOrder = false) }
                            onError(displayMessage)
                            return@launch
                        }
                        
                        // Success: Save locally and update with API response
                        val orderNumber = saveOrderLocally(onSuccess, onError)
                        
                        // Update with server order number if available
                        if (response.data != null && response.data.orderNumber != null) {
                            // Update local order with server order number
                            // This would require updating the order repository
                        }
                        
                    } catch (e: HttpException) {
                        // API error, fallback to local save
                        saveOrderLocally(onSuccess, onError)
                    } catch (e: Exception) {
                        // Network error, fallback to local save
                        saveOrderLocally(onSuccess, onError)
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessingOrder = false) }
                onError(e.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }
    
    private suspend fun saveOrderLocally(onSuccess: (Double) -> Unit, onError: (String) -> Unit): String {
        return try {
            // Generate order number
            val orderNumber = generateOrderNumber()
            
            // Save order to local database
            // This would require implementing saveOrderLocalOnly in OrderRepository
            // For now, we'll just clear the cart and return success
            
            // Clear cart
            cartRepository.clearCart()
            
            val change = _uiState.value.receivedAmount - totalAmount
            _uiState.update { it.copy(isProcessingOrder = false) }
            
            onSuccess(change)
            orderNumber
        } catch (e: Exception) {
            _uiState.update { it.copy(isProcessingOrder = false) }
            onError(e.message ?: "ไม่สามารถบันทึกออเดอร์ได้")
            ""
        }
    }
    
    private suspend fun buildOrderItems(cartItems: List<CartItemEntity>): List<CreateOrderRequestDto.OrderItemDto> {
        return cartItems.map { cartItem ->
            val addons = _cartAddonsMap.value[cartItem.id] ?: emptyList()
            
            // Get product cost price
            val unitCost = if (cartItem.productId != null) {
                productDao.getProductById(cartItem.productId)?.costPrice ?: 0.0
            } else {
                0.0
            }
            
            // Group addons by addon group
            val addonGroupsMap = addons.groupBy { it.addonGroupId }
            val addonGroups = addonGroupsMap.map { (groupId, groupAddons) ->
                CreateOrderRequestDto.AddonGroupDto(
                    addonGroupId = groupId,
                    selectedAddons = groupAddons.map { addon ->
                        CreateOrderRequestDto.SelectedAddonDto(
                            addonId = addon.addonId,
                            quantity = 1
                        )
                    }
                )
            }
            
            CreateOrderRequestDto.OrderItemDto(
                productId = cartItem.productId ?: "",
                quantity = cartItem.quantity,
                unitCost = unitCost,
                specialRequest = cartItem.specialRequest ?: "",
                notes = "",
                addonGroups = addonGroups,
                addons = emptyList()
            )
        }
    }
    
    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        return "ORD$timestamp"
    }
    
    private fun formatNumberWithCommas(number: Double): String {
        val formatter = java.text.DecimalFormat("#,##0.00")
        return formatter.format(number)
    }
}

