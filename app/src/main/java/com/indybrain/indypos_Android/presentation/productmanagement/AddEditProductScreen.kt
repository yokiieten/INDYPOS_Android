package com.indybrain.indypos_Android.presentation.productmanagement

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import com.indybrain.indypos_Android.core.config.AppConfig

/**
 * Add/Edit Product Screen
 * @param productId If provided, screen is in edit mode. If null, screen is in add mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productId: String? = null,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    onBarcodeScannerClick: () -> Unit = {},
    scannedBarcode: String? = null,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isEditMode = productId != null
    val context = LocalContext.current
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var processedBarcode by remember { mutableStateOf<String?>(null) }
    
    // Load product data if in edit mode
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
        }
    }
    
    // Handle scanned barcode
    // Wait for loading to complete in edit mode, then update productCode
    LaunchedEffect(scannedBarcode, uiState.isLoading) {
        scannedBarcode?.let { barcode ->
            // Only process if not already processed
            if (barcode != processedBarcode) {
                // In edit mode, wait for loading to complete before updating
                if (isEditMode) {
                    // Wait for loadProduct to finish (isLoading becomes false)
                    if (uiState.isLoading) {
                        return@LaunchedEffect
                    }
                    // Small delay to ensure state is stable
                    kotlinx.coroutines.delay(100)
                }
                processedBarcode = barcode
                viewModel.updateProductCode(barcode)
            }
        } ?: run {
            // Clear processedBarcode when scannedBarcode is null
            processedBarcode = null
        }
    }
    
    // Image picker launcher (Gallery)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Convert URI to string and update image URL
            viewModel.updateImageUrl(it.toString())
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            // Image captured successfully
            cameraImageUri?.let { uri ->
                // Verify the URI is accessible before updating
                try {
                    val uriString = uri.toString()
                    // For FileProvider URIs (content://), check if we can read it
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        inputStream.close()
                        // URI is accessible, update image URL
                        viewModel.updateImageUrl(uriString)
                    } else {
                        android.util.Log.e("AddEditProduct", "Cannot read image from URI: $uriString")
                    }
                } catch (e: Exception) {
                    // Handle error - log it but still try to update
                    android.util.Log.e("AddEditProduct", "Error handling camera image: ${e.message}")
                    // Still try to update the URI - might work for display
                    viewModel.updateImageUrl(uri.toString())
                }
            }
        } else {
            android.util.Log.d("AddEditProduct", "Camera capture failed or URI is null. success=$success, uri=$cameraImageUri")
        }
    }
    
    // Success dialog will handle navigation when user clicks OK button
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) 
                            "แก้ไขสินค้า"
                        else 
                            stringResource(id = R.string.product_management_add_product),
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
            if (uiState.isLoading && isEditMode) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Product Name
                    FormFieldLabel("ชื่อสินค้า", required = true)
                    OutlinedTextField(
                        value = uiState.productName,
                        onValueChange = { viewModel.updateProductName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("กรุณากรอกชื่อสินค้า", color = PlaceholderText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                            focusedBorderColor = PrimaryButton
                        ),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Product Code
                    FormFieldLabel("รหัสสินค้า", required = true)
                    OutlinedTextField(
                        value = uiState.productCode,
                        onValueChange = { viewModel.updateProductCode(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("กรุณากรอกรหัสสินค้า", color = PlaceholderText) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = onBarcodeScannerClick) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "สแกนบาร์โค้ด",
                                    tint = SecondaryText
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                            focusedBorderColor = PrimaryButton
                        ),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Selling Price
                    FormFieldLabel("ราคาขาย", required = true)
                    var sellingPriceFocused by rememberSaveable { mutableStateOf(false) }
                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(
                        value = if (sellingPriceFocused) {
                            uiState.sellingPrice
                        } else {
                            formatPriceForDisplay(uiState.sellingPrice)
                        },
                        onValueChange = { viewModel.updateSellingPrice(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                val wasFocused = sellingPriceFocused
                                sellingPriceFocused = focusState.isFocused
                                // When losing focus, format the price
                                if (wasFocused && !focusState.isFocused) {
                                    viewModel.formatSellingPriceOnUnfocus()
                                }
                            },
                        placeholder = { Text("0", color = PlaceholderText) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                // Format price when Done is pressed
                                viewModel.formatSellingPriceOnUnfocus()
                                // Clear focus to show formatted value
                                focusManager.clearFocus()
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                            focusedBorderColor = PrimaryButton
                        ),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cost Price
                    FormFieldLabel("ราคาต้นทุน", required = false)
                    var costPriceFocused by rememberSaveable { mutableStateOf(false) }
                    val focusManagerCost = LocalFocusManager.current
                    OutlinedTextField(
                        value = if (costPriceFocused) {
                            uiState.costPrice
                        } else {
                            formatPriceForDisplay(uiState.costPrice)
                        },
                        onValueChange = { viewModel.updateCostPrice(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                val wasFocused = costPriceFocused
                                costPriceFocused = focusState.isFocused
                                // When losing focus, format the price
                                if (wasFocused && !focusState.isFocused) {
                                    viewModel.formatCostPriceOnUnfocus()
                                }
                            },
                        placeholder = { Text("0 (ไม่บังคับ)", color = PlaceholderText) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                // Format price when Done is pressed
                                viewModel.formatCostPriceOnUnfocus()
                                // Clear focus to show formatted value
                                focusManagerCost.clearFocus()
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                            focusedBorderColor = PrimaryButton
                        ),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Unit
                    FormFieldLabel("หน่วยนับ", required = true)
                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = { viewModel.updateUnit(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("เช่น ชิ้น, แพ็ค, กล่อง", color = PlaceholderText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                            focusedBorderColor = PrimaryButton
                        ),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Product Image/Color
                    FormFieldLabel("ภาพสินค้า", required = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                viewModel.updateImageUrl(null)
                            }
                        ) {
                            RadioButton(
                                selected = uiState.isImageSelected,
                                onClick = { 
                                    viewModel.updateImageUrl(null)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryButton
                                )
                            )
                            Text("รูปภาพ", style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Medium))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                viewModel.updateSelectedColorHex(null)
                            }
                        ) {
                            RadioButton(
                                selected = !uiState.isImageSelected,
                                onClick = { 
                                    viewModel.updateSelectedColorHex(null)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryButton
                                )
                            )
                            Text("สี", style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Medium))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (uiState.isImageSelected) {
                        // Show image picker or selected image
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showImagePickerDialog = true },
                            color = Color(0xFFE3F2FD)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                val imageUrl = uiState.imageUrl?.takeIf { it.isNotBlank() }
                                if (!imageUrl.isNullOrBlank()) {
                                    // Determine if it's a local URI or a server URL
                                    val imageData = if (imageUrl.startsWith("content://") || imageUrl.startsWith("file://")) {
                                        // Local URI - use Uri object directly
                                        Uri.parse(imageUrl)
                                    } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                                        // Already a full URL
                                        imageUrl
                                    } else {
                                        // Relative path - build full URL
                                        AppConfig.buildImageUrl(imageUrl)
                                    }
                                    
                                    // Show selected image
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(imageData)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "รูปภาพสินค้า",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(id = R.drawable.logo_appstore),
                                        placeholder = painterResource(id = R.drawable.logo_appstore)
                                    )
                                } else {
                                    // Show placeholder text
                                    Text(
                                        text = "เลือกรูปภาพ",
                                        style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Medium),
                                        color = PrimaryButton
                                    )
                                }
                            }
                        }
                    } else {
                        // Show color picker grid
                        ColorPickerGrid(
                            selectedColorHex = uiState.selectedColorHex,
                            onColorSelected = { colorHex ->
                                viewModel.updateSelectedColorHex(colorHex)
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormFieldLabel("หมวดหมู่", required = true, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.showAddCategoryDialog() }) {
                            Text("+เพิ่ม", color = PrimaryButton)
                        }
                    }
                    CategoryDropdown(
                        categories = categories,
                        selectedCategoryId = uiState.categoryId,
                        onCategorySelected = { viewModel.updateCategory(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // SKU Enabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "สินค้า SKU",
                            style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Medium),
                            color = PrimaryText
                        )
                        Switch(
                            checked = uiState.isSkuEnabled,
                            onCheckedChange = { viewModel.updateSkuEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryButton
                            )
                        )
                    }
                    
                    // SKU Code (if enabled)
                    if (uiState.isSkuEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FormFieldLabel("รหัส SKU", required = false)
                        OutlinedTextField(
                            value = uiState.skuCode,
                            onValueChange = { viewModel.updateSkuCode(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("กรอกรหัส SKU", color = PlaceholderText) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                                focusedBorderColor = PrimaryButton
                            ),
                            enabled = !uiState.isLoading
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stock Enabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "สต็อกสินค้า",
                            style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Medium),
                            color = PrimaryText
                        )
                        Switch(
                            checked = uiState.isStockEnabled,
                            onCheckedChange = { viewModel.updateStockEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryButton
                            )
                        )
                    }
                    
                    // Stock Quantity (if enabled)
                    if (uiState.isStockEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FormFieldLabel("จำนวนสินค้า", required = false)
                        OutlinedTextField(
                            value = uiState.stockQuantity,
                            onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.updateStockQuantity(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("กรอกจำนวนสินค้า", color = PlaceholderText) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                                focusedBorderColor = PrimaryButton
                            ),
                            enabled = !uiState.isLoading
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Additional Options Enabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "มีออปชั่นเพิ่มเติม",
                            style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Medium),
                            color = PrimaryText
                        )
                        Switch(
                            checked = uiState.hasAdditionalOptions,
                            onCheckedChange = { viewModel.updateAdditionalOptionsEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryButton
                            )
                        )
                    }
                    
                    // AddOn Groups (if enabled)
                    if (uiState.hasAdditionalOptions) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FormFieldLabel("เลือก AddOn Groups", required = false)
                        
                        if (uiState.availableAddonGroups.isEmpty()) {
                            Text(
                                text = "ไม่มี AddOn Groups ที่สามารถเลือกได้",
                                style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Small),
                                color = SecondaryText,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.availableAddonGroups.forEach { addonGroup ->
                                AddonGroupSelectionItem(
                                    addonGroup = addonGroup,
                                    isSelected = uiState.addonGroupIds.contains(addonGroup.id),
                                    onToggle = { viewModel.toggleAddonGroupSelection(addonGroup.id) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp)) // Space for save button
                }
            }
            
            // Save Button - Fixed at bottom
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        enabled = !uiState.isLoading,
                        onClick = { viewModel.saveProduct {} }
                    ),
                color = if (uiState.isLoading) 
                    PrimaryButton.copy(alpha = 0.6f) 
                else 
                    PrimaryButton
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                            if (!uiState.loadingMessage.isNullOrBlank()) {
                                Text(
                                    text = uiState.loadingMessage ?: "",
                                    style = FontUtils.mainFont(AppFontStyle.Regular, FontSize.Small),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "บันทึกข้อมูล",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Medium
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Success Dialog - wait for user to click OK
        if (uiState.isSuccess) {
            AlertDialog(
                onDismissRequest = { 
                    // Prevent dismissing by clicking outside
                },
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
                        text = if (isEditMode) "แก้ไขสินค้าสำเร็จ" else "เพิ่มสินค้าสำเร็จ",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.dismissSuccessDialog()
                        onSaveSuccess()
                    }) {
                        Text("ตกลง", color = PrimaryButton)
                    }
                }
            )
        }
        
        // Error dialog
        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = {
                    Text(
                        text = "เกิดข้อผิดพลาด",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        )
                    )
                },
                text = {
                    Text(
                        text = error,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("ตกลง")
                    }
                }
            )
        }
        
        // Add Category Dialog
        if (uiState.showAddCategoryDialog) {
            AddCategoryDialog(
                categoryName = uiState.categoryName,
                isLoading = uiState.isCreatingCategory,
                errorMessage = uiState.categoryError,
                onCategoryNameChange = { viewModel.updateCategoryName(it) },
                onConfirm = { viewModel.createCategory() },
                onDismiss = { viewModel.dismissAddCategoryDialog() }
            )
        }
        
        // Category Success Dialog
        uiState.categorySuccess?.let { message ->
            AlertDialog(
                onDismissRequest = { 
                    viewModel.clearCategorySuccess()
                },
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
                    TextButton(
                        onClick = { 
                            viewModel.clearCategorySuccess()
                        }
                    ) {
                        Text(
                            text = "ตกลง",
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
        
        // No Internet Dialog (when trying to save with image but no network)
        if (uiState.showNoInternetDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissNoInternetDialog() },
                title = {
                    Text(
                        text = "ไม่สามารถบันทึกได้",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = "กรุณาเชื่อมต่ออินเทอร์เน็ตเพื่ออัปโหลดรูปภาพ",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.dismissNoInternetDialog() }
                    ) {
                        Text(
                            text = "ตกลง",
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
        
        // Image Upload Error Dialog (when image upload fails)
        if (uiState.showImageUploadErrorDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissImageUploadErrorDialog() },
                title = {
                    Text(
                        text = "อัปโหลดรูปภาพไม่สำเร็จ",
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
                            text = uiState.errorMessage ?: "เกิดข้อผิดพลาดในการอัปโหลดรูปภาพ",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ต้องการบันทึกสินค้าโดยไม่มีรูปภาพหรือไม่?",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = SecondaryText
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.saveProductWithoutImage() }
                    ) {
                        Text(
                            text = "บันทึกโดยไม่มีรูป",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissImageUploadErrorDialog() }
                    ) {
                        Text(
                            text = "ยกเลิก",
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
        
        // Image Picker Dialog
        if (showImagePickerDialog) {
            ImagePickerDialog(
                onDismiss = { showImagePickerDialog = false },
                onCameraClick = {
                    showImagePickerDialog = false
                    // Open camera
                    try {
                        // Create unique filename to avoid conflicts
                        val timestamp = System.currentTimeMillis()
                        val photoFile = File(context.cacheDir, "temp_photo_$timestamp.jpg")
                        // Ensure parent directory exists
                        photoFile.parentFile?.mkdirs()
                        
                        val photoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )
                        cameraImageUri = photoUri
                        android.util.Log.d("AddEditProduct", "Opening camera with URI: $photoUri, File: ${photoFile.absolutePath}")
                        cameraLauncher.launch(photoUri)
                    } catch (e: Exception) {
                        android.util.Log.e("AddEditProduct", "Error opening camera: ${e.message}", e)
                        // Fallback to simple camera intent (won't work for getting result)
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        try {
                            context.startActivity(intent)
                        } catch (e2: Exception) {
                            android.util.Log.e("AddEditProduct", "Error starting camera intent: ${e2.message}", e2)
                        }
                    }
                },
                onGalleryClick = {
                    showImagePickerDialog = false
                    // Open gallery
                    imagePickerLauncher.launch("image/*")
                }
            )
        }
    }
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
                text = "เลือกรูปภาพ",
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
                        text = "ถ่ายรูป",
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
                        text = "เลือกรูปจากแกลเลอรี",
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
                    text = "ยกเลิก",
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
 * Color Picker Grid Component
 */
