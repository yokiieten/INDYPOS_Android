package com.indybrain.indypos_Android.presentation.orderproduct

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.CartAddonEntity
import com.indybrain.indypos_Android.data.local.entity.CartItemEntity
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
    onEditItemClick: (Long) -> Unit = {},
    viewModel: OrderProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var itemToDelete by remember { mutableStateOf<CartItemEntity?>(null) }
    
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
                
                TextButton(onClick = onAddMenuClick) {
                    Text(
                        text = "เพิ่มเมนู",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
            
            // Cart items list
            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ไม่มีสินค้าในตะกร้า",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = uiState.cartItems,
                        key = { it.id }
                    ) { cartItem ->
                        val addons = viewModel.getCartAddons(cartItem.id)

                        SwipeToDeleteCartItem(
                            onDelete = { itemToDelete = cartItem }
                        ) {
                            CartItemRow(
                                cartItem = cartItem,
                                addons = addons,
                                onEditClick = { onEditItemClick(cartItem.id) },
                                onDeleteClick = { itemToDelete = cartItem }
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
                            
                            TextButton(onClick = { /* TODO: Add discount dialog */ }) {
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
            if (uiState.cartItems.isNotEmpty()) {
                OrderButton(
                    itemCount = uiState.cartItems.size,
                    totalAmount = viewModel.calculateTotal(),
                    onClick = { viewModel.placeOrder() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
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
                        itemToDelete?.let { viewModel.deleteCartItem(it.id) }
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
private fun CartItemRow(
    cartItem: CartItemEntity,
    addons: List<CartAddonEntity>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = cartItem.productColorHex?.let {
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
                val imageUrl = cartItem.productImageUrl?.takeIf { it.isNotBlank() }
                
                if (!imageUrl.isNullOrBlank()) {
                    val fullImageUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                        imageUrl
                    } else {
                        "https://indy-pos.com$imageUrl"
                    }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fullImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = cartItem.productName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.logo_appstore),
                        placeholder = painterResource(id = R.drawable.logo_appstore)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo_appstore),
                        contentDescription = cartItem.productName,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Quantity badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cartItem.quantity.toString(),
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
                text = cartItem.productName,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Addons (customizations)
            if (addons.isNotEmpty()) {
                val addonNames = addons.map { it.addonName }
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
                text = formatCurrency(
                    cartItem.unitPrice * cartItem.quantity +
                        addons.sumOf { it.addonPrice } * cartItem.quantity
                ),
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
            // Wallet icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (paymentType == PaymentType.CASH) {
                    // Cash icon - simple wallet representation
                    Canvas(modifier = Modifier.size(20.dp)) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val iconColor = if (isSelected) PrimaryButton else SecondaryText
                        
                        // Draw wallet shape
                        drawRoundRect(
                            color = iconColor,
                            topLeft = Offset(centerX - 8f, centerY - 6f),
                            size = Size(16f, 10f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        // Draw circle inside (representing coin/money)
                        drawCircle(
                            color = iconColor,
                            radius = 3f,
                            center = Offset(centerX, centerY)
                        )
                    }
                } else {
                    // Transfer icon - circular arrows
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = if (isSelected) PrimaryButton else SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
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

