package com.indybrain.indypos_Android.presentation.cashpayment

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.core.notification.StockNotificationHelper
import com.indybrain.indypos_Android.core.printer.PrinterService
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.dto.CreateOrderRequestDto
import com.indybrain.indypos_Android.domain.model.PaymentType
import com.indybrain.indypos_Android.domain.model.PaymentType as DomainPaymentType
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.domain.repository.ReceiptSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CashPaymentViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val ordersApi: OrdersApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val productRepository: ProductRepository,
    private val stockNotificationHelper: StockNotificationHelper,
    private val receiptSettingsRepository: ReceiptSettingsRepository,
    private val printerService: PrinterService,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    private val gson: Gson
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
            totalAmount
        } else {
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

        if (currentState.isProcessingOrder) return

        val tolerance = 0.01
        if (currentState.receivedAmount < (totalAmount - tolerance)) {
            onError("จำนวนเงินที่รับมาไม่เพียงพอ")
            return
        }

        completePayment(onSuccess, onError)
    }

    private fun completePayment(onSuccess: (Double) -> Unit, onError: (String) -> Unit) {
        _uiState.update { it.copy(isProcessingOrder = true) }

        viewModelScope.launch {
            try {
                val cartItems = cartRepository.getCartItemsSync()
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

                if (!networkConnectivityChecker.isConnected()) {
                    _uiState.update { it.copy(isProcessingOrder = false) }
                    onError(context.getString(R.string.logout_no_internet_title))
                    return@launch
                }

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
                                context.getString(R.string.product_detail_insufficient_stock)
                            }
                            else -> errorMessage
                        }

                        _uiState.update { it.copy(isProcessingOrder = false) }
                        onError(displayMessage)
                        return@launch
                    }

                    val orderNumber =
                        response.data?.orderNumber?.takeIf { it.isNotBlank() }
                            ?: generateOrderNumber()
                    val change = _uiState.value.receivedAmount - totalAmount
                    val cartAddonsMap = cartItems.associate { item ->
                        item.id to cartRepository.getCartAddonsByItemId(item.id)
                    }

                    handlePrintingAndCashDrawer(
                        cartItems = cartItems,
                        cartAddonsMap = cartAddonsMap,
                        orderNumber = orderNumber,
                        receivedAmount = _uiState.value.receivedAmount,
                        change = change
                    )

                    checkLowStockUsingFreshApiStock(cartItems)

                    cartRepository.clearCart()
                    _uiState.update { it.copy(isProcessingOrder = false) }
                    onSuccess(change)
                } catch (e: HttpException) {
                    _uiState.update { it.copy(isProcessingOrder = false) }

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
                    _uiState.update { it.copy(isProcessingOrder = false) }
                    onError("เกิดข้อผิดพลาดในการเชื่อมต่อ: ${e.message}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessingOrder = false) }
                onError(e.message ?: "เกิดข้อผิดพลาด")
            }
        }
    }

    private fun buildOrderItems(cartItems: List<CartItemEntity>): List<CreateOrderRequestDto.OrderItemDto> {
        return cartItems.map { cartItem ->
            val addons = _cartAddonsMap.value[cartItem.id] ?: emptyList()
            val unitCost = 0.0

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
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        return "ORD$dateStr${timestamp.toString().takeLast(6)}"
    }

    private fun formatNumberWithCommas(number: Double): String {
        val formatter = DecimalFormat("#,##0.00")
        return formatter.format(number)
    }

    private suspend fun handlePrintingAndCashDrawer(
        cartItems: List<CartItemEntity>,
        cartAddonsMap: Map<String, List<CartAddonEntity>>,
        orderNumber: String,
        receivedAmount: Double,
        change: Double
    ) {
        try {
            val receiptSettings = receiptSettingsRepository.getReceiptSettingsSync()
            val shopName = authRepository.getCurrentUser().first()?.shopName
                ?: "INDYPOS"
            val paymentType = DomainPaymentType.CASH

            if (receiptSettings?.openCashDrawer == true) {
                printerService.openCashDrawer()
            }

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
        }
    }

    private fun checkLowStockUsingFreshApiStock(cartItems: List<CartItemEntity>) {
        viewModelScope.launch {
            try {
                val orderedIds = cartItems.mapNotNull { it.productId }.toSet()
                if (orderedIds.isEmpty()) return@launch
                productRepository.getProductListFromApi().onSuccess { data ->
                    val updated = data.products.filter { it.id in orderedIds }
                    stockNotificationHelper.checkAndNotifyLowStock(updated, emptyMap())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseErrorFromHttpException(e: HttpException): String {
        return try {
            val errorBody: ResponseBody? = e.response()?.errorBody()
            if (errorBody != null) {
                val errorJson = errorBody.string()
                if (errorJson.isNotBlank()) {
                    try {
                        val errorResponse = gson.fromJson(errorJson, ErrorResponse::class.java)
                        errorResponse.error ?: errorResponse.message ?: e.message() ?: "เกิดข้อผิดพลาด"
                    } catch (_: Exception) {
                        errorJson
                    }
                } else {
                    e.message() ?: "เกิดข้อผิดพลาด"
                }
            } else {
                e.message() ?: "เกิดข้อผิดพลาด"
            }
        } catch (_: Exception) {
            e.message() ?: "เกิดข้อผิดพลาด"
        }
    }
}

private data class ErrorResponse(
    val error: String?,
    val message: String?
)
