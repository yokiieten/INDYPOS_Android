package com.indybrain.indypos_Android.presentation.orderdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.data.remote.dto.OrderAddonDto
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.model.PaymentType
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date
import java.util.TimeZone

@Composable
fun OrderDetailScreen(
    orderId: String,
    onBackClick: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null && !uiState.isCancelling) {
            errorMessage = uiState.errorMessage ?: ""
            showErrorDialog = true
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "กลับ",
                        tint = PrimaryText
                    )
                }
                Text(
                    text = stringResource(id = R.string.order_detail_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryButton)
                }
            }
            uiState.order == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "ไม่พบออเดอร์",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                }
            }
            else -> {
                OrderDetailContent(
                    order = uiState.order!!,
                    orderItems = uiState.orderItems,
                    onCancelClick = {
                        showCancelDialog = true
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
        
        // Cancel Confirmation Dialog
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.order_cancel_confirm_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.order_cancel_confirm_message),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelDialog = false
                            viewModel.cancelOrder(
                                orderId = orderId,
                                onSuccess = {
                                    showSuccessDialog = true
                                },
                                onError = { error ->
                                    errorMessage = error
                                    showErrorDialog = true
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedFailure
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_cancel_yes),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text(
                            text = stringResource(id = R.string.order_cancel_no),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                    }
                }
            )
        }
        
        // Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSuccessDialog = false
                    onBackClick()
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.success_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.order_cancel_success),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onBackClick()
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.dialog_button_ok),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = Color.White
                        )
                    }
                }
            )
        }
        
        // Error Dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.common_error),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showErrorDialog = false }
                    ) {
                        Text(
                            text = stringResource(id = R.string.dialog_button_ok),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = Color.White
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: com.indybrain.indypos_Android.data.local.entity.OrderEntity,
    orderItems: List<OrderItemEntity>,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = OrderStatus.fromCode(order.statusRaw)
    val paymentType = PaymentType.fromCode(order.paymentTypeRaw)
    val canCancel = status != OrderStatus.CANCELLED && status != OrderStatus.DELIVERED
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Order Info Card
        OrderInfoCard(
            order = order,
            status = status,
            paymentType = paymentType
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Menu Items Section
        Text(
            text = stringResource(id = R.string.order_menu),
            style = FontUtils.mainFont(
                style = AppFontStyle.Bold,
                size = FontSize.Large
            ),
            color = PrimaryText
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Order Items
        if (orderItems.isEmpty()) {
            Text(
                text = "ไม่มีรายการสินค้า",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            orderItems.sortedBy { it.productName }.forEach { item ->
                OrderItemCard(item = item)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Summary Card
        SummaryCard(
            subtotal = order.subtotal,
            discount = order.discount,
            total = order.total
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Buttons
        if (canCancel) {
            TextButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.order_cancel),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = RedFailure
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OrderInfoCard(
    order: com.indybrain.indypos_Android.data.local.entity.OrderEntity,
    status: OrderStatus,
    paymentType: PaymentType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Order Number and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = order.orderNumber,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    modifier = Modifier.weight(1f)
                )
                
                // Status Badge
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (status) {
                                OrderStatus.CONFIRMED,
                                OrderStatus.PREPARING,
                                OrderStatus.READY,
                                OrderStatus.DELIVERED -> GreenComplete
                                OrderStatus.CANCELLED -> RedFailure
                                OrderStatus.DRAFT -> Color(0xFFFF9800) // Orange
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getStatusText(status),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Medium
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Order Date
            Text(
                text = formatDate(order.orderDate),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Payment Type
            Text(
                text = "${stringResource(id = R.string.order_payment_method)}: ${getPaymentTypeText(paymentType)}",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            // Cancel Date (if cancelled)
            if (status == OrderStatus.CANCELLED && order.updatedAt != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(id = R.string.order_cancel_date)}: ${formatDate(order.updatedAt)}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = RedFailure
                )
            }
        }
    }
}

@Composable
private fun OrderItemCard(item: OrderItemEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.productName,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${stringResource(id = R.string.order_detail_quantity_unit).replace("{quantity}", item.quantity.toString())}",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Small
                        ),
                        color = SecondaryText
                    )
                    
                    // Addons
                    val addonsText = formatAddons(item.addons)
                    if (addonsText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = addonsText,
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Small
                            ),
                            color = SecondaryText
                        )
                    }
                    
                    // Special Request
                    if (!item.specialRequest.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(id = R.string.order_detail_note).replace("{note}", item.specialRequest)}",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Small
                            ),
                            color = Color(0xFFFF9800) // Orange
                        )
                    }
                }
                
                Text(
                    text = formatCurrency(item.totalPrice),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    subtotal: Double,
    discount: Double,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Subtotal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.order_subtotal),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                Text(
                    text = formatCurrency(subtotal),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            }
            
            // Discount (if > 0)
            if (discount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.order_discount),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                    Text(
                        text = "-${formatCurrency(discount)}",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = RedFailure
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(if (discount > 0) 12.dp else 20.dp))
            
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.order_total_net),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
                Text(
                    text = formatCurrency(total),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
            }
        }
    }
}

