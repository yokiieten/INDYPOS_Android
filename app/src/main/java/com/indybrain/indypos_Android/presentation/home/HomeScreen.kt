package com.indybrain.indypos_Android.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.ui.components.ShopTopAppBar
import com.indybrain.indypos_Android.presentation.graph.GraphScreen
import com.indybrain.indypos_Android.presentation.navigation.HomeBottomDestination
import com.indybrain.indypos_Android.presentation.order.OrderScreen
import com.indybrain.indypos_Android.presentation.settings.SettingsScreen
import com.indybrain.indypos_Android.presentation.settings.SettingsItem
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToMainProduct: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {},
    onNavigateToLanguageSettings: () -> Unit = {},
    onNavigateToAccountSettings: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToOrderSettings: () -> Unit = {},
    onNavigateToStockManagement: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToContactUs: () -> Unit = {},
    onNavigateToReceiptSettings: () -> Unit = {},
    onNavigateToPrinterSettings: () -> Unit = {},
    onNavigateToOrderDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedDestination by rememberSaveable { mutableStateOf(HomeBottomDestination.Home) }
    val context = LocalContext.current
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateShopImage(it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { viewModel.updateShopImage(it) }
        }
    }
    
    // Fetch data when screen appears (like viewWillAppear in iOS)
    // This will trigger when:
    // 1. Screen first appears (selectedDestination is Home)
    // 2. User navigates back to Home tab from other tabs
    LaunchedEffect(selectedDestination) {
        if (selectedDestination == HomeBottomDestination.Home) {
            viewModel.refreshData()
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            if (selectedDestination != HomeBottomDestination.Charts && selectedDestination != HomeBottomDestination.Orders) {
                ShopTopAppBar(
                    shopName = uiState.shopName,
                    onEditClick = { viewModel.showEditStoreNameDialog() }
                )
            }
        },
        bottomBar = {
            HomeBottomBar(
                selected = selectedDestination,
                onSelected = { selectedDestination = it }
            )
        }
    ) { padding ->
        when (selectedDestination) {
            HomeBottomDestination.Charts -> {
                GraphScreen()
            }
            HomeBottomDestination.Orders -> {
                OrderScreen(
                    onOrderClick = onNavigateToOrderDetail
                )
            }
            HomeBottomDestination.Settings -> {
                SettingsScreen(
                    onSettingsItemClick = { item ->
                        when (item) {
                            SettingsItem.Account -> {
                                onNavigateToAccountSettings()
                            }
                            SettingsItem.Language -> {
                                onNavigateToLanguageSettings()
                            }
                            SettingsItem.ChangePassword -> {
                                onNavigateToChangePassword()
                            }
                            SettingsItem.SalesSettings -> {
                                onNavigateToOrderSettings()
                            }
                            SettingsItem.ManageStock -> {
                                onNavigateToStockManagement()
                            }
                            SettingsItem.ManageData -> {
                                onNavigateToDataManagement()
                            }
                            SettingsItem.ContactUs -> {
                                onNavigateToContactUs()
                            }
                            SettingsItem.ReceiptSettings -> {
                                onNavigateToReceiptSettings()
                            }
                            SettingsItem.PrinterSettings -> {
                                onNavigateToPrinterSettings()
                            }
                            // Handle other settings items here
                            else -> {}
                        }
                    },
                    onLogoutSuccess = onLogoutSuccess
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    ShopCoverCard(
                        shopImageUrl = uiState.shopImageUrl,
                        description = uiState.shopDescription,
                        onImageClick = { /* TODO: Show fullscreen image */ },
                        onChangeImageClick = { viewModel.showImagePicker() },
                        onDescriptionClick = { viewModel.showEditDescriptionDialog() }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    DailyOverviewSection(statistics = uiState.statistics)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    TopProductSection(statistics = uiState.statistics)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    ShortcutsSection(
                        shortcuts = uiState.shortcuts,
                        onShortcutClick = { shortcutId ->
                            when (shortcutId) {
                                "start_order" -> onNavigateToMainProduct()
                                // Add other shortcut handlers here
                            }
                        }
                    )
                }
            }
        }
        
        // Image Picker Dialog
        if (uiState.showImagePickerDialog) {
            ImagePickerDialog(
                onDismiss = { viewModel.dismissImagePicker() },
                onCameraClick = {
                    viewModel.dismissImagePicker()
                    try {
                        val photoFile = File(context.cacheDir, "temp_shop_photo.jpg")
                        val photoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                        cameraImageUri = photoUri
                        cameraLauncher.launch(photoUri)
                    } catch (e: Exception) {
                        // Fallback: open default camera intent if FileProvider fails
                        try {
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // Swallow; no-op if camera cannot open
                        }
                    }
                },
                onGalleryClick = {
                    viewModel.dismissImagePicker()
                    imagePickerLauncher.launch("image/*")
                }
            )
        }
        
        // Edit Store Name Dialog
        if (uiState.showEditStoreNameDialog) {
            EditStoreNameDialog(
                currentName = uiState.shopName,
                onDismiss = { viewModel.dismissEditStoreNameDialog() },
                onSave = { newName -> 
                    viewModel.updateStoreName(newName)
                    viewModel.dismissEditStoreNameDialog()
                }
            )
        }
        
        // Edit Description Dialog
        if (uiState.showEditDescriptionDialog) {
            EditDescriptionDialog(
                currentDescription = uiState.shopDescription,
                onDismiss = { viewModel.dismissEditDescriptionDialog() },
                onSave = { newDescription ->
                    viewModel.updateShopDescription(newDescription)
                    viewModel.dismissEditDescriptionDialog()
                }
            )
        }

        // Success popup
        uiState.successMessage?.let { msgResName ->
            val message = stringResource(
                id = when (msgResName) {
                    "home_store_description_updated" -> R.string.home_store_description_updated
                    "home_store_name_updated" -> R.string.home_store_name_updated
                    else -> R.string.home_success_title
                }
            )
            AlertDialog(
                onDismissRequest = { viewModel.clearSuccessMessage() },
                title = {
                    Text(
                        text = stringResource(id = R.string.home_success_title),
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
                    TextButton(onClick = { viewModel.clearSuccessMessage() }) {
                        Text(
                            text = stringResource(id = R.string.home_ok),
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
    }
}

@Composable
private fun ShopCoverCard(
    shopImageUrl: String?,
    description: String,
    onImageClick: () -> Unit,
    onChangeImageClick: () -> Unit,
    onDescriptionClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Store Image with 16:9 aspect ratio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F5F7))
                .clickable(onClick = onImageClick)
        ) {
            val imageUrl = shopImageUrl?.takeIf { it.isNotBlank() }
            if (!imageUrl.isNullOrBlank()) {
                val fullImageUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    imageUrl
                } else {
                    // Build shop image URL (similar to product images but for shop-images)
                    if (imageUrl.contains("://")) {
                        imageUrl
                    } else {
                        var path = imageUrl
                        if (!path.startsWith("/")) {
                            path = "/$path"
                        }
                        if (path.startsWith("/api/v1/files/shop-images/")) {
                            "${AppConfig.baseImageUrl}$path"
                        } else {
                            val cleanFile = if (path.startsWith("/")) path.drop(1) else path
                            "${AppConfig.baseImageUrl}/api/v1/files/shop-images/$cleanFile"
                        }
                    }
                }
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(fullImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.logo_appstore),
                    placeholder = painterResource(id = R.drawable.logo_appstore)
                )
            } else {
                // Placeholder
                Image(
                    painter = painterResource(id = R.drawable.logo_appstore),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp)
                )
            }
            
            // Edit Cover Image Button - positioned at bottom right
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clickable(onClick = onChangeImageClick),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF5EA6ED),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_eye),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.home_change_cover),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Small
                        ),
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Description Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDescriptionClick),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE5E5E5))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = description.ifBlank {
                        stringResource(id = R.string.home_description_placeholder)
                    },
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = if (description.isBlank()) PlaceholderText else PrimaryText,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.home_edit_shop_cd),
                    tint = PlaceholderText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyOverviewSection(statistics: HomeStatistics) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.home_daily_sales_title),
            style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = stringResource(id = R.string.home_daily_sales_label),
                value = "${formatCurrency(statistics.todaysSales)} ${stringResource(id = R.string.home_currency_suffix)}",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = stringResource(id = R.string.home_orders_today_label),
                value = "${statistics.ordersToday} ${stringResource(id = R.string.home_orders_unit)}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Smaller),
                color = PrimaryText.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Large),
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun TopProductSection(statistics: HomeStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(id = R.string.home_best_seller_title),
                style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Smaller),
                color = PrimaryText.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (statistics.topProductName.isBlank()) {
                Text(
                    text = stringResource(id = R.string.home_best_seller_empty),
                    style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Large),
                    color = SecondaryText
                )
            } else {
                val qty = "${statistics.topProductQuantity} ${stringResource(id = R.string.home_orders_unit)}"
                val amount = "${formatCurrency(statistics.topProductAmount)} ${stringResource(id = R.string.home_currency_suffix)}"
                val displayText = buildAnnotatedString {
                    // Product name in blue (SecondaryText)
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = SecondaryText)) {
                        append(statistics.topProductName)
                    }
                    append(" ")
                    // Count and amount in dark gray (PrimaryText)
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = PrimaryText)) {
                        append("$qty ($amount)")
                    }
                }
                Text(
                    text = displayText,
                    style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Large)
                )
            }
        }
    }
}

