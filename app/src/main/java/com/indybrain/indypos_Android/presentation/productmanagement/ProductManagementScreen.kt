package com.indybrain.indypos_Android.presentation.productmanagement

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
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
import java.text.DecimalFormat

/**
 * Entry point for accessing NetworkConnectivityChecker in Composable
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkCheckerEntryPoint {
    fun networkConnectivityChecker(): NetworkConnectivityChecker
}

/**
 * Build proper image URL from image path
 * Handles both absolute URLs and relative paths
 */
@Composable
private fun buildImageUrl(imagePath: String): String {
    return AppConfig.buildImageUrl(imagePath)
}

/**
 * Product Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    onBackClick: () -> Unit = {},
    onAddProductClick: () -> Unit = {},
    onEditProductClick: (String) -> Unit = {},
    viewModel: ProductManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var showMultipleDeleteConfirmation by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    // Pull to refresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading)
    
    // Load sync statistics when dialog opens
    LaunchedEffect(showSyncDialog) {
        if (showSyncDialog) {
            viewModel.loadSyncStatistics()
        }
    }
    
    // Update search when query changes
    LaunchedEffect(searchQuery) {
        viewModel.searchProducts(searchQuery)
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isSelectionMode) 
                            stringResource(id = R.string.product_management_select_products)
                        else 
                            stringResource(id = R.string.product_management_title),
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
                actions = {
                    // Sync button
                    // if (!uiState.isSelectionMode) {
                    //     IconButton(onClick = { showSyncDialog = true }) {
                    //         Icon(
                    //             imageVector = Icons.Filled.Sync,
                    //             contentDescription = stringResource(id = R.string.product_management_sync),
                    //             tint = GreenComplete
                    //         )
                    //     }
                    // }
                    // Edit/Cancel button
                    TextButton(
                        onClick = { viewModel.toggleSelectionMode() }
                    ) {
                        Text(
                            text = if (uiState.isSelectionMode) 
                                stringResource(id = R.string.product_management_cancel)
                            else 
                                stringResource(id = R.string.product_management_edit),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = if (uiState.isSelectionMode) PrimaryText else PrimaryButton
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar - Hide in selection mode
                if (!uiState.isSelectionMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.product_management_search_placeholder),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PlaceholderText
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = PlaceholderText,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFE6F2FF),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                    
                    // Category Dropdown - Hide in selection mode
                    CategoryDropdown(
                        categories = uiState.categories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategorySelected = { categoryId ->
                            viewModel.selectCategory(categoryId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Product List with Pull to Refresh
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshProducts() }
                ) {
                    // Calculate products to show
                    val productsToShow = uiState.filteredProducts ?: emptyList()
                    // Show loading if data hasn't been loaded yet (products is null) or isLoading is true
                    val shouldShowLoading = uiState.isLoading || uiState.products == null
                    
                    when {
                        shouldShowLoading -> {
                            // Show loading indicator when loading or data hasn't been loaded yet
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryButton)
                            }
                        }
                        productsToShow.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.product_management_empty),
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Medium
                                    ),
                                    color = SecondaryText
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 8.dp,
                                    end = 16.dp,
                                    bottom = if (uiState.isSelectionMode) 80.dp else 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(productsToShow) { product ->
                                    ProductItem(
                                        product = product,
                                        categories = uiState.categories,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isSelected = uiState.selectedProductIds.contains(product.id),
                                        onClick = { 
                                            if (uiState.isSelectionMode) {
                                                viewModel.toggleProductSelection(product.id ?: "")
                                            } else {
                                                selectedProduct = product
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Action Bar - Show different UI based on selection mode
            if (uiState.isSelectionMode) {
                // Selection Mode - Show selection actions
                val productsToShow = uiState.filteredProducts ?: emptyList()
                SelectionModeBottomBar(
                    selectedCount = uiState.selectedProductIds.size,
                    totalCount = productsToShow.size,
                    onSelectAll = { viewModel.selectAllProducts() },
                    onDeselectAll = { viewModel.deselectAllProducts() },
                    onDelete = {
                        if (uiState.selectedProductIds.isNotEmpty()) {
                            showMultipleDeleteConfirmation = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // Normal Mode - Show Add Product Button
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddProductClick),
                    color = PrimaryButton
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.product_management_add_product),
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
        
        // Product Action Sheet
        selectedProduct?.let { product ->
            ProductActionSheet(
                product = product,
                onDismiss = { selectedProduct = null },
                onToggleStatus = {
                    viewModel.toggleProductStatus(product.id ?: "", product.isActive ?: false)
                    selectedProduct = null
                },
                onEdit = {
                    product.id?.let { productId ->
                        onEditProductClick(productId)
                    }
                    selectedProduct = null
                },
                onDelete = {
                    productToDelete = selectedProduct
                    showDeleteConfirmation = true
                    selectedProduct = null
                }
            )
        }
        
        // Delete Confirmation Dialog (Single)
        if (showDeleteConfirmation && productToDelete != null) {
            DeleteConfirmationDialog(
                productName = productToDelete!!.name ?: "สินค้า",
                onConfirm = {
                    productToDelete?.id?.let { productId ->
                        viewModel.deleteProduct(productId)
                    }
                    showDeleteConfirmation = false
                    productToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmation = false
                    productToDelete = null
                }
            )
        }
        
        // Multiple Delete Confirmation Dialog
        if (showMultipleDeleteConfirmation) {
            MultipleDeleteConfirmationDialog(
                selectedCount = uiState.selectedProductIds.size,
                onConfirm = {
                    viewModel.deleteSelectedProducts()
                    showMultipleDeleteConfirmation = false
                },
                onDismiss = {
                    showMultipleDeleteConfirmation = false
                }
            )
        }
        
        // Sync Status Dialog
        if (showSyncDialog) {
            SyncStatusDialog(
                statistics = uiState.syncStatistics,
                onDismiss = { showSyncDialog = false },
                onSyncNow = {
                    viewModel.refreshProducts()
                    showSyncDialog = false
                }
            )
        }
        
        // Toggle Success Dialog
        uiState.toggleSuccessMessage?.let { message ->
            ToggleSuccessDialog(
                message = message,
                onDismiss = {
                    viewModel.clearToggleSuccessMessage()
                }
            )
        }
        
        // Delete Success Dialog
        uiState.deleteSuccessMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearDeleteSuccessMessage() },
                title = {
                    Text(
                        text = "สำเร็จ",
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
                    TextButton(onClick = { viewModel.clearDeleteSuccessMessage() }) {
                        Text("ตกลง", color = PrimaryButton)
                    }
                }
            )
        }
        
        // Error Message Dialog
        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = {
                    Text(
                        text = "เกิดข้อผิดพลาด",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = error,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("ตกลง", color = PrimaryButton)
                    }
                }
            )
        }
    }
}

/**
 * Category Dropdown
 */
