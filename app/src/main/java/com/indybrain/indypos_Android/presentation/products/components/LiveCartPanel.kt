package com.indybrain.indypos_Android.presentation.products.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.domain.model.GroupedCartItem
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

/**
 * Live Cart Panel component for landscape mode
 * Displays cart items in real-time with summary and checkout button
 */
@Composable
fun LiveCartPanel(
    groupedItems: List<GroupedCartItem>,
    subtotal: Double,
    discount: Double,
    total: Double,
    onCartItemClick: (String, String) -> Unit = { _, _ -> }, // productId, productName
    onCheckoutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.product_cart),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
                
                // Item count badge
                if (groupedItems.isNotEmpty()) {
                    val totalItems = groupedItems.sumOf { it.totalQuantity }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryButton),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = totalItems.toString(),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Small
                            ),
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Cart items or empty state
            if (groupedItems.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            tint = PlaceholderText,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.order_product_empty_cart),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PlaceholderText
                        )
                    }
                }
            } else {
                // Cart items list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(groupedItems, key = { it.key }) { groupedItem ->
                        CartItemCard(
                            groupedItem = groupedItem,
                            onClick = {
                                val firstItem = groupedItem.items.firstOrNull()
                                if (firstItem != null) {
                                    onCartItemClick(firstItem.product.id, firstItem.product.name)
                                }
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Divider(color = Color(0xFFE5E5E5), thickness = 1.dp)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Subtotal
                SummaryRow(
                    label = stringResource(id = R.string.order_product_subtotal),
                    value = formatCurrency(subtotal),
                    isTotal = false
                )
                
                // Discount (if any)
                if (discount > 0.001) {
                    SummaryRow(
                        label = stringResource(id = R.string.order_product_discount),
                        value = "-${formatCurrency(discount)}",
                        isTotal = false,
                        valueColor = Color(0xFFE53935)
                    )
                }
                
                Divider(color = Color(0xFFE5E5E5), thickness = 1.dp)
                
                // Total
                SummaryRow(
                    label = stringResource(id = R.string.order_product_total),
                    value = formatCurrency(total),
                    isTotal = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Checkout button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(
                        enabled = groupedItems.isNotEmpty(),
                        onClick = onCheckoutClick
                    ),
                color = if (groupedItems.isNotEmpty()) PrimaryButton else PlaceholderText
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.order_product_checkout),
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
}

@Composable
private fun CartItemCard(
    groupedItem: GroupedCartItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstItem = groupedItem.items.firstOrNull() ?: return
    val product = firstItem.product
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BaseBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        product.selectedColorHex?.let {
                            try {
                                Color(android.graphics.Color.parseColor(it))
                            } catch (e: Exception) {
                                Color(0xFFE0E0E0)
                            }
                        } ?: Color(0xFFE0E0E0)
                    )
            ) {
                val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
                if (!imageUrl.isNullOrBlank()) {
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
                }
            }
            
            // Product details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Product name
                Text(
                    text = product.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Show addons if any
                val addons = firstItem.selectedAddons.values.flatten()
                if (addons.isNotEmpty()) {
                    Text(
                        text = addons.joinToString(", ") { it.name },
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Show special request if any
                if (!firstItem.specialRequest.isNullOrBlank()) {
                    Text(
                        text = "\u0e2b\u0e21\u0e32\u0e22\u0e40\u0e2b\u0e15\u0e38: ${firstItem.specialRequest}",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Price and quantity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity badge
                    Text(
                        text = "x${groupedItem.totalQuantity}",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                    
                    // Price
                    val itemPrice = product.price + addons.sumOf { it.price }
                    val totalPrice = itemPrice * groupedItem.totalQuantity
                    Text(
                        text = formatCurrency(totalPrice),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isTotal: Boolean,
    valueColor: Color = PrimaryText,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = FontUtils.mainFont(
                style = if (isTotal) AppFontStyle.Bold else AppFontStyle.Regular,
                size = if (isTotal) FontSize.Large else FontSize.Medium
            ),
            color = if (isTotal) PrimaryText else SecondaryText
        )
        
        Text(
            text = value,
            style = FontUtils.mainFont(
                style = AppFontStyle.Bold,
                size = if (isTotal) FontSize.Larger else FontSize.Medium
            ),
            color = if (isTotal) PrimaryButton else valueColor
        )
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}
