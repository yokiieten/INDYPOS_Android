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
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.printer.PrinterService
import com.indybrain.indypos_Android.data.local.dao.OrderAddonDao
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.data.local.dao.OrderItemDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.local.entity.OrderAddonEntity
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.model.PaymentType as DomainPaymentType
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import com.indybrain.indypos_Android.domain.repository.ReceiptSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.UUID
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
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val orderAddonDao: OrderAddonDao,
    private val receiptSettingsRepository: ReceiptSettingsRepository,
    private val printerService: PrinterService,
    private val authRepository: AuthRepository
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
        
        // Proceed with payment directly (no change alert)
        completePayment(onSuccess, onError)
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
                    // Get cart addons map before clearing cart
                    val cartAddonsMap = cartItems.associate { item ->
                        item.id to cartRepository.getCartAddonsByItemId(item.id)
                    }
                    
                    val orderNumber = saveOrderToRoom(cartItems)
                    if (orderNumber != null) {
                        val change = _uiState.value.receivedAmount - totalAmount
                        
                        // Handle printing after saving to Room
                        handlePrintingAndCashDrawer(
                            cartItems = cartItems,
                            cartAddonsMap = cartAddonsMap,
                            orderNumber = orderNumber,
                            receivedAmount = _uiState.value.receivedAmount,
                            change = change
                        )
                        
                        cartRepository.clearCart()
                        _uiState.update { it.copy(isProcessingOrder = false) }
                        onSuccess(change)
                    } else {
                        _uiState.update { it.copy(isProcessingOrder = false) }
                        onError("ไม่สามารถบันทึกออเดอร์ได้")
                    }
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
                            
                            // API error: Don't save to Room, just show error
                            _uiState.update { it.copy(isProcessingOrder = false) }
                            onError(displayMessage)
                            return@launch
                        }
                        
                        // API Success: Save locally with API response data
                        // Get cart addons map before clearing cart
                        val cartAddonsMap = cartItems.associate { item ->
                            item.id to cartRepository.getCartAddonsByItemId(item.id)
                        }
                        
                        val orderNumber = if (response.data != null) {
                            saveOrderToRoom(cartItems, response.data.orderNumber, response.data.id)
                        } else {
                            saveOrderToRoom(cartItems)
                        }
                        
                        if (orderNumber != null) {
                            val change = _uiState.value.receivedAmount - totalAmount
                            
                            // Handle printing after API success and saving to Room
                            handlePrintingAndCashDrawer(
                                cartItems = cartItems,
                                cartAddonsMap = cartAddonsMap,
                                orderNumber = orderNumber,
                                receivedAmount = _uiState.value.receivedAmount,
                                change = change
                            )
                            
                            cartRepository.clearCart()
                            _uiState.update { it.copy(isProcessingOrder = false) }
                            onSuccess(change)
                        } else {
                            _uiState.update { it.copy(isProcessingOrder = false) }
                            onError("ไม่สามารถบันทึกออเดอร์ได้")
                        }
                        
                    } catch (e: HttpException) {
                        // API error: Don't save to Room, show error
                        _uiState.update { it.copy(isProcessingOrder = false) }
                        onError("เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}")
                    } catch (e: Exception) {
                        // Network error: Don't save to Room, show error
                        _uiState.update { it.copy(isProcessingOrder = false) }
                        onError("เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessingOrder = false) }
                onError(e.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }
    
    private suspend fun saveOrderToRoom(
        cartItems: List<CartItemEntity>,
        serverOrderNumber: String? = null,
        serverOrderId: String? = null
    ): String? {
        return try {
            val now = Date()
            val orderId = serverOrderId ?: UUID.randomUUID().toString()
            val orderNumber = serverOrderNumber ?: generateOrderNumber()
            
            // Create order entity
            val orderEntity = OrderEntity(
                id = orderId,
                orderNumber = orderNumber,
                orderDate = now,
                subtotal = subtotal,
                discount = discount,
                total = totalAmount,
                paymentTypeRaw = PaymentType.CASH.code,
                statusRaw = OrderStatus.CONFIRMED.code,
                isDeletedLocally = false,
                isFromServer = serverOrderId != null,
                isSynced = serverOrderId != null,
                updatedAt = now,
                discountAmount = discount,
                discountPercentage = if (subtotal > 0.000001) (discount / subtotal) * 100.0 else 0.0,
                createdAt = now
            )
            
            // Save order
            orderDao.insertOrder(orderEntity)
            
            // Create order items and addons
            val orderItems = mutableListOf<OrderItemEntity>()
            val orderAddons = mutableListOf<OrderAddonEntity>()
            
            cartItems.forEach { cartItem ->
                val addons = _cartAddonsMap.value[cartItem.id] ?: emptyList()
                val product = if (cartItem.productId != null) {
                    productDao.getProductById(cartItem.productId)
                } else {
                    null
                }
                
                val itemTotalPrice = (cartItem.unitPrice ?: 0.0) * cartItem.quantity +
                    addons.sumOf { it.addonPrice } * cartItem.quantity
                
                // Convert addons to JSON
                val addonsJson = if (addons.isNotEmpty()) {
                    Gson().toJson(addons.map { 
                        mapOf(
                            "addonId" to it.addonId,
                            "addonName" to it.addonName,
                            "addonPrice" to it.addonPrice,
                            "addonGroupId" to it.addonGroupId
                        )
                    })
                } else {
                    null
                }
                
                val orderItemId = UUID.randomUUID().toString()
                val orderItem = OrderItemEntity(
                    id = orderItemId,
                    orderId = orderId,
                    productName = cartItem.productName ?: "",
                    productPrice = cartItem.unitPrice ?: 0.0,
                    productUnitPrice = cartItem.unitPrice ?: 0.0,
                    quantity = cartItem.quantity,
                    totalPrice = itemTotalPrice,
                    addons = addonsJson,
                    specialRequest = cartItem.specialRequest,
                    productId = cartItem.productId,
                    unitCost = product?.costPrice ?: 0.0,
                    createdAt = now
                )
                orderItems.add(orderItem)
                
                // Create order addons
                addons.forEach { addon ->
                    orderAddons.add(
                        OrderAddonEntity(
                            orderItemId = orderItemId,
                            addonId = addon.addonId,
                            addonName = addon.addonName,
                            addonPrice = addon.addonPrice,
                            quantity = 1
                        )
                    )
                }
            }
            
            // Save order items and addons
            if (orderItems.isNotEmpty()) {
                orderItemDao.insertOrderItems(orderItems)
            }
            if (orderAddons.isNotEmpty()) {
                orderAddonDao.insertOrderAddons(orderAddons)
            }
            
            orderNumber
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        return "ORD$dateStr${timestamp.toString().takeLast(6)}"
    }
    
    private fun formatNumberWithCommas(number: Double): String {
        val formatter = java.text.DecimalFormat("#,##0.00")
        return formatter.format(number)
    }
    
    /**
     * Handle printing receipt and opening cash drawer after order completion
     */
    private fun handlePrintingAndCashDrawer(
        cartItems: List<CartItemEntity>,
        cartAddonsMap: Map<String, List<CartAddonEntity>>,
        orderNumber: String,
        receivedAmount: Double,
        change: Double
    ) {
        viewModelScope.launch {
            try {
                val receiptSettings = receiptSettingsRepository.getReceiptSettingsSync()
                
                // Get shop name
                val shopName = authRepository.getCurrentUser().first()?.shopName
                    ?: "INDYPOS"
                
                val paymentType = DomainPaymentType.CASH
                
                // Check if cash drawer should be opened
                if (receiptSettings?.openCashDrawer == true) {
                    printerService.openCashDrawer()
                }
                
                // Check if receipt should be printed
                if (receiptSettings?.printAfterFinish == true) {
                    printerService.printOrderReceipt(
                        cartItems = cartItems,
                        cartAddonsMap = cartAddonsMap,
                        receiptSettings = receiptSettings,
                        shopName = shopName,
                        orderNumber = orderNumber,
                        subtotal = subtotal,
                        discount = discount,
                        total = totalAmount,
                        paymentType = paymentType,
                        receivedAmount = receivedAmount,
                        change = change
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Don't fail the order if printing fails
            }
        }
    }
}