@Composable
private fun CategoryDropdown(
    categories: List<com.indybrain.indypos_Android.data.local.entity.CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    val selectedCategoryName = selectedCategoryId?.let { id ->
        categories.find { it.id == id }?.name
    } ?: stringResource(id = R.string.product_management_all_categories)
    
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showDropdown = true },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E5E5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedCategoryName,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = SecondaryText
            )
        }
    }
    
    if (showDropdown) {
        AlertDialog(
            onDismissRequest = { showDropdown = false },
            title = {
                Text(
                    text = "${stringResource(id = R.string.product_management_all_categories)} (${categories.size + 1} รายการ)",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
            },
            text = {
                // Use LazyColumn for scrollable list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // Limit max height for scrolling
                ) {
                    // All Categories option
                    item {
                        TextButton(
                            onClick = {
                                onCategorySelected(null)
                                showDropdown = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.product_management_all_categories),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = if (selectedCategoryId == null) PrimaryButton else PrimaryText
                            )
                        }
                    }
                    // Category options
                    items(categories) { category ->
                        TextButton(
                            onClick = {
                                onCategorySelected(category.id)
                                showDropdown = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = category.name,
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = if (selectedCategoryId == category.id) PrimaryButton else PrimaryText
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDropdown = false }) {
                    Text(
                        text = stringResource(id = R.string.product_management_cancel),
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

/**
 * Product Item Row
 */
@Composable
private fun ProductItem(
    product: ProductEntity,
    categories: List<com.indybrain.indypos_Android.data.local.entity.CategoryEntity>,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = product.selectedColorHex?.let { 
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            Color(0xFFE0E0E0)
        }
    } ?: Color(0xFFE0E0E0)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelectionMode && isSelected) Color(0xFFF5F5F5) else Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (in selection mode)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryButton,
                        uncheckedColor = SecondaryText
                    )
                )
            }
            
            // Product Image/Color
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
                if (!imageUrl.isNullOrBlank()) {
                    // Build proper image URL
                    val fullImageUrl = buildImageUrl(imageUrl)
                    
                    // Check network connectivity
                    val networkChecker = remember { 
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            NetworkCheckerEntryPoint::class.java
                        ).networkConnectivityChecker()
                    }
                    val hasNetwork = networkChecker.isConnected()
                    
                    // Build ImageRequest with cache policy
                    val imageRequest = ImageRequest.Builder(context)
                        .data(fullImageUrl)
                        .crossfade(true)
                        .apply {
                            if (hasNetwork) {
                                // Online: allow network and cache
                                memoryCachePolicy(CachePolicy.ENABLED)
                                diskCachePolicy(CachePolicy.ENABLED)
                                networkCachePolicy(CachePolicy.ENABLED)
                            } else {
                                // Offline: only use cache
                                memoryCachePolicy(CachePolicy.ENABLED)
                                diskCachePolicy(CachePolicy.ENABLED)
                                networkCachePolicy(CachePolicy.DISABLED)
                            }
                        }
                        .build()
                    
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.logo_appstore),
                        placeholder = painterResource(id = R.drawable.logo_appstore)
                    )
                } else if (product.selectedColorHex == null || product.selectedColorHex.isBlank()) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_appstore),
                        contentDescription = product.name,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Product Name
                Text(
                    text = product.name ?: "",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Category and Price
                val categoryName = product.categoryId?.let { categoryId ->
                    categories.find { it.id == categoryId }?.name
                } ?: stringResource(id = R.string.product_no_category_title)
                Text(
                    text = "$categoryName - ${formatCurrency(product.price ?: 0.0)}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText
                )
            }
            
            // Active Status Button (right side)
            if (!isSelectionMode) {
                Surface(
                    color = if (product.isActive == true) GreenComplete else RedFailure,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (product.isActive == true) "ใช้งาน" else "ไม่ใช้งาน",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Selection Mode Bottom Bar
 */
@Composable
private fun SelectionModeBottomBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Select All / Deselect All Button
            TextButton(
                onClick = if (selectedCount == totalCount) onDeselectAll else onSelectAll
            ) {
                Text(
                    text = if (selectedCount == totalCount) 
                        stringResource(id = R.string.product_management_deselect_all)
                    else 
                        stringResource(id = R.string.product_management_select_all),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryButton
                )
            }
            
            // Selected Count
            Text(
                text = stringResource(
                    id = R.string.product_management_select_items,
                    selectedCount
                ),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            // Delete Button
            TextButton(
                onClick = onDelete,
                enabled = selectedCount > 0
            ) {
                Text(
                    text = stringResource(id = R.string.product_management_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = if (selectedCount > 0) RedFailure else SecondaryText
                )
            }
        }
    }
}

/**
 * Product Action Sheet
 */
@Composable
private fun ProductActionSheet(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.product_management_action_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(id = R.string.product_management_action_subtitle),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Toggle Status
                    TextButton(
                        onClick = onToggleStatus,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                id = if (product.isActive == true) 
                                    R.string.product_management_action_deactivate 
                                else 
                                    R.string.product_management_action_activate
                            ),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                    
                    // Edit
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.product_management_action_edit),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                    
                    // Delete
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.product_management_action_delete),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = RedFailure
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cancel Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss),
                        color = PrimaryButton
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.product_management_cancel),
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
    }
}