@Composable
private fun ShortcutsSection(
    shortcuts: List<HomeShortcut>,
    onShortcutClick: (String) -> Unit
) {
    Text(
        text = stringResource(id = R.string.home_shortcuts_title),
        style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
        color = PrimaryText
    )
    Spacer(modifier = Modifier.height(12.dp))
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        shortcuts.forEachIndexed { index, shortcut ->
            ShortcutListItem(
                shortcut = shortcut,
                onClick = { onShortcutClick(shortcut.id) },
                showDivider = index < shortcuts.size - 1
            )
        }
    }
}

@Composable
private fun ShortcutListItem(
    shortcut: HomeShortcut,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container - Light gray rounded square
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFF1F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = shortcut.icon,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = shortcut.title,
                    style = FontUtils.mainFont(style = AppFontStyle.Medium, size = FontSize.Medium),
                    color = PrimaryText
                )
                Text(
                    text = shortcut.subtitle,
                    style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Small),
                    color = PrimaryText.copy(alpha = 0.6f)
                )
            }
            
            // Chevron Arrow
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = PrimaryText.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
        
        // Separator
        if (showDivider) {
            Divider(
                color = Color(0xFFE5E5E5),
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 68.dp) // Align with text (44 icon + 12 spacing + 12 padding)
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    selected: HomeBottomDestination,
    onSelected: (HomeBottomDestination) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        HomeBottomDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (destination == selected) destination.selectedIcon else destination.icon,
                        contentDescription = stringResource(destination.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = FontUtils.mainFont(
                            style = if (destination == selected) AppFontStyle.Bold else AppFontStyle.Regular,
                            size = FontSize.Smallest
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White, // Background สีขาวเมื่อเลือก
                    selectedIconColor = PrimaryText, // ไอคอนสีดำเมื่อเลือก
                    selectedTextColor = PrimaryText, // ข้อความสีดำเมื่อเลือก
                    unselectedIconColor = PlaceholderText, // ไอคอนสีเทาเมื่อไม่เลือก
                    unselectedTextColor = PlaceholderText // ข้อความสีเทาเมื่อไม่เลือก
                )
            )
        }
    }
}


private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(value)
}

/**
 * Image Picker Dialog
 */
@Composable
private fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.home_edit_cover_image),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Column {
                TextButton(
                    onClick = onCameraClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.home_take_photo),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                }
                TextButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.home_choose_from_gallery),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.home_cancel),
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

/**
 * Edit Store Name Dialog
 */
@Composable
private fun EditStoreNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var storeName by remember { mutableStateOf(TextFieldValue(currentName)) }
    val placeholderText = stringResource(id = R.string.home_store_name_placeholder)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.home_edit_store_name_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text(placeholderText) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = storeName.text.trim()
                    if (trimmed.isNotEmpty()) {
                        onSave(trimmed)
                    }
                }
            ) {
                Text(
                    text = stringResource(id = R.string.home_save),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.home_cancel),
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

/**
 * Edit Description Dialog
 */
@Composable
private fun EditDescriptionDialog(
    currentDescription: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var description by remember { mutableStateOf(TextFieldValue(currentDescription)) }
    val placeholderText = stringResource(id = R.string.home_description_placeholder)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.home_edit_description_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(placeholderText) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(description.text.trim())
                }
            ) {
                Text(
                    text = stringResource(id = R.string.home_save),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.home_cancel),
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

