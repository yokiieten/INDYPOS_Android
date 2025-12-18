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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import androidx.compose.ui.res.stringResource
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainProductScreen(
    onBackClick: () -> Unit = {},
    onProductClick: (productId: String, productName: String, isInCart: Boolean) -> Unit = { _, _, _ -> },
    onCartClick: () -> Unit = {},
    onBarcodeScannerClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    scannedBarcode: String? = null,
    viewModel: MainProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Handle scanned barcode
    var showProductNotFoundDialog by remember { mutableStateOf(false) }
    var processedBarcode by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let { barcode ->
            // Only process if not already processed
            if (barcode != processedBarcode) {
                processedBarcode = barcode
                // Add small delay to ensure state is stable
                kotlinx.coroutines.delay(100)
                val product: ProductEntity? = viewModel.findProductByCode(barcode)
                product?.let { foundProduct ->
                    // Product found - navigate to detail
                    val currentCartItems = cartItems
                    val cartQuantity = currentCartItems
                        .filter { it.productId == foundProduct.id }
                        .sumOf { it.quantity }
                    // Clear processedBarcode before navigation to prevent re-trigger
                    processedBarcode = null
                    onProductClick(foundProduct.id, foundProduct.name, cartQuantity > 0)
                } ?: run {
                    // Product not found - show dialog
                    showProductNotFoundDialog = true
                    // Clear processedBarcode after showing dialog
                    processedBarcode = null
                }
            }
        } ?: run {
            // Clear processedBarcode when scannedBarcode is null
            processedBarcode = null
        }
    }
    
    // Save scroll position
    var savedScrollIndex by rememberSaveable { mutableStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableStateOf(0) }
    
    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedScrollIndex,
        initialFirstVisibleItemScrollOffset = savedScrollOffset
    )
    
    // Save scroll position when it changes
    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        savedScrollIndex = scrollState.firstVisibleItemIndex
        savedScrollOffset = scrollState.firstVisibleItemScrollOffset
    }
    
            // Track which category is visible based on scroll position
            val visibleCategoryId = remember {
                derivedStateOf {
                    // Use the same categories order as CategoryFilterBar
                    val categoriesWithProducts = uiState.categories.filter { category ->
                        uiState.allProducts.any { it.categoryId == category.id }
                    }
                    
                    if (categoriesWithProducts.isEmpty() || scrollState.layoutInfo.visibleItemsInfo.isEmpty()) {
                        return@derivedStateOf null
                    }
            
            // Get visible items info
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            
            // Find the first visible item that is a category header or products grid
            for (visibleItem in visibleItems) {
                val itemKey = visibleItem.key as? String
                if (itemKey != null) {
                    when {
                        itemKey.startsWith("category_") -> {
                            // Found a category header
                            val categoryId = itemKey.removePrefix("category_")
                            return@derivedStateOf categoryId
                        }
                        itemKey.startsWith("products_") -> {
                            // Found a products grid - get the category ID
                            val categoryId = itemKey.removePrefix("products_")
                            return@derivedStateOf categoryId
                        }
                    }
                }
            }
            
            // If no category found in visible items, check the first visible index
            // Each category takes 2 items: header (even index) and products (odd index)
            val firstVisibleIndex = scrollState.firstVisibleItemIndex
            val categoryIndex = firstVisibleIndex / 2
            
            if (categoryIndex < categoriesWithProducts.size) {
                return@derivedStateOf categoriesWithProducts[categoryIndex].id
            }
            
            // Default to last category if scrolled to bottom
            categoriesWithProducts.lastOrNull()?.id
        }
    }
    
    // Update focused category when scrolling
    LaunchedEffect(visibleCategoryId.value) {
        visibleCategoryId.value?.let { categoryId ->
            if (categoryId != uiState.focusedCategoryId) {
                viewModel.updateFocusedCategory(categoryId)
            }
        }
    }
    
    // Scroll to selected category when category is selected
    // Only perform scroll when the selected category actually changes,
    // so navigating back from ProductDetail won't trigger an extra scroll.
    var lastScrolledCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.selectedCategoryId) {
        val categoryId = uiState.selectedCategoryId
        if (categoryId != null && categoryId != lastScrolledCategoryId) {
            // Use the same categories order as CategoryFilterBar
            val categoriesWithProducts = uiState.categories.filter { category ->
                uiState.allProducts.any { it.categoryId == category.id }
            }
            
            // Each category has 2 items: header (index 0) and products grid (index 1)
            // So category indices are: 0, 2, 4, 6, ...
            var targetIndex = 0
            for (category in categoriesWithProducts) {
                if (category.id == categoryId) {
                    // Scroll to category header with a small offset to ensure header is visible
                    scrollState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = -16 // Small negative offset to show header clearly
                    )
                    break
                }
                // Each category uses 2 items: header + products grid
                targetIndex += 2
            }
            
            lastScrolledCategoryId = categoryId
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.product_title),
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
                    IconButton(onClick = onBarcodeScannerClick) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = stringResource(id = R.string.product_barcode_scanner),
                            tint = PrimaryText
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(id = R.string.product_search),
                            tint = PrimaryText
                        )
                    }
                    IconButton(onClick = { /* TODO: Grid/List toggle */ }) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = stringResource(id = R.string.product_change_view),
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
            // Observe cart count once for both list padding and cart button
            val cartItemCount by viewModel.cartItemCount.collectAsStateWithLifecycle(initialValue = 0)
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Category filter bar at top - only show categories that have products
                // Products without categoryId are already filtered in ViewModel
                val categoriesWithProducts = uiState.categories.filter { category ->
                    uiState.allProducts.any { it.categoryId == category.id }
                }
                // Only show focused category if it has products
                val validFocusedCategoryId = uiState.focusedCategoryId?.takeIf { categoryId ->
                    uiState.allProducts.any { it.categoryId == categoryId }
                }
                CategoryFilterBar(
                    categories = categoriesWithProducts,
                    focusedCategoryId = validFocusedCategoryId,
                    onCategorySelected = { categoryId ->
                        viewModel.selectCategory(categoryId)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Products grid
                // Use allProducts instead of products to avoid flickering
                val hasProducts = uiState.allProducts.isNotEmpty()
                
                when {
                    uiState.isLoading -> {
                        // Show loading only when loading
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryButton)
                        }
                    }
                    !hasProducts -> {
                        // Show empty state only when not loading and no products
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.product_empty),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = SecondaryText
                            )
                        }
                    }
                    else -> {
                        // Show products when we have products
                        // Always show all products grouped by category
                        // Filtering is handled by scrolling to the selected category
                        // Products without categoryId are already filtered in ViewModel
                        
                        // Use the same categories order as CategoryFilterBar
                        val categoriesWithProducts = uiState.categories.filter { category ->
                            uiState.allProducts.any { it.categoryId == category.id }
                        }
                        
                        // Group products by categoryId for quick lookup
                        val productsByCategoryId = uiState.allProducts
                            .filter { it.categoryId != null && it.categoryId.isNotBlank() }
                            .groupBy { it.categoryId }
                    
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            // Fixed bottom padding so content doesn't jump when cart button appears/disappears
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                    if (categoriesWithProducts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.product_empty),
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Medium
                                    ),
                                    color = SecondaryText
                                )
                            }
                        }
                    } else {
                        // Show products grouped by category in the same order as CategoryFilterBar
                        categoriesWithProducts.forEach { category ->
                            val products = productsByCategoryId[category.id] ?: emptyList()
                            
                            // Skip if no products in this category
                            if (products.isEmpty()) return@forEach
                            
                            item(key = "category_${category.id}") {
                                // Category header
                                Text(
                                    text = category.name,
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Large
                                    ),
                                    color = PrimaryText,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            // Products grid for this category
                            item(key = "products_${category.id}") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    products.chunked(2).forEach { rowProducts ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowProducts.forEach { product ->
                                                val cartQuantity = cartItems
                                                    .filter { it.productId == product.id }
                                                    .sumOf { it.quantity }
                                                
                                                ProductCard(
                                                    product = product,
                                                    cartQuantity = cartQuantity,
                                                    onClick = { 
                                                        // If product is in cart, pass productName for ProductEditScreen
                                                        // Otherwise, just pass productId for ProductDetailScreen
                                                        onProductClick(product.id, product.name, cartQuantity > 0)
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxWidth()
                                                )
                                            }
                                            // Add spacer if odd number of products
                                            if (rowProducts.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
            
                // Error message
                uiState.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = error,
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            
            // Cart Button - Floating at bottom
            if (cartItemCount > 0) {
                CartButton(
                    itemCount = cartItemCount,
                    onClick = onCartClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
        
        // Product Not Found Dialog
        if (showProductNotFoundDialog) {
            AlertDialog(
                onDismissRequest = { showProductNotFoundDialog = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_product_not_found_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_product_not_found_message),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showProductNotFoundDialog = false }
                    ) {
                        Text(
                            text = stringResource(id = R.string.dialog_button_ok),
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
    }
}

@Composable
private fun CategoryFilterBar(
    categories: List<CategoryEntity>,
    focusedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryScrollState = rememberLazyListState()
    
    // Scroll to focused category when it changes
    LaunchedEffect(focusedCategoryId) {
        focusedCategoryId?.let { categoryId ->
            val categoryIndex = categories.indexOfFirst { it.id == categoryId }
            if (categoryIndex >= 0) {
                // Check if the item is visible
                val layoutInfo = categoryScrollState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val isVisible = visibleItems.any { it.index == categoryIndex }
                
                if (!isVisible) {
                    // Scroll to the category if it's not visible
                    categoryScrollState.animateScrollToItem(
                        index = categoryIndex,
                        scrollOffset = 0
                    )
                }
            }
        }
    }
    
    LazyRow(
        state = categoryScrollState,
        modifier = modifier
            .background(BaseBackground)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // Category chips only (no "ทั้งหมด")
        items(categories) { category ->
            CategoryChip(
                text = category.name,
                isFocused = focusedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .wrapContentWidth(Alignment.Start)
    ) {
        Text(
            text = text,
            style = FontUtils.mainFont(
                style = if (isFocused) AppFontStyle.Bold else AppFontStyle.Regular,
                size = FontSize.Medium
            ),
            color = if (isFocused) PrimaryText else SecondaryText,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                textWidth = with(density) {
                    coordinates.size.width.toDp()
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Underline when focused (light blue)
        if (isFocused) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(textWidth)
                    .background(PrimaryButton)
            )
        } else {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
    cartQuantity: Int = 0,
    onClick: () -> Unit = {},
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
    
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Product image or color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(backgroundColor)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Check if imageUrl exists and is not empty
                    val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
                    val hasColor = product.selectedColorHex != null && product.selectedColorHex.isNotBlank()
                    
                    if (!imageUrl.isNullOrBlank()) {
                    // Load image from URL using Coil
                    // Handle both absolute URLs and relative URLs
                    val fullImageUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                        imageUrl
                    } else {
                        // If relative URL, prepend base URL
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
                        // If no image URL and no color, show placeholder icon
                        Image(
                            painter = painterResource(id = R.drawable.logo_appstore),
                            contentDescription = product.name,
                            modifier = Modifier.size(60.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    // If has color but no image, just show the background color (no icon)
                }
            }
            
            // Product info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Small
                    ),
                    color = PrimaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCurrency(product.price),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Small
                        ),
                        color = PrimaryButton
                    )
                    
                    // Cart Badge - Bottom Right (if in cart)
                    if (cartQuantity > 0) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryButton),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cartQuantity.toString(),
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
        }
    }
}

@Composable
private fun CartButton(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = PrimaryButton
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = stringResource(id = R.string.product_cart),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = stringResource(id = R.string.product_view_cart, itemCount),
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

