package com.indybrain.indypos_Android.presentation.products

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

/**
 * Compact Split Layout for Mobile Landscape (30:70)
 * Optimized for mobile phones in landscape orientation
 */
@Composable
fun ProductDetailContentMobileLandscape(
    product: ProductEntity,
    addonGroups: List<AddonGroupEntity>,
    addonsByGroup: Map<String, List<AddonEntity>>,
    selectedAddons: Map<String, Set<String>>,
    quantity: Int,
    specialRequest: String,
    onAddonToggle: (String, String) -> Unit,
    onQuantityIncrease: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onSpecialRequestChange: (String) -> Unit,
    onAddToCart: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backgroundColor = product.selectedColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            Color(0xFFE0E0E0)
        }
    } ?: Color(0xFFE0E0E0)
    
    // Calculate total price
    val basePrice = product.price
    val addonPrice = selectedAddons.values.flatten().sumOf { addonId ->
        addonsByGroup.values.flatten().find { it.id == addonId }?.price ?: 0.0
    }
    val totalPrice = (basePrice + addonPrice) * quantity
    
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(BaseBackground)
    ) {
        // Left: Product Image (30%) - Compact
        Box(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(backgroundColor)
                .clickable { onImageClick() },
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
            val hasColor = product.selectedColorHex != null && product.selectedColorHex.isNotBlank()
            
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
            } else if (!hasColor) {
                Image(
                    painter = painterResource(id = R.drawable.logo_appstore),
                    contentDescription = product.name,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // Right: Product Details + Addons + Actions (70%)
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Product Name & Price - Compact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Large
                            ),
                            color = PrimaryText,
                            maxLines = 2
                        )
                    }
                    
                    Text(
                        text = formatCurrency(basePrice),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Addon Groups - Compact
                if (addonGroups.isNotEmpty()) {
                    addonGroups.forEach { addonGroup ->
                        val addons = addonsByGroup[addonGroup.id] ?: emptyList()
                        if (addons.isNotEmpty()) {
                            AddonGroupSectionMobile(
                                addonGroup = addonGroup,
                                addons = addons,
                                selectedAddonIds = selectedAddons[addonGroup.id] ?: emptySet(),
                                onAddonToggle = { addonId ->
                                    onAddonToggle(addonGroup.id, addonId)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                
                // Special Request - Compact
                SpecialRequestSectionMobile(
                    specialRequest = specialRequest,
                    onSpecialRequestChange = onSpecialRequestChange
                )
            }
            
            // Bottom Action Bar (Fixed at bottom) - Compact
            BottomActionBarMobile(
                quantity = quantity,
                totalPrice = totalPrice,
                onQuantityIncrease = onQuantityIncrease,
                onQuantityDecrease = onQuantityDecrease,
                onAddToCart = onAddToCart
            )
        }
    }
}

@Composable
private fun BottomActionBarMobile(
    quantity: Int,
    totalPrice: Double,
    onQuantityIncrease: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onAddToCart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantity Selector - Compact
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onQuantityDecrease,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
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
                
                Text(
                    text = quantity.toString(),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                TextButton(
                    onClick = onQuantityIncrease,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF5F5F5))
                ) {
                    Text(
                        text = "+",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                }
            }
            
            // Add to Cart Button - Compact
            TextButton(
                onClick = onAddToCart,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PrimaryButton),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "เพิ่ม ${formatCurrency(totalPrice)}",
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

@Composable
private fun AddonGroupSectionMobile(
    addonGroup: AddonGroupEntity,
    addons: List<AddonEntity>,
    selectedAddonIds: Set<String>,
    onAddonToggle: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header - Compact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = addonGroup.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                
                if (addonGroup.maxSelection != null) {
                    Text(
                        text = "(${addonGroup.maxSelection})",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = PrimaryButton
                    )
                }
            }
            
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "ย่อ" else "ขยาย",
                tint = PrimaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        
        // Addon Items - Compact
        if (isExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
            
            addons.forEach { addon ->
                AddonItemMobile(
                    addon = addon,
                    isSelected = selectedAddonIds.contains(addon.id),
                    onToggle = { onAddonToggle(addon.id) }
                )
            }
        }
    }
}

@Composable
private fun AddonItemMobile(
    addon: AddonEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryButton,
                    uncheckedColor = SecondaryText
                ),
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = addon.name,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
        }
        
        Text(
            text = formatCurrency(addon.price),
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Small
            ),
            color = SecondaryText
        )
    }
}

@Composable
private fun SpecialRequestSectionMobile(
    specialRequest: String,
    onSpecialRequestChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "คำขอพิเศษ",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "ย่อ" else "ขยาย",
                tint = PrimaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = specialRequest,
                onValueChange = onSpecialRequestChange,
                placeholder = {
                    Text(
                        text = "เช่น ไม่ใส่ผัก",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = PlaceholderText
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                textStyle = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                singleLine = false,
                maxLines = 2
            )
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}
