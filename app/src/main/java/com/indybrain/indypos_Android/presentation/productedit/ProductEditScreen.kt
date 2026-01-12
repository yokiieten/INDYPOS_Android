package com.indybrain.indypos_Android.presentation.productedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.domain.model.Addon
import com.indybrain.indypos_Android.domain.model.GroupedCartItem
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    productId: String,
    productName: String,
    viewModel: ProductEditViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onAddAnother: () -> Unit,
    onUpdateBasket: () -> Unit,
    onEditClick: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var itemToDelete by remember { mutableStateOf<GroupedCartItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Flag to prevent multiple dismiss calls
    var isDismissing by remember { mutableStateOf(false) }
    
    // Flag to prevent multiple update basket calls
    var isUpdatingBasket by remember { mutableStateOf(false) }
    
    // Safe dismiss function with debounce
    val safeDismiss: () -> Unit = {
        if (!isDismissing && itemToDelete == null) {
            isDismissing = true
            onDismiss()
        }
    }
    
    // Safe update basket function with debounce
    val safeUpdateBasket: () -> Unit = {
        if (!isUpdatingBasket && !isDismissing && itemToDelete == null) {
            isUpdatingBasket = true
            onUpdateBasket()
        }
    }
    
    LaunchedEffect(productId) {
        viewModel.init(productId)
    }
    
    // Show product name as title since we're displaying only this product's cart items
    val displayProductName = productName.ifBlank { stringResource(id = R.string.product_cart) }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ProductEditEvent.ShowError -> {
                    // Show snackbar/toast - can be enhanced with SnackbarHost
                }
                is ProductEditEvent.ItemDeleted -> {
                    // Check if no items left after deletion
                    // Wait a bit for state to update
                    delay(100)
                    val currentState = viewModel.uiState.value
                    if (currentState.groupedItems.isEmpty() && !isDismissing) {
                        isDismissing = true
                        onDismiss()
                    }
                }
                is ProductEditEvent.CartUpdated -> {
                    // Show success toast
                }
            }
        }
    }
    
    // Track if we've initialized to prevent auto-dismiss on first load
    var hasInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.groupedItems) {
        if (hasInitialized && uiState.groupedItems.isEmpty() && itemToDelete == null && !isDismissing) {
            // Only dismiss if we've initialized and items become empty
            isDismissing = true
            onDismiss()
        }
        if (uiState.groupedItems.isNotEmpty()) {
            hasInitialized = true
        }
    }
    
    // Bottom Sheet Modal with semi-transparent background
    // Using alpha 0.5 to make main screen visible behind
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures {
                    // Only dismiss if not already dismissing and no dialog is showing
                    if (!isDismissing && itemToDelete == null) {
                        safeDismiss()
                    }
                }
            }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(
                    onClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Product title with delete all button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayProductName,
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Large
                            ),
                            color = PrimaryText,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Delete all button
                        IconButton(
                            onClick = {
                                // Show delete all confirmation
                                itemToDelete = uiState.groupedItems.firstOrNull()?.let { 
                                    // Create a special marker to indicate delete all
                                    it.copy(items = uiState.groupedItems.flatMap { group -> group.items })
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "ลบสินค้าทั้งหมด",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Show error as snackbar (toast without icon)
                    uiState.errorMessage?.let { errorMessage ->
                        LaunchedEffect(errorMessage) {
                            snackbarHostState.showSnackbar(
                                message = errorMessage,
                                duration = androidx.compose.material3.SnackbarDuration.Short
                            )
                            viewModel.clearErrorMessage()
                        }
                    }
                    
                    // Cart items - show all items
                    if (uiState.groupedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
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
                        // Show all grouped items in scrollable list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.groupedItems) { groupedItem ->
                                CartItemGroupCard(
                                    groupedItem = groupedItem,
                                    onIncreaseQuantity = { 
                                        viewModel.increaseQuantity(groupedItem.items.first().id) 
                                    },
                                    onDecreaseQuantity = { 
                                        viewModel.decreaseQuantity(groupedItem.items.first().id) 
                                    },
                                    onDelete = { 
                                        itemToDelete = groupedItem
                                    },
                                    onEditClick = {
                                        val firstItem = groupedItem.items.firstOrNull()
                                        val itemProductId = firstItem?.product?.id
                                        val cartItemId = firstItem?.id
                                        if (itemProductId != null && cartItemId != null) {
                                            onEditClick(itemProductId, cartItemId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onAddAnother,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "เพิ่มอีก",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "เพิ่มอีก",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PrimaryButton
                            )
                        }
                        
                        Button(
                            onClick = safeUpdateBasket,
                            modifier = Modifier.weight(1f),
                            enabled = !isUpdatingBasket && !isDismissing && itemToDelete == null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryButton,
                                disabledContainerColor = PrimaryButton.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "อัปเดตตะกร้า",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Snackbar host for toast messages
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = Color(0xFF323232),
                        contentColor = Color.White
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (itemToDelete != null) {
        // Check if it's delete all (has multiple items from different groups)
        val isDeleteAll = itemToDelete?.items?.size ?: 0 > (uiState.groupedItems.firstOrNull()?.items?.size ?: 0)
        
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = if (isDeleteAll) "ลบสินค้าทั้งหมด" else "ลบสินค้า",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = if (isDeleteAll) {
                        "คุณต้องการลบสินค้าทั้งหมดจากตะกร้าหรือไม่?"
                    } else {
                        "คุณต้องการลบสินค้านี้จากตะกร้าหรือไม่?"
                    },
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { group ->
                            viewModel.deleteItemGroup(group.items.map { it.id })
                            itemToDelete = null
                            // Will be handled by LaunchedEffect watching groupedItems
                        }
                    }
                ) {
                    Text(
                        text = "ลบ",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDelete = null }
                ) {
                    Text(
                        text = "ยกเลิก",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                }
            }
        )
    }
}

@Composable
fun CartItemGroupCard(
    groupedItem: GroupedCartItem,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit,
    onDelete: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val firstItem = groupedItem.items.firstOrNull() ?: return
    val product = firstItem.product
    val totalPrice = calculateItemPrice(firstItem, groupedItem.totalQuantity)
    
    // Get product color for background
    val backgroundColor = product.selectedColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            Color(0xFFE0E0E0)
        }
    } ?: Color(0xFFE0E0E0)
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Product image with AsyncImage
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
                val hasColor = product.selectedColorHex != null && product.selectedColorHex.isNotBlank()
                
                if (!imageUrl.isNullOrBlank()) {
                    // มีรูป: แสดงรูป (ถ้า error ให้แสดง logo default)
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
                } else if (!hasColor) {
                    // ไม่มีทั้งรูปและสี: แสดงไอคอน logo default
                    Image(
                        painter = painterResource(id = R.drawable.logo_appstore),
                        contentDescription = product.name,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                // ถ้ามีแค่สี (hasColor = true) แต่ไม่มีรูป: แสดงแค่สีพื้นหลัง ไม่มีไอคอน
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Product name
                Text(
                    text = product.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Addons display
                if (firstItem.selectedAddons.isNotEmpty()) {
                    val addonsText = firstItem.selectedAddons.values
                        .flatten()
                        .joinToString(", ") { it.name }
                    Text(
                        text = addonsText,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Special request
                if (!firstItem.specialRequest.isNullOrBlank()) {
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quantity controls and price row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Show delete button if quantity == 1, otherwise show decrease button
                        if (groupedItem.totalQuantity == 1) {
                            // Delete button (red circle with trash icon)
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "ลบ",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Decrease button (light gray circle with minus icon)
                            IconButton(
                                onClick = onDecreaseQuantity,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5F5F5))
                            ) {
                                Text(
                                    text = "-",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Large
                                    ),
                                    color = PrimaryText
                                )
                            }
                        }
                        
                        // Quantity display
                        Text(
                            text = "${groupedItem.totalQuantity}",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Increase button (light blue circle with plus icon)
                        IconButton(
                            onClick = onIncreaseQuantity,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "เพิ่ม",
                                tint = PrimaryButton,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Price (right aligned)
                    Text(
                        text = formatCurrency(totalPrice),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Edit button
                TextButton(
                    onClick = onEditClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SecondaryText
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "แก้ไข",
                        modifier = Modifier.size(16.dp),
                        tint = SecondaryText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "แก้ไข",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = SecondaryText
                    )
                }
            }
        }
    }
}

private fun calculateItemPrice(item: com.indybrain.indypos_Android.domain.model.CartItem, quantity: Int): Double {
    val productPrice = item.product.price * quantity
    val addonsPrice = item.selectedAddons.values
        .flatten()
        .sumOf { it.price } * quantity
    return productPrice + addonsPrice
}

private fun formatCurrency(amount: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(amount)}"
}

