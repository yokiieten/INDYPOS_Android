package com.indybrain.indypos_Android.presentation.orderproduct

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
import com.indybrain.indypos_Android.domain.model.GroupedCartItem
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderProductScreen(
    onBackClick: () -> Unit = {},
    onAddMenuClick: () -> Unit = {},
    onEditItemClick: (productId: String, productName: String, cartItemIds: List<String>) -> Unit = { _, _, _ -> },
    onDiscountClick: () -> Unit = {},
    onPlaceOrderClick: (totalAmount: Double, subtotal: Double, discount: Double) -> Unit = { _, _, _ -> },
    onOrderSuccess: (totalAmount: Double) -> Unit = { _ -> },
    viewModel: OrderProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var itemToDelete by remember { mutableStateOf<GroupedCartItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ออเดอร์",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "กลับ",
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BaseBackground,
                    titleContentColor = PrimaryText
                )
            )
        }
    ) { padding ->
        if (isLandscape) {
            // Landscape Layout - Split screen
            LandscapeOrderContent(
                padding = padding,
                uiState = uiState,
                viewModel = viewModel,
                onAddMenuClick = onAddMenuClick,
                onEditItemClick = onEditItemClick,
                onDeleteItem = { itemToDelete = it },
                onDiscountClick = onDiscountClick,
                onPlaceOrderClick = onPlaceOrderClick,
                onOrderSuccess = onOrderSuccess,
                onClearAllClick = { showClearAllDialog = true },
                onError = { errorMessage = it }
            )
        } else {
            // Portrait Layout - Original vertical layout
            PortraitOrderContent(
                padding = padding,
                uiState = uiState,
                viewModel = viewModel,
                onAddMenuClick = onAddMenuClick,
                onEditItemClick = onEditItemClick,
                onDeleteItem = { itemToDelete = it },
                onDiscountClick = onDiscountClick,
                onPlaceOrderClick = onPlaceOrderClick,
                onOrderSuccess = onOrderSuccess,
                onClearAllClick = { showClearAllDialog = true },
                onError = { errorMessage = it }
            )
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "ลบสินค้า",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = "ต้องการลบสินค้านี้ออกจากตะกร้าหรือไม่?",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { group ->
                            // Delete all items in the group
                            group.items.forEach { item ->
                                viewModel.deleteCartItem(item.id)
                            }
                        }
                        itemToDelete = null
                    }
                ) {
                    Text(
                        text = "ลบ",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(
                        text = "ยกเลิก",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                }
            }
        )
    }
    
    // Error dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = {
                Text(
                    text = "เกิดข้อผิดพลาด",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = errorMessage ?: "เกิดข้อผิดพลาด",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text(
                        text = "ตกลง",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
        )
    }

    // Clear all confirmation dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = "ลบรายการทั้งหมด",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = "ต้องการลบสินค้าออกจากออเดอร์ทั้งหมดหรือไม่?",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllDialog = false
                        viewModel.clearAllCartItems()
                    }
                ) {
                    Text(
                        text = "ลบทั้งหมด",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = Color(0xFFFF5252)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text(
                        text = "ยกเลิก",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                }
            }
        )
    }
}