/**
 * Delete Confirmation Dialog
 */
@Composable
private fun DeleteConfirmationDialog(
    productName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.product_management_confirm_delete_title),
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
                    text = "คุณต้องการลบสินค้า '$productName' ใช่หรือไม่?",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.product_management_confirm_delete_warning),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.product_management_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = RedFailure
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.product_management_cancel),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        }
    )
}

/**
 * Multiple Delete Confirmation Dialog
 */
@Composable
private fun MultipleDeleteConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.product_management_confirm_delete_title),
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
                    text = "คุณต้องการลบสินค้า $selectedCount รายการใช่หรือไม่?",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.product_management_confirm_delete_warning),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.product_management_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = RedFailure
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.product_management_cancel),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = PrimaryButton
                )
            }
        }
    )
}

/**
 * Sync Status Dialog
 */
@Composable
private fun SyncStatusDialog(
    statistics: com.indybrain.indypos_Android.domain.repository.ProductSyncStatistics?,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.product_management_sync_status_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Column {
                if (statistics != null) {
                    Text(
                        text = stringResource(id = R.string.product_management_sync_total, statistics.totalProducts),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.product_management_sync_synced, statistics.syncedCount),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.product_management_sync_pending, statistics.pendingSyncCount),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.product_management_sync_deleted, statistics.deletedCount),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryButton
                    )
                }
            }
        },
        confirmButton = {
            if (statistics != null && statistics.pendingSyncCount > 0) {
                TextButton(onClick = onSyncNow) {
                    Text(
                        text = stringResource(id = R.string.product_management_sync_now),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.dialog_button_ok),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        }
    )
}

/**
 * Toggle Success Dialog
 */
@Composable
private fun ToggleSuccessDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.product_management_toggle_success_title),
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
                color = PrimaryText
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.dialog_button_ok),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryButton
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Format currency
 */
private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

