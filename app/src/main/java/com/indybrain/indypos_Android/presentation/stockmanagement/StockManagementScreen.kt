package com.indybrain.indypos_Android.presentation.stockmanagement

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Stock Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockManagementScreen(
    onBackClick: () -> Unit = {},
    viewModel: StockManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_manage_stock),
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
                            contentDescription = stringResource(id = R.string.product_back),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.products.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryButton
                )
            } else if (uiState.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.stock_management_empty_products),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.products) { product ->
                        StockProductItem(
                            product = product,
                            onClick = { viewModel.showStockUpdateDialog(product) },
                            context = context
                        )
                    }
                }
            }
        }
    }
    
    // Stock Update Dialog
    if (uiState.showStockUpdateDialog && uiState.selectedProduct != null) {
        StockUpdateDialog(
            product = uiState.selectedProduct!!,
            quantity = uiState.stockUpdateQuantity,
            isUpdating = uiState.isUpdatingStock,
            onQuantityChange = { viewModel.updateStockQuantityInput(it) },
            onUpdate = { viewModel.updateStock() },
            onDismiss = { viewModel.dismissStockUpdateDialog() }
        )
    }
    
    // No Internet Dialog
    if (uiState.showNoInternetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNoInternetDialog() },
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
                    text = stringResource(id = R.string.stock_management_no_internet_message),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissNoInternetDialog() }) {
                    Text(
                        text = stringResource(id = R.string.dialog_button_ok),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        )
                    )
                }
            }
        )
    }
    
    // Success Message
    uiState.updateSuccessMessage?.let { message ->
        AlertDialog(
            // Prevent dismissing by clicking outside or back button
            onDismissRequest = { /* Do nothing */ },
            title = {
                Text(
                    text = stringResource(id = R.string.stock_management_success_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    text = message,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                    Text(
                        text = stringResource(id = R.string.dialog_button_ok),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        )
                    )
                }
            }
        )
    }
    
    // Error Message
    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = {
                Text(
                    text = stringResource(id = R.string.dialog_error_title),
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
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(
                        text = stringResource(id = R.string.dialog_button_ok),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        )
                    )
                }
            }
        )
    }
}

/**
 * Stock Product Item
 */
@Composable
private fun StockProductItem(
    product: ProductEntity,
    onClick: () -> Unit,
    context: android.content.Context
) {
    val stockQuantity = product.stockQuantity ?: 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image/Color
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
                val hasColor = product.selectedColorHex != null && product.selectedColorHex.isNotBlank()
                val isColorMode = hasColor && !imageUrl.isNullOrBlank() == false
                
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
                } else if (isColorMode && product.selectedColorHex != null) {
                    // Show color background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(product.selectedColorHex))
                                } catch (e: Exception) {
                                    Color(0xFFE0E0E0)
                                }
                            )
                    )
                } else {
                    // Show placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_appstore),
                            contentDescription = product.name,
                            modifier = Modifier.size(40.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(id = R.string.stock_management_stock_label, stockQuantity),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText
                )
            }
            
            // Sync Indicator
            if (!product.isSynced) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(
                            Color(0xFFFF9500),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.stock_management_sync_pending),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Small
                        ),
                        color = Color.White
                    )
                }
            }
            
            // Stock Status Indicator
            val statusColor = when {
                stockQuantity <= 0 -> RedFailure
                stockQuantity <= 10 -> Color(0xFFFF9500) // Orange
                else -> GreenComplete
            }
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
        }
    }
}

/**
 * Stock Update Dialog
 */
@Composable
private fun StockUpdateDialog(
    product: ProductEntity,
    quantity: String,
    isUpdating: Boolean,
    onQuantityChange: (String) -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = {
            Text(
                text = stringResource(id = R.string.stock_management_update_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Column {
                Text(
                    text = product.name + "\n" + stringResource(
                        id = R.string.stock_management_current_stock,
                        product.stockQuantity ?: 0
                    ),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        // Only allow numbers and minus sign
                        if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*"))) {
                            onQuantityChange(newValue)
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.stock_management_quantity_label),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                        focusedBorderColor = PrimaryButton
                    ),
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (isUpdating) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PrimaryButton,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "อัปเดต",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            } else {
                TextButton(
                    onClick = onUpdate,
                    enabled = quantity.isNotBlank()
                ) {
                    Text(
                        text = stringResource(id = R.string.stock_management_update_button),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text(
                    text = stringResource(id = R.string.stock_management_cancel_button),
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