@Composable
private fun ColorPickerGrid(
    selectedColorHex: String?,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#000000", // Black
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#FFEB3B", // Yellow
        "#9C27B0", // Purple
        "#FF9800", // Orange
        "#F44336", // Red
        "#795548", // Brown
        "#00BCD4", // Cyan
        "#FFC107", // Amber
        "#3F51B5", // Indigo
        "#E91E63"  // Pink
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.take(6).forEach { colorHex ->
                    ColorSwatch(
                        colorHex = colorHex,
                        isSelected = selectedColorHex == colorHex,
                        onClick = { onColorSelected(colorHex) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colors.drop(6).forEach { colorHex ->
                    ColorSwatch(
                        colorHex = colorHex,
                        isSelected = selectedColorHex == colorHex,
                        onClick = { onColorSelected(colorHex) }
                    )
                }
            }
        }
    }
}

/**
 * Color Swatch Component
 */
@Composable
private fun ColorSwatch(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.padding(2.dp)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(color)
        )
        if (isSelected) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE0E0E0))
            ) {}
        }
    }
}

/**
 * Add Category Dialog
 */
@Composable
private fun AddCategoryDialog(
    categoryName: String,
    isLoading: Boolean,
    errorMessage: String?,
    onCategoryNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "เพิ่มหมวดหมู่ใหม่",
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
                    text = "กรุณาใส่ชื่อหมวดหมู่",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = onCategoryNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "กรอกชื่อหมวดหมู่",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PlaceholderText
                        )
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                        focusedBorderColor = PrimaryButton
                    )
                )
                // Don't show errors inline - all errors (including validation) are shown as popup
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "ยกเลิก",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                }
                TextButton(
                    onClick = onConfirm,
                    enabled = !isLoading && categoryName.trim().isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = PrimaryButton
                        )
                    } else {
                        Text(
                            text = "ตกลง",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                }
            }
        }
    )
}