@Composable
private fun formatDate(date: Date): String {
    val context = LocalContext.current
    val locale = LocaleHelper.getCurrentLocale(context)
    val isEnglish = locale.language == "en"

    val bangkokTimeZone = TimeZone.getTimeZone("Asia/Bangkok")
    val calendar = Calendar.getInstance(bangkokTimeZone)
    calendar.timeInMillis = date.time

    return if (isEnglish) {
        // English format: "Dec 11, 2025, 11:15" (24-hour, no AM/PM)
        val dateFormat = SimpleDateFormat("MMM d, yyyy, HH:mm", locale)
        dateFormat.timeZone = bangkokTimeZone
        dateFormat.format(date)
    } else {
        // Thai format: "11 ธ.ค. 68, 11:15"
        val year = calendar.get(Calendar.YEAR) + 543
        val monthFormat = SimpleDateFormat("MMM", locale)
        monthFormat.timeZone = bangkokTimeZone
        val monthStr = monthFormat.format(date)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val shortYear = year % 100
        "$day $monthStr $shortYear, ${String.format(locale, "%02d:%02d", hour, minute)}"
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

@Composable
private fun getStatusText(status: OrderStatus): String {
    return when (status) {
        OrderStatus.DRAFT -> stringResource(id = R.string.order_status_draft)
        OrderStatus.CONFIRMED -> stringResource(id = R.string.order_status_confirmed)
        OrderStatus.PREPARING -> stringResource(id = R.string.order_status_preparing)
        OrderStatus.READY -> stringResource(id = R.string.order_status_ready)
        OrderStatus.DELIVERED -> stringResource(id = R.string.order_status_delivered)
        OrderStatus.CANCELLED -> stringResource(id = R.string.order_status_cancelled)
    }
}

@Composable
private fun getPaymentTypeText(paymentType: PaymentType): String {
    return when (paymentType) {
        PaymentType.CASH -> stringResource(id = R.string.payment_type_cash)
        PaymentType.TRANSFER -> stringResource(id = R.string.payment_type_transfer)
        PaymentType.CARD -> stringResource(id = R.string.payment_type_card)
        PaymentType.QR_CODE -> stringResource(id = R.string.payment_type_qr_code)
    }
}

private fun formatAddons(addonsJson: String?): String {
    if (addonsJson.isNullOrEmpty()) return ""
    
    return try {
        val gson = Gson()
        val listType = object : TypeToken<List<OrderAddonDto>>() {}.type
        val addons: List<OrderAddonDto> = gson.fromJson(addonsJson, listType)
        
        if (addons.isEmpty()) return ""
        
        // Group by addon name and sum quantities (filter out entries with null name from stale cache)
        val grouped = addons.filter { !it.addonName.isNullOrEmpty() }
            .groupBy { it.addonName }
            .mapValues { (_, list) -> list.sumOf { it.quantity } }

        if (grouped.isEmpty()) return ""

        grouped.map { (name, quantity) ->
            if (quantity > 1) "$name x$quantity" else name ?: ""
        }.joinToString(", ")
    } catch (e: Exception) {
        // If parsing fails, try to return as-is (might be formatted string)
        addonsJson
    }
}


