package com.indybrain.indypos_Android.presentation.products

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.indybrain.indypos_Android.core.ui.isTabletLandscape
import com.indybrain.indypos_Android.core.ui.getProductGridColumns

enum class ProductViewMode {
    GRID,
    LIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainProductScreen(
    onBackClick: () -> Unit = {},
    onProductClick: (productId: String, productName: String, isInCart: Boolean) -> Unit = { _, _, _ -> },
    onProductClickFromScan: (productId: String, productName: String) -> Unit = { _, _ -> },
    onCartClick: () -> Unit = {},
    onBarcodeScannerClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    scannedBarcode: String? = null,
    viewModel: MainProductViewModel = hiltViewModel()
) {
    // Use split screen layout only for tablet in landscape mode
    // Mobile (all orientations) and Tablet portrait → use original layout
    val shouldUseSplitScreen = isTabletLandscape()
    
    if (shouldUseSplitScreen) {
        // Tablet landscape: Use split screen layout with live cart panel
        MainProductScreenLandscape(
            onBackClick = onBackClick,
            onProductClick = onProductClick,
            onProductClickFromScan = onProductClickFromScan,
            onCartClick = onCartClick,
            onBarcodeScannerClick = onBarcodeScannerClick,
            onSearchClick = onSearchClick,
            scannedBarcode = scannedBarcode,
            viewModel = viewModel
        )
        return
    }
    
    // Default layout (Mobile all orientations + Tablet portrait)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // View mode state
    var viewMode by rememberSaveable { mutableStateOf(ProductViewMode.GRID) }
    
    // Snackbar for stock error messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show stock error message
    LaunchedEffect(uiState.stockErrorMessage) {
        uiState.stockErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStockErrorMessage()
        }
    }
    
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
                    // Product found - navigate to detail (always go to ProductDetailScreen when scanned)
                    // Clear processedBarcode before navigation to prevent re-trigger
                    processedBarcode = null
                    onProductClickFromScan(foundProduct.id, foundProduct.name)
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF2C2C2C),
                    contentColor = Color.White
                )
            }
        },
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
                    IconButton(onClick = { 
                        viewMode = if (viewMode == ProductViewMode.GRID) {
                            ProductViewMode.LIST
                        } else {
                            ProductViewMode.GRID
                        }
                    }) {
                        Icon(
                            imageVector = if (viewMode == ProductViewMode.GRID) {
                                Icons.Outlined.ViewList
                            } else {
                                Icons.Outlined.GridView
                            },
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
                    uiState.isLoading && uiState.errorMessage.isNullOrBlank() -> {
                        // Show skeleton loading when loading (regardless of whether we have old data)
                        val columnsCount = getProductGridColumns()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Show skeleton for 2-3 categories
                            repeat(3) { categoryIndex ->
                                // Category header skeleton
                                item(key = "skeleton_category_$categoryIndex") {
                                    SkeletonText(
                                        modifier = Modifier
                                            .fillMaxWidth(0.3f)
                                            .height(24.dp)
                                            .padding(bottom = 8.dp)
                                    )
                                }
                                
                                // Products skeleton
                                item(key = "skeleton_products_$categoryIndex") {
                                    if (viewMode == ProductViewMode.GRID) {
                                        // Grid skeleton
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Show 2 rows of skeleton products
                                            repeat(2) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    repeat(columnsCount) {
                                                        SkeletonProductCard(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // List skeleton
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            repeat(3) {
                                                SkeletonProductListItem(
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    !hasProducts && !uiState.isLoading -> {
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
                        
                        // Get dynamic column count based on device and orientation
                        val columnsCount = getProductGridColumns()
                    
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
                            
                            // Products grid/list for this category based on viewMode
                            item(key = "products_${category.id}") {
                                if (viewMode == ProductViewMode.GRID) {
                                    // Grid View
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        products.chunked(columnsCount).forEach { rowProducts ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowProducts.forEach { product ->
                                                    val cartQuantity = cartItems
                                                        .filter { it.productId == product.id }
                                                        .sumOf { it.quantity }
                                                    val isExpanded = uiState.expandedProductId == product.id
                                                    
                                                    ProductCard(
                                                        product = product,
                                                        cartQuantity = cartQuantity,
                                                        isExpanded = isExpanded,
                                                        onClick = { 
                                                            onProductClick(product.id, product.name, cartQuantity > 0)
                                                        },
                                                        onAddToCart = {
                                                            // Check if product has additional options
                                                            if (product.hasAdditionalOptions == true) {
                                                                // Has options -> navigate to product detail
                                                                onProductClick(product.id, product.name, cartQuantity > 0)
                                                            } else {
                                                                // No options and not in cart -> add to cart directly
                                                                if (cartQuantity == 0) {
                                                                    viewModel.addQuickToCart(product)
                                                                } else {
                                                                    // Already in cart -> show quantity adjuster
                                                                    viewModel.showQuantityAdjuster(product.id)
                                                                }
                                                            }
                                                        },
                                                        onIncrease = {
                                                            viewModel.increaseQuantity(product)
                                                        },
                                                        onDecrease = {
                                                            viewModel.decreaseQuantity(product)
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxWidth()
                                                    )
                                                }
                                                // Add spacers for remaining columns
                                                repeat(columnsCount - rowProducts.size) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // List View
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        products.forEach { product ->
                                            val cartQuantity = cartItems
                                                .filter { it.productId == product.id }
                                                .sumOf { it.quantity }
                                            val isExpanded = uiState.expandedProductId == product.id
                                            
                                            ProductListItem(
                                                product = product,
                                                cartQuantity = cartQuantity,
                                                isExpanded = isExpanded,
                                                onClick = { 
                                                    onProductClick(product.id, product.name, cartQuantity > 0)
                                                },
                                                onAddToCart = {
                                                    // Check if product has additional options
                                                    if (product.hasAdditionalOptions == true) {
                                                        // Has options -> navigate to product detail
                                                        onProductClick(product.id, product.name, cartQuantity > 0)
                                                    } else {
                                                        // No options and not in cart -> add to cart directly
                                                        if (cartQuantity == 0) {
                                                            viewModel.addQuickToCart(product)
                                                        } else {
                                                            // Already in cart -> show quantity adjuster
                                                            viewModel.showQuantityAdjuster(product.id)
                                                        }
                                                    }
                                                },
                                                onIncrease = {
                                                    viewModel.increaseQuantity(product)
                                                },
                                                onDecrease = {
                                                    viewModel.decreaseQuantity(product)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
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
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    onAddToCart: () -> Unit = {},
    onIncrease: () -> Unit = {},
    onDecrease: () -> Unit = {},
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
                    
                    val haptic = LocalHapticFeedback.current
                    
                    // Animated transition between collapsed button and expanded adjuster
                    AnimatedContent(
                        targetState = isExpanded && cartQuantity > 0,
                        transitionSpec = {
                            // Smooth scale + expand animation
                            scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialScale = 0.8f
                            ) + expandHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                expandFrom = Alignment.End
                            ) togetherWith
                            scaleOut(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                targetScale = 0.8f
                            ) + shrinkHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                shrinkTowards = Alignment.End
                            ) using
                            SizeTransform(clip = false)
                        },
                        label = "quantity_adjuster"
                    ) { expanded ->
                        if (expanded) {
                            // Expanded: Full quantity adjuster
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                            ) {
                                // Minus button
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDecrease()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "−",
                                        style = FontUtils.mainFont(
                                            style = AppFontStyle.Bold,
                                            size = FontSize.Large
                                        ),
                                        color = PrimaryButton
                                    )
                                }
                                
                                // Quantity with scale animation
                                Text(
                                    text = cartQuantity.toString(),
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Small
                                    ),
                                    color = PrimaryText,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                )
                                
                                // Plus button
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryButton)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onIncrease()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Increase",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            // Collapsed: Single add button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PrimaryButton)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onAddToCart()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cartQuantity > 0) {
                                    Text(
                                        text = cartQuantity.toString(),
                                        style = FontUtils.mainFont(
                                            style = AppFontStyle.Bold,
                                            size = FontSize.Small
                                        ),
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add to cart",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: ProductEntity,
    cartQuantity: Int = 0,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    onAddToCart: () -> Unit = {},
    onIncrease: () -> Unit = {},
    onDecrease: () -> Unit = {},
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // Product image or color - Left side (square)
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(backgroundColor)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.size(50.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            // Product info - Right side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product name and price
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
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
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = formatCurrency(product.price),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
                
                val haptic = LocalHapticFeedback.current
                
                // Animated transition between collapsed button and expanded adjuster
                AnimatedContent(
                    targetState = isExpanded && cartQuantity > 0,
                    transitionSpec = {
                        scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialScale = 0.8f
                        ) + expandHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            expandFrom = Alignment.End
                        ) togetherWith
                        scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            targetScale = 0.8f
                        ) + shrinkHorizontally(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            shrinkTowards = Alignment.End
                        ) using
                        SizeTransform(clip = false)
                    },
                    label = "quantity_adjuster_list"
                ) { expanded ->
                    if (expanded) {
                        // Expanded: Full quantity adjuster
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                        ) {
                            // Minus button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDecrease()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Large
                                    ),
                                    color = PrimaryButton
                                )
                            }
                            
                            // Quantity
                            Text(
                                text = cartQuantity.toString(),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Bold,
                                    size = FontSize.Medium
                                ),
                                color = PrimaryText,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                            )
                            
                            // Plus button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PrimaryButton)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onIncrease()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Increase",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Collapsed: Single add button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(PrimaryButton)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAddToCart()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (cartQuantity > 0) {
                                Text(
                                    text = cartQuantity.toString(),
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Medium
                                    ),
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add to cart",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

@Composable
private fun SkeletonText(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE0E0E0))
            .alpha(alpha)
    )
}

@Composable
private fun SkeletonProductCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_card")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_card_alpha"
    )
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color(0xFFE0E0E0))
                    .alpha(alpha)
            )
            
            // Content skeleton
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Product name skeleton
                SkeletonText(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Price and button skeleton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price skeleton
                    SkeletonText(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                    )
                    
                    // Button skeleton
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE0E0E0))
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonProductListItem(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_list_item")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_list_item_alpha"
    )
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // Image skeleton
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(Color(0xFFE0E0E0))
                    .alpha(alpha)
            )
            
            // Content skeleton
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Product name skeleton
                    SkeletonText(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(18.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Price skeleton
                    SkeletonText(
                        modifier = Modifier
                            .width(80.dp)
                            .height(18.dp)
                    )
                }
                
                // Button skeleton
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE0E0E0))
                        .alpha(alpha)
                )
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

