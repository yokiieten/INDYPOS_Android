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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
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
 * Landscape layout for ProductDetailScreen
 * Split screen: Product image (left 40%) + Details/Addons/Actions (right 60%)
 */
@Composable
fun ProductDetailContentLandscape(
    product: ProductEntity,
    addonGroups: List<AddonGroupEntity>,
    addonsByGroup: Map<String, List<AddonEntity>>,
    selectedAddons: Map<String, Set<String>>,
    quantity: Int,
    specialRequest: String,
    isEditing: Boolean,
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
    
    val hasAddons = product.hasAdditionalOptions == true
    
    // Calculate total price
    val basePrice = product.price
    val addonPrice = if (hasAddons) {
        selectedAddons.values.flatten().sumOf { addonId ->
            addonsByGroup.values.flatten().find { it.id == addonId }?.price ?: 0.0
        }
    } else 0.0
    val totalPrice = (basePrice + addonPrice) * quantity
    
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(BaseBackground)
    ) {
        // Left: Product Image (40%)
        Box(
            modifier = Modifier
                .weight(0.4f)
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
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // Right: Product Details + Addons + Actions (60%)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Product Name & Price
                Text(
                    text = product.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Larger
                    ),
                    color = PrimaryText
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${formatCurrency(basePrice)} บาท",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Large
                    ),
                    color = SecondaryText
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Addon Groups (only show when hasAdditionalOptions is true)
                if (hasAddons && addonGroups.isNotEmpty()) {
                    addonGroups.forEach { addonGroup ->
                        val addons = addonsByGroup[addonGroup.id] ?: emptyList()
                        if (addons.isNotEmpty()) {
                            AddonGroupSectionLandscape(
                                addonGroup = addonGroup,
                                addons = addons,
                                selectedAddonIds = selectedAddons[addonGroup.id] ?: emptySet(),
                                onAddonToggle = { addonId ->
                                    onAddonToggle(addonGroup.id, addonId)
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                // Special Request
                SpecialRequestSectionLandscape(
                    specialRequest = specialRequest,
                    onSpecialRequestChange = onSpecialRequestChange
                )
            }
            
            // Bottom Action Bar (Fixed at bottom)
            BottomActionBarLandscape(
                quantity = quantity,
                totalPrice = totalPrice,
                onQuantityIncrease = onQuantityIncrease,
                onQuantityDecrease = onQuantityDecrease,
                onAddToCart = onAddToCart,
                isEditing = isEditing
            )
        }
    }
}

@Composable
private fun BottomActionBarLandscape(
    quantity: Int,
    totalPrice: Double,
    onQuantityIncrease: () -> Unit,
    onQuantityDecrease: () -> Unit,
    onAddToCart: () -> Unit,
    isEditing: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quantity Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "จำนวน",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onQuantityDecrease,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
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
                            size = FontSize.Large
                        ),
                        color = PrimaryText,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    
                    TextButton(
                        onClick = onQuantityIncrease,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
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
            }
            
            // Add to Cart / Save Edit Button
            TextButton(
                onClick = onAddToCart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(PrimaryButton),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) {
                            stringResource(id = R.string.product_detail_save_edit)
                        } else {
                            stringResource(id = R.string.product_detail_add_to_cart)
                        },
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = Color.White
                    )
                    Text(
                        text = formatCurrency(totalPrice),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AddonGroupSectionLandscape(
    addonGroup: AddonGroupEntity,
    addons: List<AddonEntity>,
    selectedAddonIds: Set<String>,
    onAddonToggle: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = addonGroup.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                
                // Required + Max selection info (only show required text when required)
                val maxSelection = addonGroup.maxSelection

                Spacer(modifier = Modifier.height(4.dp))

                val infoText = if (addonGroup.isRequired) {
                    val requiredText = stringResource(id = R.string.addon_group_required)
                    if (maxSelection != null && maxSelection > 0) {
                        val selectionText = stringResource(
                            id = R.string.product_max_selection,
                            maxSelection
                        )
                        "$requiredText \u2022 $selectionText"
                    } else {
                        requiredText
                    }
                } else {
                    // Only show max selection if not required
                    if (maxSelection != null && maxSelection > 0) {
                        stringResource(
                            id = R.string.product_max_selection,
                            maxSelection
                        )
                    } else {
                        null
                    }
                }

                if (infoText != null) {
                    Text(
                        text = infoText,
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
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Addon Items
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            
            addons.forEach { addon ->
                AddonItemLandscape(
                    addon = addon,
                    isSelected = selectedAddonIds.contains(addon.id),
                    onToggle = { onAddonToggle(addon.id) }
                )
            }
        }
    }
}

@Composable
private fun AddonItemLandscape(
    addon: AddonEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryButton,
                    uncheckedColor = SecondaryText
                )
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
                size = FontSize.Medium
            ),
            color = PrimaryText
        )
    }
}

@Composable
private fun SpecialRequestSectionLandscape(
    specialRequest: String,
    onSpecialRequestChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.product_detail_special_request),
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
                modifier = Modifier.size(20.dp)
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = specialRequest,
                onValueChange = onSpecialRequestChange,
                placeholder = {
                    Text(
                        text = "เช่น ไม่ใส่ผัก",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PlaceholderText
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                singleLine = false,
                maxLines = 3
            )
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}
