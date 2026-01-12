package com.indybrain.indypos_Android.presentation.orderproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.OrderAddonDao
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.data.local.dao.OrderItemDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.core.printer.PrinterService
import com.indybrain.indypos_Android.core.printer.LabelPrinterService
import com.indybrain.indypos_Android.core.printer.PrinterType
import com.indybrain.indypos_Android.data.local.dao.ReceiptSettingsDao
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.local.entity.OrderAddonEntity
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.dto.CreateOrderRequestDto
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.model.PaymentType as DomainPaymentType
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ReceiptSettingsRepository
import com.indybrain.indypos_Android.domain.repository.PrinterSettingsRepository
import com.indybrain.indypos_Android.domain.usecase.GetGroupedCartItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.indybrain.indypos_Android.R
import okhttp3.ResponseBody

@HiltViewModel
class OrderProductViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val ordersApi: OrdersApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val productDao: ProductDao,
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val orderAddonDao: OrderAddonDao,
    private val receiptSettingsDao: ReceiptSettingsDao,
    private val receiptSettingsRepository: ReceiptSettingsRepository,
    private val printerSettingsRepository: PrinterSettingsRepository,
    private val printerService: PrinterService,
    private val labelPrinterService: LabelPrinterService,
    private val authRepository: AuthRepository,
    private val getGroupedCartItemsUseCase: GetGroupedCartItemsUseCase,
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderProductUiState())
    val uiState: StateFlow<OrderProductUiState> = _uiState.asStateFlow()
    
    private val _cartAddonsMap = MutableStateFlow<Map<String, List<CartAddonEntity>>>(emptyMap())
    
    init {
        observeCartItems()
    }
    
    private fun observeCartItems() {
        viewModelScope.launch {
            // Observe grouped items for UI display
            getGroupedCartItemsUseCase().collect { groupedItems ->
                _uiState.update { currentState ->
                    // Preserve discount information when updating grouped items
                    currentState.copy(groupedItems = groupedItems)
                }
            }
        }
        
        viewModelScope.launch {
            // Also observe cart items for order creation and addons mapping
            cartRepository.getCartItems().collect { cartItems ->
                _uiState.update { currentState ->
                    // Preserve discount information when updating cart items
                    currentState.copy(cartItems = cartItems)
                }
                
                // Load addons for each cart item
                val addonsMap = mutableMapOf<String, List<CartAddonEntity>>()
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
    
    fun setDiscount(discountModel: com.indybrain.indypos_Android.presentation.discount.DiscountModel, subtotal: Double) {
        val discountAmount = when (discountModel.type) {
            com.indybrain.indypos_Android.presentation.discount.DiscountType.PERCENTAGE -> {
                (subtotal * discountModel.value / 100.0).coerceAtMost(subtotal)
            }
            com.indybrain.indypos_Android.presentation.discount.DiscountType.FIXED_AMOUNT -> {
                discountModel.value.coerceAtMost(subtotal)
            }
        }
        _uiState.update { 
            it.copy(
                discountAmount = discountAmount,
                discountType = discountModel.type,
                discountValue = discountModel.value
            ) 
        }
    }
    
    fun getCartAddons(itemId: String): List<CartAddonEntity> {
        return _cartAddonsMap.value[itemId] ?: emptyList()
    }
    
    fun calculateSubtotal(): Double {
        // Use grouped items for calculation to match ProductEditScreen
        return _uiState.value.groupedItems.sumOf { groupedItem ->
            val firstItem = groupedItem.items.firstOrNull() ?: return@sumOf 0.0
            val productPrice = firstItem.product.price * groupedItem.totalQuantity
            val addonsPrice = firstItem.selectedAddons.values
                .flatten()
                .sumOf { it.price } * groupedItem.totalQuantity
            productPrice + addonsPrice
        }
    }
    
    fun calculateTotal(): Double {
        val subtotal = calculateSubtotal()
        val discount = _uiState.value.discountAmount
        return (subtotal - discount).coerceAtLeast(0.0)
    }
    
    fun deleteCartItem(itemId: String) {
        viewModelScope.launch {
            cartRepository.deleteCartItem(itemId)
        }
    }
    
    fun clearAllCartItems() {
        viewModelScope.launch {
            cartRepository.clearCart()
            _uiState.update {
                it.copy(
                    discountAmount = 0.0,
                    discountType = null,
                    discountValue = 0.0
                )
            }
        }
    }
    
    fun placeOrder(
        onSuccess: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentState = _uiState.value
        
        // Prevent multiple taps
        if (currentState.isLoading) return
        
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                // Get cart items
                val cartItems = cartRepository.getCartItemsSync()
                
                // Build order request
                val items = buildOrderItems(cartItems)
                
                if (items.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false) }
                    onError("ไม่มีสินค้าในตะกร้า")
                    return@launch
                }
                
                val subtotal = calculateSubtotal()
                val discount = currentState.discountAmount
                val total = calculateTotal()
                
                val discountPercentage = if (subtotal > 0.000001) {
                    (discount / subtotal) * 100.0
                } else {
                    0.0
                }
                
                // Convert PaymentType enum to domain PaymentType code
                val paymentTypeCode = when (currentState.selectedPaymentType) {
                    PaymentType.CASH -> DomainPaymentType.CASH.code
                    PaymentType.TRANSFER -> DomainPaymentType.TRANSFER.code
                }
                
                val request = CreateOrderRequestDto(
                    customerName = "",
                    customerPhone = "",
                    customerEmail = "",
                    paymentType = paymentTypeCode,
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
                    val orderNumber = saveOrderToRoom(
                        cartItems = cartItems,
                        subtotal = subtotal,
                        discount = discount,
                        total = total,
                        paymentTypeCode = paymentTypeCode
                    )
                    
                    if (orderNumber != null) {
                        // Check receipt settings and print if enabled
                        handlePrintingAndCashDrawer(orderNumber, subtotal, discount, paymentTypeCode)
                        
                        cartRepository.clearCart()
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                discountAmount = 0.0,
                                discountType = null,
                                discountValue = 0.0
                            ) 
                        }
                        onSuccess(orderNumber)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        onError("ไม่สามารถบันทึกออเดอร์ได้")
                    }
                } else {
                    // Online: Try API first
                    try {
                        val response = ordersApi.createOrder(request)
                        
                        if (response.status == 403) {
                            val errorCode = response.error?.lowercase()
                            if (errorCode == "free_plan_limit_exceeded") {
                                _uiState.update { it.copy(isLoading = false) }
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
                                    context.getString(R.string.product_detail_insufficient_stock)
                                }
                                else -> errorMessage
                            }
                            
                            // API error: Don't save to Room, just show error
                            _uiState.update { it.copy(isLoading = false) }
                            onError(displayMessage)
                            return@launch
                        }
                        
                        // API Success: Save locally with API response data
                        val orderNumber = if (response.data != null) {
                            saveOrderToRoom(
                                cartItems = cartItems,
                                subtotal = subtotal,
                                discount = discount,
                                total = total,
                                paymentTypeCode = paymentTypeCode,
                                serverOrderNumber = response.data.orderNumber,
                                serverOrderId = response.data.id
                            )
                        } else {
                            saveOrderToRoom(
                                cartItems = cartItems,
                                subtotal = subtotal,
                                discount = discount,
                                total = total,
                                paymentTypeCode = paymentTypeCode
                            )
                        }
                        
                        if (orderNumber != null) {
                            // Check receipt settings and print if enabled
                            handlePrintingAndCashDrawer(orderNumber, subtotal, discount, paymentTypeCode)
                            
                            cartRepository.clearCart()
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    discountAmount = 0.0,
                                    discountType = null,
                                    discountValue = 0.0
                                ) 
                            }
                            onSuccess(orderNumber)
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                            onError("ไม่สามารถบันทึกออเดอร์ได้")
                        }
                        
                    } catch (e: HttpException) {
                        // API error: Don't save to Room, show error
                        _uiState.update { it.copy(isLoading = false) }
                        
                        // Check error message for insufficient stock
                        val errorMessage = parseErrorFromHttpException(e)
                        val lowercasedError = errorMessage.lowercase()
                        
                        val displayMessage = when {
                            lowercasedError.contains("insufficient stock") || 
                            lowercasedError.contains("at least one item is required") -> {
                                context.getString(R.string.product_detail_insufficient_stock)
                            }
                            else -> "เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message()}"
                        }
                        
                        onError(displayMessage)
                    } catch (e: Exception) {
                        // Network error: Don't save to Room, show error
                        _uiState.update { it.copy(isLoading = false) }
                        onError("เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                onError(e.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }
    
    private suspend fun saveOrderToRoom(
        cartItems: List<CartItemEntity>,
        subtotal: Double,
        discount: Double,
        total: Double,
        paymentTypeCode: Int,
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
                total = total,
                paymentTypeRaw = paymentTypeCode,
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
    
    private suspend fun handlePrintingAndCashDrawer(
        orderNumber: String?,
        subtotal: Double,
        discount: Double,
        paymentTypeCode: Int
    ) {
        try {
            val receiptSettings = receiptSettingsRepository.getReceiptSettingsSync()
            val labelPrinterSettings = printerSettingsRepository.getPrinterSettingsSync(PrinterType.LABEL)
            
            // Get shop name
            val shopName = authRepository.getCurrentUser().first()?.shopName
                ?: "INDYPOS"
            
            // Get cart items and addons for printing
            val cartItems = cartRepository.getCartItemsSync()
            val cartAddonsMap = cartItems.associate { item ->
                item.id to cartRepository.getCartAddonsByItemId(item.id)
            }
            
            val total = calculateTotal()
            val paymentType = DomainPaymentType.fromCode(paymentTypeCode)
            
            // Check if cash drawer should be opened
            if (receiptSettings?.openCashDrawer == true) {
                printerService.openCashDrawer()
            }
            
            // 1. Check if receipt should be printed
            if (receiptSettings?.printAfterFinish == true) {
                printerService.printOrderReceipt(
                    cartItems = cartItems,
                    cartAddonsMap = cartAddonsMap,
                    receiptSettings = receiptSettings,
                    shopName = shopName,
                    orderNumber = orderNumber,
                    subtotal = subtotal,
                    discount = discount,
                    total = total,
                    paymentType = paymentType,
                    receivedAmount = null, // Not available for transfer payment
                    change = null // Not available for transfer payment
                )
            }
            
            // 2. Check if label printer is enabled and print stickers
            if (labelPrinterSettings?.enabled == true) {
                // Print labels for each item based on quantity
                val printSuccess = labelPrinterService.printLabels(
                    cartItems = cartItems,
                    cartAddonsMap = cartAddonsMap,
                    shopName = shopName
                )
                
                if (!printSuccess) {
                    // Log error but don't fail the order
                    android.util.Log.e("OrderProductViewModel", "Failed to print some labels")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't fail the order if printing fails
        }
    }
    
    /**
     * Parse error message from HttpException
     */
    private fun parseErrorFromHttpException(e: HttpException): String {
        return try {
            val errorBody: ResponseBody? = e.response()?.errorBody()
            if (errorBody != null) {
                val errorJson = errorBody.string()
                if (errorJson.isNotBlank()) {
                    try {
                        val errorResponse = gson.fromJson(errorJson, ErrorResponse::class.java)
                        errorResponse.error ?: errorResponse.message ?: e.message() ?: "เกิดข้อผิดพลาด"
                    } catch (parseException: Exception) {
                        errorJson
                    }
                } else {
                    e.message() ?: "เกิดข้อผิดพลาด"
                }
            } else {
                e.message() ?: "เกิดข้อผิดพลาด"
            }
        } catch (exception: Exception) {
            e.message() ?: "เกิดข้อผิดพลาด"
        }
    }
    
    private fun generateOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        return "ORD$dateStr${timestamp.toString().takeLast(6)}"
    }
}

/**
 * Error response DTO for parsing API errors
 */
private data class ErrorResponse(
    val error: String?,
    val message: String?
)