/**
 * Format price for display: Hide .00, show 2 decimal places otherwise
 * Example: 5000.00 -> 5000, 5000.50 -> 5000.50
 * Preserves decimal point if user is typing (e.g., "5000." stays as "5000.")
 */
private fun formatPriceForDisplay(price: String): String {
    if (price.isBlank()) return price
    
    // If price ends with ".", preserve it (user is typing)
    val endsWithDot = price.trim().endsWith(".")
    val priceToParse = if (endsWithDot) price.trim().dropLast(1) else price.trim()
    
    val parsed = priceToParse.toDoubleOrNull()
    return if (parsed != null) {
        val formatted = if (parsed % 1.0 == 0.0) {
            // If decimal is .00, don't show decimals
            parsed.toInt().toString()
        } else {
            // Show 2 decimal places
            String.format("%.2f", parsed)
        }
        // If original ended with ".", add it back
        if (endsWithDot) "$formatted." else formatted
    } else {
        // If can't parse, return as is (might be invalid input or user typing)
        price
    }
}

@Composable
private fun FormFieldLabel(
    text: String,
    required: Boolean = false,
    modifier: Modifier = Modifier
) {
    val annotatedLabel = buildAnnotatedString {
        append(text)
        if (required) {
            append(" ")
            withStyle(style = SpanStyle(color = Color(0xFFE83808))) {
                append("*")
            }
        }
    }
    Text(
        text = annotatedLabel,
        style = FontUtils.mainFont(
            style = AppFontStyle.Regular,
            size = FontSize.Medium
        ),
        modifier = modifier.padding(bottom = 8.dp),
        color = PrimaryText
    )
}

/**
 * AddOn Group Selection Item Component
 */
@Composable
private fun AddonGroupSelectionItem(
    addonGroup: com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = addonGroup.name,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.weight(1f)
            )
            
            // Checkbox Icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) PrimaryButton else Color.Transparent)
                    .then(
                        if (!isSelected) {
                            Modifier.border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "เลือก",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    val selectedCategoryName = selectedCategoryId?.let { id ->
        categories.find { it.id == id }?.name
    } ?: "กรุณาเลือกหมวดหมู่"
    
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { showDropdown = true },
        color = Color(0xFFF5F5F5),
        shape = RoundedCornerShape(8.dp)
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
                color = if (selectedCategoryId == null) PlaceholderText else PrimaryText,
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
                    text = "เลือกหมวดหมู่ (${categories.size} รายการ)",
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
                    Text("ยกเลิก", color = PrimaryText)
                }
            }
        )
    }
}

