package com.indybrain.indypos_Android.presentation.orderproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.core.notification.StockNotificationHelper
import com.indybrain.indypos_Android.core.printer.PrinterService
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.dto.CreateOrderRequestDto
import com.indybrain.indypos_Android.domain.model.PaymentType as DomainPaymentType
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.domain.repository.ReceiptSettingsRepository
import com.indybrain.indypos_Android.domain.usecase.GetGroupedCartItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private val productRepository: ProductRepository,
    private val stockNotificationHelper: StockNotificationHelper,
    private val receiptSettingsRepository: ReceiptSettingsRepository,
    private val printerService: PrinterService,
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

    /**
     * Same as Main Product flow: GET product list then [CartRepository.syncCartRelatedCatalogFromPosList]
     * (upsert catalog for cart lines, refresh addons from detail API when needed, refresh line snapshots).
     */
    fun syncCartCatalogFromPosList() {
        viewModelScope.launch {
            productRepository.getProductListFromApi()
                .onSuccess { data ->
                    cartRepository.syncCartRelatedCatalogFromPosList(
                        data.categories,
                        data.products
                    )
                }
        }
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
                
                if (!networkConnectivityChecker.isConnected()) {
                    _uiState.update { it.copy(isLoading = false) }
                    onError(context.getString(R.string.logout_no_internet_title))
                    return@launch
                }

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

                        _uiState.update { it.copy(isLoading = false) }
                        onError(displayMessage)
                        return@launch
                    }

                    val orderNumber =
                        response.data?.orderNumber?.takeIf { it.isNotBlank() }
                            ?: generateOrderNumber()

                    handlePrintingAndCashDrawer(
                        orderNumber,
                        subtotal,
                        discount,
                        paymentTypeCode
                    )

                    checkLowStockUsingFreshApiStock(cartItems)

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
                } catch (e: HttpException) {
                    _uiState.update { it.copy(isLoading = false) }

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
                    _uiState.update { it.copy(isLoading = false) }
                    onError("เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}")
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                onError(e.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }
    
    private fun buildOrderItems(cartItems: List<CartItemEntity>): List<CreateOrderRequestDto.OrderItemDto> {
        return cartItems.map { cartItem ->
            val addons = _cartAddonsMap.value[cartItem.id] ?: emptyList()

            val unitCost = 0.0
            
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
    
    /**
     * ดึงรายการสินค้าล่าสุดจาก API หลังสร้างออเดอร์สำเร็จ แล้วเช็ค low stock
     * (ค่า stock จาก API เป็นยอดหลังตัดแล้ว — ใช้ [StockNotificationHelper.checkAndNotifyLowStock] กับ orderedItems ว่าง
     * เพื่อไม่หักจำนวนที่สั่งซ้ำ)
     */
    /**
     * Same as [com.indybrain.indypos_Android.presentation.cashpayment.CashPaymentViewModel]:
     * nested [viewModelScope.launch] was cancelled when leaving this screen after order success,
     * so stock notifications never appeared.
     */
    private suspend fun checkLowStockUsingFreshApiStock(cartItems: List<CartItemEntity>) {
        try {
            val orderedIds = cartItems.mapNotNull { it.productId }.toSet()
            if (orderedIds.isEmpty()) return
            productRepository.getProductListFromApi().onSuccess { data ->
                val updated = data.products.filter { it.id in orderedIds }
                stockNotificationHelper.checkAndNotifyLowStock(updated, emptyMap())
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                    total = total,
                    paymentType = paymentType,
                    receivedAmount = null, // Not available for transfer payment
                    change = null // Not available for transfer payment
                )
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






