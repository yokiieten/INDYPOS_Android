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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    onProductClick: (String) -> Unit = {},
    viewModel: MainProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    
    // Load products when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadProducts()
    }
    
    // Track which category is visible based on scroll position
    val visibleCategoryId = remember {
        derivedStateOf {
            if (uiState.allProducts.isEmpty() || scrollState.layoutInfo.visibleItemsInfo.isEmpty()) {
                return@derivedStateOf null
            }
            
            // Group all products by category
            val productsByCategory = uiState.allProducts.groupBy { it.categoryId }
            val sortedCategories = productsByCategory.toList().sortedBy { (categoryId, _) ->
                uiState.categories.find { it.id == categoryId }?.sortOrder ?: Int.MAX_VALUE
            }
            
            // Get the first visible item index
            val firstVisibleIndex = scrollState.firstVisibleItemIndex
            
            // Calculate which category is currently visible
            var currentIndex = 0
            for ((categoryId, products) in sortedCategories) {
                // Each category section has: 1 header item + 1 grid container item
                val gridRows = (products.size + 1) / 2 // 2 columns per row
                val sectionSize = 1 + gridRows // header + grid rows
                
                if (firstVisibleIndex < currentIndex + sectionSize) {
                    return@derivedStateOf categoryId
                }
                currentIndex += sectionSize
            }
            
            // Default to last category if scrolled to bottom
            sortedCategories.lastOrNull()?.first
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
    LaunchedEffect(uiState.selectedCategoryId) {
        uiState.selectedCategoryId?.let { categoryId ->
            // Find the position of this category in the list
            val productsByCategory = uiState.allProducts.groupBy { it.categoryId }
            val sortedCategories = productsByCategory.toList().sortedBy { (catId, _) ->
                uiState.categories.find { it.id == catId }?.sortOrder ?: Int.MAX_VALUE
            }
            
            // Each category has 2 items: header (index 0) and products grid (index 1)
            // So category indices are: 0, 2, 4, 6, ...
            var targetIndex = 0
            for ((catId, _) in sortedCategories) {
                if (catId == categoryId) {
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
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "สินค้า",
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
                            contentDescription = "กลับ",
                            tint = PrimaryText
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "ค้นหา",
                            tint = PrimaryText
                        )
                    }
                    IconButton(onClick = { /* TODO: Grid/List toggle */ }) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "เปลี่ยนมุมมอง",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter bar at top - only show categories that have products
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
            if (uiState.isLoading && uiState.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryButton)
                }
            } else if (uiState.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ไม่มีสินค้า",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                }
            } else {
                // Always show all products grouped by category
                // Filtering is handled by scrolling to the selected category
                val allProductsGrouped = uiState.allProducts.groupBy { it.categoryId }
                
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (allProductsGrouped.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ไม่มีสินค้า",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Medium
                                    ),
                                    color = SecondaryText
                                )
                            }
                        }
                    } else {
                        // Show products grouped by category
                        // If a category is selected, we still show all but could scroll to it
                        allProductsGrouped.toList().sortedBy { (categoryId, _) ->
                            uiState.categories.find { it.id == categoryId }?.sortOrder ?: Int.MAX_VALUE
                        }.forEach { (categoryId, products) ->
                            val category = uiState.categories.find { it.id == categoryId }
                            
                            item(key = "category_$categoryId") {
                                // Category header
                                Text(
                                    text = category?.name ?: "ไม่มีหมวดหมู่",
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Bold,
                                        size = FontSize.Large
                                    ),
                                    color = PrimaryText,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            // Products grid for this category
                            item(key = "products_$categoryId") {
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
                                                ProductCard(
                                                    product = product,
                                                    onClick = { onProductClick(product.id) },
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
    }
}

@Composable
private fun CategoryFilterBar(
    categories: List<CategoryEntity>,
    focusedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
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
                    .background(backgroundColor),
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
                
                Text(
                    text = formatCurrency(product.price),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Small
                    ),
                    color = PrimaryButton
                )
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

