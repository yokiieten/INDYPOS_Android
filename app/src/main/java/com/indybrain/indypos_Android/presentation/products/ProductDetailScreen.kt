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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit = {},
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "ปิด",
                        tint = PrimaryText
                    )
                }
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
            uiState.product == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ไม่พบสินค้า",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                }
            }
            else -> {
                ProductDetailContent(
                    product = uiState.product!!,
                    addonGroups = uiState.addonGroups,
                    addonsByGroup = uiState.addonsByGroup,
                    selectedAddons = uiState.selectedAddons,
                    quantity = uiState.quantity,
                    specialRequest = uiState.specialRequest,
                    onAddonToggle = { addonGroupId, addonId ->
                        viewModel.toggleAddon(addonGroupId, addonId)
                    },
                    onQuantityChange = { newQuantity ->
                        viewModel.updateQuantity(newQuantity)
                    },
                    onSpecialRequestChange = { request ->
                        viewModel.updateSpecialRequest(request)
                    },
                    onAddToCart = {
                        viewModel.addToCart()
                        onBackClick() // Navigate back after adding to cart
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductEntity,
    addonGroups: List<AddonGroupEntity>,
    addonsByGroup: Map<String, List<AddonEntity>>,
    selectedAddons: Map<String, Set<String>>,
    quantity: Int,
    specialRequest: String,
    onAddonToggle: (String, String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onSpecialRequestChange: (String) -> Unit,
    onAddToCart: () -> Unit,
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
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        // Product Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
            val hasColor = product.selectedColorHex != null && product.selectedColorHex.isNotBlank()
            
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
            } else if (!hasColor) {
                Image(
                    painter = painterResource(id = R.drawable.logo_appstore),
                    contentDescription = product.name,
                    modifier = Modifier.size(60.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        // Product Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = product.name,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Larger
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${formatCurrency(basePrice)} บาท",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
        }
        
        // Addon Groups
        if (addonGroups.isNotEmpty()) {
            addonGroups.forEach { addonGroup ->
                val addons = addonsByGroup[addonGroup.id] ?: emptyList()
                if (addons.isNotEmpty()) {
                    AddonGroupSection(
                        addonGroup = addonGroup,
                        addons = addons,
                        selectedAddonIds = selectedAddons[addonGroup.id] ?: emptySet(),
                        onAddonToggle = { addonId ->
                            onAddonToggle(addonGroup.id, addonId)
                        }
                    )
                }
            }
        }
        
        // Special Request Section
        SpecialRequestSection(
            specialRequest = specialRequest,
            onSpecialRequestChange = onSpecialRequestChange
        )
        
            Spacer(modifier = Modifier.height(100.dp)) // Space for bottom bar
        }
        
        // Bottom Bar with Quantity and Add to Cart
        BottomActionBar(
            quantity = quantity,
            totalPrice = totalPrice,
            onQuantityChange = onQuantityChange,
            onAddToCart = onAddToCart,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun BottomActionBar(
    quantity: Int,
    totalPrice: Double,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quantity Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
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
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                TextButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
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
            
            // Add to Cart Button
            TextButton(
                onClick = onAddToCart,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PrimaryButton),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "เพิ่มลงตะกร้า ${formatCurrency(totalPrice)}",
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
private fun AddonGroupSection(
    addonGroup: AddonGroupEntity,
    addons: List<AddonEntity>,
    selectedAddonIds: Set<String>,
    onAddonToggle: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = addonGroup.name,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Max selection indicator
                if (addonGroup.maxSelection != null) {
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "เลือกได้สูงสุด ${addonGroup.maxSelection} รายการ",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryButton,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        }
        
        // Addon Items
        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            
            addons.forEach { addon ->
                AddonItem(
                    addon = addon,
                    isSelected = selectedAddonIds.contains(addon.id),
                    onToggle = { onAddonToggle(addon.id) }
                )
            }
        }
    }
}

@Composable
private fun AddonItem(
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
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
private fun SpecialRequestSection(
    specialRequest: String,
    onSpecialRequestChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
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