@Composable
private fun LandscapeOrderContent(
    padding: PaddingValues,
    uiState: OrderProductUiState,
    viewModel: OrderProductViewModel,
    onAddMenuClick: () -> Unit,
    onEditItemClick: (productId: String, productName: String, cartItemIds: List<String>) -> Unit,
    onDeleteItem: (GroupedCartItem) -> Unit,
    onDiscountClick: () -> Unit,
    onPlaceOrderClick: (totalAmount: Double, subtotal: Double, discount: Double) -> Unit,
    onOrderSuccess: (totalAmount: Double) -> Unit,
    onClearAllClick: () -> Unit,
    onError: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Left side - Cart Items List (60% width)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxSize()
        ) {
            // Header section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ออเดอร์ของฉัน",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.groupedItems.isNotEmpty()) {
                        TextButton(onClick = onClearAllClick) {
                            Text(
                                text = stringResource(R.string.order_product_clear_all),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Small
                                ),
                                color = Color(0xFFFF5252)
                            )
                        }
                    }
                    
                    TextButton(onClick = onAddMenuClick) {
                        Text(
                            text = stringResource(R.string.order_product_add_product),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryButton
                        )
                    }
                }
            }
            
            // Cart items list
            if (uiState.groupedItems.isEmpty()) {
                EmptyCartContent(
                    modifier = Modifier.weight(1f),
                    onAddMenuClick = onAddMenuClick
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.groupedItems,
                        key = { it.key }
                    ) { groupedItem ->
                        SwipeToDeleteCartItem(
                            onDelete = { onDeleteItem(groupedItem) }
                        ) {
                            GroupedCartItemRow(
                                groupedItem = groupedItem,
                                onEditClick = { 
                                    val productId = groupedItem.items.firstOrNull()?.product?.id
                                    val productName = groupedItem.items.firstOrNull()?.product?.name ?: ""
                                    if (productId != null) {
                                        val cartItemIds = groupedItem.items.map { it.id }
                                        onEditItemClick(productId, productName, cartItemIds)
                                    }
                                },
                                onDeleteClick = { onDeleteItem(groupedItem) }
                            )
                        }
                    }
                }
            }
        }
        
        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxSize()
                .background(Color(0xFFE0E0E0))
        )
        
        // Right side - Payment & Summary (40% width)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxSize()
        ) {
            if (uiState.groupedItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Payment type section
                    Text(
                        text = "ประเภทการจ่ายเงิน",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PaymentTypeButton(
                            paymentType = PaymentType.CASH,
                            isSelected = uiState.selectedPaymentType == PaymentType.CASH,
                            onClick = { viewModel.selectPaymentType(PaymentType.CASH) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        PaymentTypeButton(
                            paymentType = PaymentType.TRANSFER,
                            isSelected = uiState.selectedPaymentType == PaymentType.TRANSFER,
                            onClick = { viewModel.selectPaymentType(PaymentType.TRANSFER) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE0E0E0))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Discount section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ส่วนลด",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                        
                        if (uiState.discountAmount > 0 && uiState.discountType != null) {
                            val discountText = when (val discountType = uiState.discountType) {
                                com.indybrain.indypos_Android.presentation.discount.DiscountType.PERCENTAGE -> {
                                    "${uiState.discountValue.toInt()}%"
                                }
                                com.indybrain.indypos_Android.presentation.discount.DiscountType.FIXED_AMOUNT -> {
                                    formatCurrency(uiState.discountValue)
                                }
                                null -> ""
                            }
                            TextButton(
                                onClick = onDiscountClick,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = discountText,
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Medium
                                    ),
                                    color = PrimaryButton
                                )
                            }
                        } else {
                            TextButton(onClick = onDiscountClick) {
                                Text(
                                    text = "เพิ่ม (ถ้ามี)",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Small
                                    ),
                                    color = PrimaryButton
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Total section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ยอดรวมราคา",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                        
                        Text(
                            text = formatCurrency(viewModel.calculateSubtotal()),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                    }
                    
                    if (uiState.discountAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ส่วนลด",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PrimaryText
                            )
                            
                            Text(
                                text = "-${formatCurrency(uiState.discountAmount)}",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PrimaryButton
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "รวม",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Large
                            ),
                            color = PrimaryText
                        )
                        
                        Text(
                            text = formatCurrency(viewModel.calculateTotal()),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Large
                            ),
                            color = PrimaryButton
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Order button
                    val totalItemCount = uiState.groupedItems.sumOf { it.totalQuantity }
                    OrderButton(
                        itemCount = totalItemCount,
                        totalAmount = viewModel.calculateTotal(),
                        onClick = {
                            val total = viewModel.calculateTotal()
                            val subtotal = viewModel.calculateSubtotal()
                            val discount = uiState.discountAmount
                            
                            if (uiState.selectedPaymentType == PaymentType.CASH) {
                                onPlaceOrderClick(total, subtotal, discount)
                            } else {
                                viewModel.placeOrder(
                                    onSuccess = { orderNumber ->
                                        // สำหรับการชำระเงินด้วยการโอนเงิน ให้แสดงเงินทอนเป็น 0 บาท
                                        onOrderSuccess(0.0)
                                    },
                                    onError = { error ->
                                        onError(error)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitOrderContent(
    padding: PaddingValues,
    uiState: OrderProductUiState,
    viewModel: OrderProductViewModel,
    onAddMenuClick: () -> Unit,
    onEditItemClick: (productId: String, productName: String, cartItemIds: List<String>) -> Unit,
    onDeleteItem: (GroupedCartItem) -> Unit,
    onDiscountClick: () -> Unit,
    onPlaceOrderClick: (totalAmount: Double, subtotal: Double, discount: Double) -> Unit,
    onOrderSuccess: (totalAmount: Double) -> Unit,
    onClearAllClick: () -> Unit,
    onError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Header section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ออเดอร์ของฉัน",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.groupedItems.isNotEmpty()) {
                    TextButton(onClick = onClearAllClick) {
                        Text(
                            text = stringResource(R.string.order_product_clear_all),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = Color(0xFFFF5252)
                        )
                    }
                }
                
                TextButton(onClick = onAddMenuClick) {
                    Text(
                        text = stringResource(R.string.order_product_add_product),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
        }
        
        // Cart items list
        if (uiState.groupedItems.isEmpty()) {
            EmptyCartContent(
                modifier = Modifier.weight(1f),
                onAddMenuClick = onAddMenuClick
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = uiState.groupedItems,
                    key = { it.key }
                ) { groupedItem ->
                    SwipeToDeleteCartItem(
                        onDelete = { onDeleteItem(groupedItem) }
                    ) {
                        GroupedCartItemRow(
                            groupedItem = groupedItem,
                            onEditClick = { 
                                val productId = groupedItem.items.firstOrNull()?.product?.id
                                val productName = groupedItem.items.firstOrNull()?.product?.name ?: ""
                                if (productId != null) {
                                    val cartItemIds = groupedItem.items.map { it.id }
                                    onEditItemClick(productId, productName, cartItemIds)
                                }
                            },
                            onDeleteClick = { onDeleteItem(groupedItem) }
                        )
                    }
                }
                
                // Divider
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFE0E0E0))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Payment type section
                item {
                    Text(
                        text = "ประเภทการจ่ายเงิน",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PaymentTypeButton(
                            paymentType = PaymentType.CASH,
                            isSelected = uiState.selectedPaymentType == PaymentType.CASH,
                            onClick = { viewModel.selectPaymentType(PaymentType.CASH) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        PaymentTypeButton(
                            paymentType = PaymentType.TRANSFER,
                            isSelected = uiState.selectedPaymentType == PaymentType.TRANSFER,
                            onClick = { viewModel.selectPaymentType(PaymentType.TRANSFER) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Discount section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ส่วนลด",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                        
                        if (uiState.discountAmount > 0 && uiState.discountType != null) {
                            val discountText = when (val discountType = uiState.discountType) {
                                com.indybrain.indypos_Android.presentation.discount.DiscountType.PERCENTAGE -> {
                                    "${uiState.discountValue.toInt()}%"
                                }
                                com.indybrain.indypos_Android.presentation.discount.DiscountType.FIXED_AMOUNT -> {
                                    formatCurrency(uiState.discountValue)
                                }
                                null -> ""
                            }
                            TextButton(
                                onClick = onDiscountClick,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = discountText,
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Medium
                                    ),
                                    color = PrimaryButton
                                )
                            }
                        } else {
                            TextButton(onClick = onDiscountClick) {
                                Text(
                                    text = "เพิ่ม (ถ้ามี)",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Small
                                    ),
                                    color = PrimaryButton
                                )
                            }
                        }
                    }
                }
                
                // Total section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ยอดรวมราคา",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                        
                        Text(
                            text = formatCurrency(viewModel.calculateSubtotal()),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                    }
                    
                    if (uiState.discountAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ส่วนลด",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PrimaryText
                            )
                            
                            Text(
                                text = "-${formatCurrency(uiState.discountAmount)}",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PrimaryButton
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "รวม",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                        
                        Text(
                            text = formatCurrency(viewModel.calculateTotal()),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                    }
                }
                
                // Bottom spacing for order button
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Order button (fixed at bottom)
        if (uiState.groupedItems.isNotEmpty()) {
            val totalItemCount = uiState.groupedItems.sumOf { it.totalQuantity }
            OrderButton(
                itemCount = totalItemCount,
                totalAmount = viewModel.calculateTotal(),
                onClick = {
                    val total = viewModel.calculateTotal()
                    val subtotal = viewModel.calculateSubtotal()
                    val discount = uiState.discountAmount
                    
                    if (uiState.selectedPaymentType == PaymentType.CASH) {
                        onPlaceOrderClick(total, subtotal, discount)
                    } else {
                        viewModel.placeOrder(
                            onSuccess = { orderNumber ->
                                // สำหรับการชำระเงินด้วยการโอนเงิน ให้แสดงเงินทอนเป็น 0 บาทเสมอ
                                onOrderSuccess(0.0)
                            },
                            onError = { error ->
                                onError(error)
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun SwipeToDeleteCartItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidthDp = 96.dp
    val maxOffsetPx = with(density) { -actionWidthDp.toPx() } // เลื่อนได้สุดเท่าความกว้างปุ่มลบ

    var offsetX by remember { mutableStateOf(0f) } // px

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // ปล่อยนิ้วแล้ว ถ้าลากเกินครึ่งหนึ่งของปุ่มลบ ให้ค้างเปิดไว้
                        if (offsetX < maxOffsetPx / 2f) {
                            offsetX = maxOffsetPx
                        } else {
                            offsetX = 0f
                        }
                    }
                ) { _, dragAmount ->
                    // เลื่อนจากขวาไปซ้ายเท่านั้น
                    val newOffset = offsetX + dragAmount
                    offsetX = newOffset.coerceIn(maxOffsetPx, 0f)
                }
            }
    ) {
        // พื้นหลังปุ่มลบสีแดง (แบบ iOS) อยู่ด้านหลัง content
        // แสดงเฉพาะตอนที่มีการลากให้ offsetX < 0 เท่านั้น
        if (offsetX < 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    modifier = Modifier
                        .width(actionWidthDp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDelete()
                        },
                    color = Color(0xFFFF5252)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "ลบ",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ลบ",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Small
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }

        // เนื้อหาหลัก เลื่อนไปตาม offsetX
        Box(
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

@Composable
private fun GroupedCartItemRow(
    groupedItem: GroupedCartItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val firstItem = groupedItem.items.firstOrNull() ?: return
    val product = firstItem.product
    
    // Calculate total price for the grouped item
    val totalPrice = calculateGroupedItemPrice(firstItem, groupedItem.totalQuantity)
    
    // Get product color for background
    val backgroundColor = product.selectedColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            Color(0xFFE0E0E0)
        }
    } ?: Color(0xFFE0E0E0)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Product image with quantity badge
        Box {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
            ) {
                val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
                val hasColor = product.selectedColorHex != null && product.selectedColorHex.isNotBlank()
                
                if (hasColor) {
                    // If product has color, just show the color background (no image, no default)
                    // The background color is already set in the Box modifier above
                } else if (!imageUrl.isNullOrBlank()) {
                    // If no color but has image, load product image
                    val fullImageUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                        imageUrl
                    } else {
                        AppConfig.buildImageUrl(imageUrl)
                    }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fullImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.logo_appstore),
                        placeholder = painterResource(id = R.drawable.logo_appstore)
                    )
                } else {
                    // If neither color nor image, show default image
                    Image(
                        painter = painterResource(id = R.drawable.logo_appstore),
                        contentDescription = product.name,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Quantity badge showing total quantity
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = groupedItem.totalQuantity.toString(),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Small
                    ),
                    color = Color.White
                )
            }
        }
        
        // Product details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = product.name,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Addons (customizations)
            if (firstItem.selectedAddons.isNotEmpty()) {
                val addonNames = firstItem.selectedAddons.values
                    .flatten()
                    .map { it.name }
                Text(
                    text = addonNames.joinToString(", "),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Special request
            if (!firstItem.specialRequest.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "หมายเหตุ: ${firstItem.specialRequest}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            TextButton(
                onClick = onEditClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "แก้ไข",
                    tint = PrimaryButton,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Edit",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = PrimaryButton
                )
            }
        }
        
        // Price
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatCurrency(totalPrice),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDeleteClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "ลบ",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ลบ",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = Color(0xFFFF5252)
                )
            }
        }
    }
}

private fun calculateGroupedItemPrice(item: com.indybrain.indypos_Android.domain.model.CartItem, quantity: Int): Double {
    val productPrice = item.product.price * quantity
    val addonsPrice = item.selectedAddons.values
        .flatten()
        .sumOf { it.price } * quantity
    return productPrice + addonsPrice
}

@Composable
private fun PaymentTypeButton(
    paymentType: PaymentType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) PrimaryButton else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Payment type icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = if (paymentType == PaymentType.CASH) {
                            R.drawable.ic_cash
                        } else {
                            R.drawable.ic_cash_transfer
                        }
                    ),
                    contentDescription = paymentType.displayName,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.Black)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = paymentType.displayName,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = if (isSelected) Color.White else PrimaryText
            )
        }
    }
}

@Composable
private fun OrderButton(
    itemCount: Int,
    totalAmount: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        color = PrimaryButton
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemCount.toString(),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Small
                        ),
                        color = PrimaryButton
                    )
                }
                
                Text(
                    text = "สั่งสินค้า",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = Color.White
                )
            }
            
            Text(
                text = formatCurrency(totalAmount),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = Color.White
            )
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}


@Composable
private fun EmptyCartContent(
    modifier: Modifier = Modifier,
    onAddMenuClick: () -> Unit
) {
    val emptyCartText = stringResource(R.string.order_product_empty_cart)
    val (title, subtitle) = remember(emptyCartText) {
        val parts = emptyCartText.split("\n", limit = 2)
        if (parts.size >= 2) parts[0] to parts[1] else parts[0] to ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = FontUtils.mainFont(
                style = AppFontStyle.Bold,
                size = FontSize.Large
            ),
            color = PrimaryText
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            ),
            color = SecondaryText,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier
                .width(200.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAddMenuClick),
            color = PrimaryButton
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = stringResource(R.string.order_product_add_product),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = Color.White
                )
            }
        }
    }
}
