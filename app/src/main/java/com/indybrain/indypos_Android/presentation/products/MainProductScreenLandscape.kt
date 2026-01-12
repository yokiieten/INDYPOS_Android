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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.ui.getLandscapeSplitRatio
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import com.indybrain.indypos_Android.domain.usecase.GetGroupedCartItemsUseCase
import com.indybrain.indypos_Android.presentation.products.components.LiveCartPanel
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Landscape layout for MainProductScreen with split screen:
 * - Left side (60-70%): Product grid with 3 columns
 * - Right side (30-40%): Live cart panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainProductScreenLandscape(
    onBackClick: () -> Unit = {},
    onProductClick: (productId: String, productName: String, isInCart: Boolean) -> Unit = { _, _, _ -> },
    onProductClickFromScan: (productId: String, productName: String) -> Unit = { _, _ -> },
    onCartClick: () -> Unit = {},
    onBarcodeScannerClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    scannedBarcode: String? = null,
    viewModel: MainProductViewModel = hiltViewModel(),
    getGroupedCartItemsUseCase: GetGroupedCartItemsUseCase = hiltViewModel<LandscapeCartViewModel>().getGroupedCartItemsUseCase
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // View mode state
    var viewMode by rememberSaveable { mutableStateOf(ProductViewMode.GRID) }
    
    // Get grouped cart items for the live cart panel
    val groupedCartItems by getGroupedCartItemsUseCase().collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Calculate totals using the same logic as OrderProductViewModel
    val subtotal = remember(groupedCartItems) {
        groupedCartItems.sumOf { groupedItem ->
            val firstItem = groupedItem.items.firstOrNull() ?: return@sumOf 0.0
            val productPrice = firstItem.product.price * groupedItem.totalQuantity
            val addonsPrice = firstItem.selectedAddons.values
                .flatten()
                .sumOf { it.price } * groupedItem.totalQuantity
            productPrice + addonsPrice
        }
    }
    val discount = 0.0 // No discount in product selection screen
    val total = (subtotal - discount).coerceAtLeast(0.0)
    
    // Handle scanned barcode
    var showProductNotFoundDialog by remember { mutableStateOf(false) }
    var processedBarcode by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let { barcode ->
            if (barcode != processedBarcode) {
                processedBarcode = barcode
                kotlinx.coroutines.delay(100)
                val product: ProductEntity? = viewModel.findProductByCode(barcode)
                product?.let { foundProduct ->
                    processedBarcode = null
                    onProductClickFromScan(foundProduct.id, foundProduct.name)
                } ?: run {
                    showProductNotFoundDialog = true
                    processedBarcode = null
                }
            }
        } ?: run {
            processedBarcode = null
        }
    }
    
    // Get split ratio based on screen size
    val splitRatio = getLandscapeSplitRatio()
    
    // Save scroll position
    var savedScrollIndex by rememberSaveable { mutableStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableStateOf(0) }
    
    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedScrollIndex,
        initialFirstVisibleItemScrollOffset = savedScrollOffset
    )
    
    LaunchedEffect(scrollState.firstVisibleItemIndex, scrollState.firstVisibleItemScrollOffset) {
        savedScrollIndex = scrollState.firstVisibleItemIndex
        savedScrollOffset = scrollState.firstVisibleItemScrollOffset
    }
    
    // Track visible category
    val visibleCategoryId = remember {
        derivedStateOf {
            val categoriesWithProducts = uiState.categories.filter { category ->
                uiState.allProducts.any { it.categoryId == category.id }
            }
            
            if (categoriesWithProducts.isEmpty() || scrollState.layoutInfo.visibleItemsInfo.isEmpty()) {
                return@derivedStateOf null
            }
            
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            
            for (visibleItem in visibleItems) {
                val itemKey = visibleItem.key as? String
                if (itemKey != null) {
                    when {
                        itemKey.startsWith("category_") -> {
                            return@derivedStateOf itemKey.removePrefix("category_")
                        }
                        itemKey.startsWith("products_") -> {
                            return@derivedStateOf itemKey.removePrefix("products_")
                        }
                    }
                }
            }
            
            val firstVisibleIndex = scrollState.firstVisibleItemIndex
            val categoryIndex = firstVisibleIndex / 2
            
            if (categoryIndex < categoriesWithProducts.size) {
                return@derivedStateOf categoriesWithProducts[categoryIndex].id
            }
            
            categoriesWithProducts.lastOrNull()?.id
        }
    }
    
    LaunchedEffect(visibleCategoryId.value) {
        visibleCategoryId.value?.let { categoryId ->
            if (categoryId != uiState.focusedCategoryId) {
                viewModel.updateFocusedCategory(categoryId)
            }
        }
    }
    
    var lastScrolledCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.selectedCategoryId) {
        val categoryId = uiState.selectedCategoryId
        if (categoryId != null && categoryId != lastScrolledCategoryId) {
            val categoriesWithProducts = uiState.categories.filter { category ->
                uiState.allProducts.any { it.categoryId == category.id }
            }
            
            var targetIndex = 0
            for (category in categoriesWithProducts) {
                if (category.id == categoryId) {
                    scrollState.animateScrollToItem(
                        index = targetIndex,
                        scrollOffset = -16
                    )
                    break
                }
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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Left side: Product grid
            Box(
                modifier = Modifier
                    .weight(splitRatio)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category filter bar
                    val categoriesWithProducts = uiState.categories.filter { category ->
                        uiState.allProducts.any { it.categoryId == category.id }
                    }
                    val validFocusedCategoryId = uiState.focusedCategoryId?.takeIf { categoryId ->
                        uiState.allProducts.any { it.categoryId == categoryId }
                    }
                    
                    CategoryFilterBarLandscape(
                        categories = categoriesWithProducts,
                        focusedCategoryId = validFocusedCategoryId,
                        onCategorySelected = { categoryId ->
                            viewModel.selectCategory(categoryId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Products grid
                    val hasProducts = uiState.allProducts.isNotEmpty()
                    
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryButton)
                            }
                        }
                        !hasProducts -> {
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
                            val productsByCategoryId = uiState.allProducts
                                .filter { it.categoryId != null && it.categoryId.isNotBlank() }
                                .groupBy { it.categoryId }
                            
                            LazyColumn(
                                state = scrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 8.dp,
                                    top = 8.dp,
                                    bottom = 16.dp
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
                                    categoriesWithProducts.forEach { category ->
                                        val products = productsByCategoryId[category.id] ?: emptyList()
                                        
                                        if (products.isEmpty()) return@forEach
                                        
                                        item(key = "category_${category.id}") {
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
                                        
                                        item(key = "products_${category.id}") {
                                            if (viewMode == ProductViewMode.GRID) {
                                                // Grid View
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    // Display 3 columns in landscape
                                                    products.chunked(3).forEach { rowProducts ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            rowProducts.forEach { product ->
                                                                val cartQuantity = cartItems
                                                                    .filter { it.productId == product.id }
                                                                    .sumOf { it.quantity }
                                                                
                                                                ProductCardLandscape(
                                                                    product = product,
                                                                    cartQuantity = cartQuantity,
                                                                    onClick = {
                                                                        onProductClick(product.id, product.name, cartQuantity > 0)
                                                                    },
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .fillMaxWidth()
                                                                )
                                                            }
                                                            // Add spacers for remaining columns
                                                            repeat(3 - rowProducts.size) {
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
                                                        
                                                        ProductListItemLandscape(
                                                            product = product,
                                                            cartQuantity = cartQuantity,
                                                            onClick = {
                                                                onProductClick(product.id, product.name, cartQuantity > 0)
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = error,
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Small
                                ),
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
            
            // Divider
            Divider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFFE5E5E5)
            )
            
            // Right side: Live cart panel
            Box(
                modifier = Modifier
                    .weight(1f - splitRatio)
                    .fillMaxHeight()
            ) {
                LiveCartPanel(
                    groupedItems = groupedCartItems,
                    subtotal = subtotal,
                    discount = discount,
                    total = total,
                    onCartItemClick = { productId, productName ->
                        // Navigate to edit cart item
                        val cartQuantity = cartItems.count { it.productId == productId }
                        onProductClick(productId, productName, cartQuantity > 0)
                    },
                    onCheckoutClick = onCartClick
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
private fun CategoryFilterBarLandscape(
    categories: List<CategoryEntity>,
    focusedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryScrollState = rememberLazyListState()
    
    LaunchedEffect(focusedCategoryId) {
        focusedCategoryId?.let { categoryId ->
            val categoryIndex = categories.indexOfFirst { it.id == categoryId }
            if (categoryIndex >= 0) {
                val layoutInfo = categoryScrollState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val isVisible = visibleItems.any { it.index == categoryIndex }
                
                if (!isVisible) {
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
        items(categories) { category ->
            CategoryChipLandscape(
                text = category.name,
                isFocused = focusedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryChipLandscape(
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
private fun ProductListItemLandscape(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            // Product image or color - Left side (square)
            Box(
                modifier = Modifier
                    .width(90.dp)
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
                            modifier = Modifier.size(45.dp),
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
                
                // Cart Badge - Right side
                if (cartQuantity > 0) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
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

@Composable
private fun ProductCardLandscape(
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
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
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
                    
                    if (cartQuantity > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(PrimaryButton),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cartQuantity.toString(),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Bold,
                                    size = FontSize.Smallest
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

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

/**
 * Helper ViewModel to inject GetGroupedCartItemsUseCase for landscape mode
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class LandscapeCartViewModel @javax.inject.Inject constructor(
    val getGroupedCartItemsUseCase: GetGroupedCartItemsUseCase
) : androidx.lifecycle.ViewModel()
