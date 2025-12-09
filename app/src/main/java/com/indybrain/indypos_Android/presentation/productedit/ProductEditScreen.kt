package com.indybrain.indypos_Android.presentation.productedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import com.indybrain.indypos_Android.R
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
    onUpdateBasket: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var itemToDelete by remember { mutableStateOf<GroupedCartItem?>(null) }
    
    LaunchedEffect(productId) {
        viewModel.init(productId)
    }
    
    // Get product name from first item if not provided
    val displayProductName = remember(uiState.groupedItems, productName) {
        if (productName.isNotBlank()) {
            productName
        } else {
            uiState.groupedItems.firstOrNull()?.items?.firstOrNull()?.product?.name ?: "สินค้า"
        }
    }
    
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
                    kotlinx.coroutines.delay(100)
                    val currentState = viewModel.uiState.value
                    if (currentState.groupedItems.isEmpty()) {
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
        if (hasInitialized && uiState.groupedItems.isEmpty() && itemToDelete == null) {
            // Only dismiss if we've initialized and items become empty
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
            .clickable(
                onClick = onDismiss,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Product title
                Text(
                    text = displayProductName,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
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
                        onClick = onUpdateBasket,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryButton
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
        }
    }
    
    // Delete confirmation dialog
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
                    text = "คุณต้องการลบสินค้านี้จากตะกร้าหรือไม่?",
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
                            // Check if this was the last item
                            if (uiState.groupedItems.size == 1) {
                                // Will be handled by LaunchedEffect watching groupedItems
                            }
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
    onDelete: () -> Unit
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
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.logo_appstore),
                        placeholder = painterResource(id = R.drawable.logo_appstore)
                    )
                } else {
                    // Show placeholder if no image
                    Image(
                        painter = painterResource(id = R.drawable.logo_appstore),
                        contentDescription = product.name,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
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
                    onClick = { /* TODO: Navigate to edit screen */ },
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

