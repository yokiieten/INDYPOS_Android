package com.indybrain.indypos_Android.presentation.settings.receipt

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Hashtable
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.domain.model.PromptPayType
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptSettingsScreen(
    onBackClick: () -> Unit = {},
    viewModel: ReceiptSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPaperSizeDialog by remember { mutableStateOf(false) }
    var showQRCodePreviewDialog by remember { mutableStateOf(false) }
    
    // Image picker launcher (Gallery)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateShopLogoImage(it)
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            cameraImageUri?.let { uri ->
                viewModel.updateShopLogoImage(uri)
            }
        }
    }
    
    // Show success/error messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            // Message will be shown in dialog
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ใบเสร็จ",
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
                            contentDescription = null,
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
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Print Shop Logo
                SwitchSettingItem(
                    title = "พิมพ์โลโก้ร้าน",
                    subtitle = "บริการพิมพ์แบบกำหนดเอง",
                    checked = uiState.printShopLogo,
                    onCheckedChange = { viewModel.updatePrintShopLogo(it) }
                )
                
                // Shop Logo Image (shown when printShopLogo is enabled)
                if (uiState.printShopLogo) {
                    ImagePickerItem(
                        title = "เลือกโลโก้ร้าน",
                        subtitle = "แนบรูปโลโก้ร้านของคุณ",
                        imageBitmap = uiState.shopLogoBitmap,
                        onClick = { showImagePickerDialog = true }
                    )
                }
                
                // Print After Finish
                SwitchSettingItem(
                    title = "พิมพ์ใบเสร็จอัตโนมัติ",
                    checked = uiState.printAfterFinish,
                    onCheckedChange = { viewModel.updatePrintAfterFinish(it) }
                )
                
                // QR Code PromptPay
                SwitchSettingItem(
                    title = "QR Code PromptPay",
                    subtitle = "สร้าง QR Code PromptPay สำหรับการรับชำระเงิน",
                    checked = uiState.showQRCode,
                    onCheckedChange = { viewModel.updateShowQRCode(it) }
                )
                
                // PromptPay Form (shown when showQRCode is enabled)
                if (uiState.showQRCode) {
                    PromptPayFormItem(
                        promptPayType = uiState.promptPayType,
                        promptPayIdentifier = uiState.promptPayIdentifier,
                        errorMessage = uiState.promptPayIdentifierError,
                        onTypeChange = { viewModel.updatePromptPayType(it) },
                        onIdentifierChange = { viewModel.updatePromptPayIdentifier(it) },
                        onPreviewClick = { showQRCodePreviewDialog = true }
                    )
                }
                
                // Open Cash Drawer
                SwitchSettingItem(
                    title = "เปิดลิ้นชักอัตโนมัติ",
                    checked = uiState.openCashDrawer,
                    onCheckedChange = { viewModel.updateOpenCashDrawer(it) }
                )
                
                // Paper Size
                PickerSettingItem(
                    title = "ขนาดกระดาษ",
                    value = when (uiState.paperSize) {
                        "58" -> "58 มม"
                        "80" -> "80 มม"
                        else -> "58 มม"
                    },
                    onClick = { showPaperSizeDialog = true }
                )
                
                // Footer
                TextFieldSettingItem(
                    title = "ส่วนท้าย",
                    value = uiState.footer,
                    placeholder = "กรอกข้อความ...",
                    onValueChange = { viewModel.updateFooter(it) }
                )
                
                // Tax Identification Number
                SwitchSettingItem(
                    title = "เลขประจำตัวผู้เสียภาษีอากร",
                    subtitle = "แสดงเลขประจำตัวผู้เสียภาษีอากรบนใบเสร็จเพื่อการปฏิบัติตามกฎหมายภาษี",
                    checked = uiState.taxIdentificationNumber,
                    onCheckedChange = { viewModel.updateTaxIdentificationNumber(it) }
                )
                
                // TIN Number Input (shown when taxIdentificationNumber is enabled)
                if (uiState.taxIdentificationNumber) {
                    TextFieldSettingItem(
                        title = null,
                        value = uiState.tinNumber,
                        placeholder = "กรอกเลขประจำตัวผู้เสียภาษีอากร 13 หลัก",
                        errorMessage = uiState.tinNumberError,
                        keyboardType = KeyboardType.Number,
                        onValueChange = { viewModel.updateTINNumber(it) }
                    )
                }
            }
            
            // Save Button
            Button(
                onClick = { viewModel.saveSettings() },
                enabled = uiState.hasChanges && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.hasChanges && !uiState.isSaving) {
                        PrimaryButton
                    } else {
                        Color(0xFFD1D1D6)
                    },
                    disabledContainerColor = Color(0xFFD1D1D6)
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "กำลังบันทึก...",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Large
                        ),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "บันทึกการตั้งค่า",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Large
                        ),
                        color = if (uiState.hasChanges) Color.White else Color(0xFF8E8E93)
                    )
                }
            }
        }
    }
    
    // Image Picker Dialog
    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onCameraClick = {
                showImagePickerDialog = false
                try {
                    val photoFile = File(context.cacheDir, "temp_shop_logo_${System.currentTimeMillis()}.jpg")
                    val photoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    cameraImageUri = photoUri
                    cameraLauncher.launch(photoUri)
                } catch (e: Exception) {
                    // Fallback to simple camera intent
                    try {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Handle error
                    }
                }
            },
            onGalleryClick = {
                showImagePickerDialog = false
                imagePickerLauncher.launch("image/*")
            },
            onRemoveClick = if (uiState.shopLogoBitmap != null) {
                {
                    showImagePickerDialog = false
                    viewModel.updateShopLogoImage(null)
                }
            } else null
        )
    }
    
    // Paper Size Dialog
    if (showPaperSizeDialog) {
        PaperSizeDialog(
            currentSize = uiState.paperSize,
            onDismiss = { showPaperSizeDialog = false },
            onSizeSelected = { size ->
                viewModel.updatePaperSize(size)
                showPaperSizeDialog = false
            }
        )
    }
    
    // QR Code Preview Dialog
    if (showQRCodePreviewDialog) {
        QRCodePreviewDialog(
            promptPayType = uiState.promptPayType,
            promptPayIdentifier = uiState.promptPayIdentifier,
            onDismiss = { showQRCodePreviewDialog = false }
        )
    }
    
    // Success Dialog
    uiState.successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSuccessMessage() },
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
                TextButton(onClick = { 
                    viewModel.clearSuccessMessage()
                    onBackClick()
                }) {
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
    
    // Error Dialog
    uiState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
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
                    text = message,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
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
}

/**
 * Switch Setting Item
 */
@Composable
private fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Smaller
                            ),
                            color = SecondaryText
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryButton,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD1D1D6)
                    )
                )
            }
        }
    }
}

/**
 * Picker Setting Item
 */
@Composable
private fun PickerSettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = FontUtils.mainFont(
                    style = AppFontStyle.SemiBold,
                    size = FontSize.Large
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = PlaceholderText
                )
            }
        }
    }
}

/**
 * Text Field Setting Item
 */
@Composable
private fun TextFieldSettingItem(
    title: String?,
    value: String,
    placeholder: String,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.SemiBold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PlaceholderText
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = keyboardType
                ),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = Color(0xFFD1D1D6),
                    errorBorderColor = Color(0xFFFF3B30)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = Color(0xFFFF3B30)
                )
            }
        }
    }
}

/**
 * Image Picker Item
 */
@Composable
private fun ImagePickerItem(
    title: String,
    subtitle: String,
    imageBitmap: android.graphics.Bitmap?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = FontUtils.mainFont(
                    style = AppFontStyle.SemiBold,
                    size = FontSize.Large
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = subtitle,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Smaller
                ),
                color = SecondaryText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F7)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = PlaceholderText,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

/**
 * PromptPay Form Item
 */
@Composable
private fun PromptPayFormItem(
    promptPayType: PromptPayType,
    promptPayIdentifier: String,
    errorMessage: String?,
    onTypeChange: (PromptPayType) -> Unit,
    onIdentifierChange: (String) -> Unit,
    onPreviewClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ข้อมูล PromptPay",
                style = FontUtils.mainFont(
                    style = AppFontStyle.SemiBold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
            Text(
                text = "กรอกข้อมูลสำหรับสร้าง QR Code PromptPay",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Smaller
                ),
                color = SecondaryText
            )
            
            // Type Selection
            Column {
                PromptPayType.values().forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTypeChange(type) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = promptPayType == type,
                            onClick = { onTypeChange(type) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryButton
                            )
                        )
                        Text(
                            text = type.displayName,
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                    }
                }
            }
            
            // Identifier Input
            OutlinedTextField(
                value = promptPayIdentifier,
                onValueChange = onIdentifierChange,
                placeholder = {
                    Text(
                        text = when (promptPayType) {
                            PromptPayType.PHONE_NUMBER -> "08XXXXXXXX"
                            PromptPayType.NATIONAL_ID -> "1234567890123"
                            PromptPayType.E_WALLET -> "กรอก e-Wallet ID"
                        },
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PlaceholderText
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = Color(0xFFD1D1D6),
                    errorBorderColor = Color(0xFFFF3B30)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = Color(0xFFFF3B30)
                )
            }
            
            // Preview QR Code Button
            Button(
                onClick = onPreviewClick,
                enabled = promptPayIdentifier.isNotBlank() && errorMessage == null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButton,
                    disabledContainerColor = Color(0xFFD1D1D6)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ดูตัวอย่าง QR Code",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = Color.White
                )
            }
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
    onGalleryClick: () -> Unit,
    onRemoveClick: (() -> Unit)? = null
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
                if (onRemoveClick != null) {
                    TextButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ลบรูปภาพ",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = Color(0xFFFF3B30)
                        )
                    }
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
 * QR Code Preview Dialog
 */
@Composable
private fun QRCodePreviewDialog(
    promptPayType: PromptPayType,
    promptPayIdentifier: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember { mutableStateOf<Bitmap?>(null) }
    
    // Generate QR code
    LaunchedEffect(promptPayType, promptPayIdentifier) {
        if (promptPayIdentifier.isNotBlank()) {
            // Generate PromptPay QR code data
            val qrData = generatePromptPayQRData(promptPayType, promptPayIdentifier)
            qrBitmap.value = generateQRCode(qrData, 512)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = PrimaryText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ดูตัวอย่าง QR Code",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "ปิด",
                        tint = PrimaryText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // QR Code
                qrBitmap.value?.let { bitmap ->
                    Surface(
                        modifier = Modifier.size(220.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } ?: run {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color(0xFFF5F5F7), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = PrimaryButton
                        )
                    }
                }
                
                // Type and Number Info
                Text(
                    text = "ประเภท: ${promptPayType.displayName}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
                Text(
                    text = "หมายเลข: $promptPayIdentifier",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.SemiBold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                
                // Note
                Text(
                    text = "QR Code นี้จะรวมจำนวนเงินอัตโนมัติเมื่อพิมพ์ใบเสร็จ",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "ปิด",
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
 * Generate PromptPay QR Code data according to EMV QR Code standard
 * Format: [00]01[01]11[29][30]A000000677010111[01-03][identifier][52]0000[53]764[58]TH[63][CRC]
 */
private fun generatePromptPayQRData(type: PromptPayType, identifier: String): String {
    // Clean identifier (remove dashes and spaces)
    var cleanIdentifier = identifier.replace(Regex("[^0-9]"), "")
    
    // Format identifier according to PromptPay standard
    when (type) {
        PromptPayType.PHONE_NUMBER -> {
            // Phone number format: 0066[phone without leading 0]
            // Example: 0821234567 -> 0066821234567 (13 digits)
            if (cleanIdentifier.startsWith("0") && cleanIdentifier.length == 10) {
                // Remove leading 0 and add 0066
                cleanIdentifier = "0066" + cleanIdentifier.substring(1)
            } else if (!cleanIdentifier.startsWith("0066")) {
                // If doesn't start with 0066, add it
                if (cleanIdentifier.startsWith("66")) {
                    cleanIdentifier = "00$cleanIdentifier"
                } else {
                    cleanIdentifier = "0066$cleanIdentifier"
                }
            }
            // Ensure exactly 13 digits
            cleanIdentifier = cleanIdentifier.padStart(13, '0').take(13)
        }
        PromptPayType.NATIONAL_ID -> {
            // National ID must be exactly 13 digits
            cleanIdentifier = cleanIdentifier.padStart(13, '0').take(13)
        }
        PromptPayType.E_WALLET -> {
            // e-Wallet ID - typically 15 digits, but can vary
            // Keep as is, just ensure it's numeric
            if (cleanIdentifier.length < 13) {
                cleanIdentifier = cleanIdentifier.padStart(13, '0')
            }
        }
    }
    
    // Build EMV QR Code payload
    val payload = buildString {
        // Payload Format Indicator (00)
        append("00")
        append("02") // Length
        append("01") // Value
        
        // Point of Initiation Method (01) - 11 = Static, 12 = Dynamic
        append("01")
        append("02") // Length
        append("11") // Static QR (no amount)
        
        // Merchant Account Information (29-51)
        append("29") // Tag
        val merchantAccountInfo = buildString {
            // AID (00)
            append("00")
            append("16") // Length
            append("A000000677010111") // PromptPay AID
            
            // Account Identifier (01-03)
            when (type) {
                PromptPayType.PHONE_NUMBER -> {
                    append("01") // Phone number
                }
                PromptPayType.NATIONAL_ID -> {
                    append("02") // National ID
                }
                PromptPayType.E_WALLET -> {
                    append("03") // e-Wallet
                }
            }
            val identifierLength = String.format("%02d", cleanIdentifier.length)
            append(identifierLength)
            append(cleanIdentifier)
        }
        val merchantAccountInfoLength = String.format("%02d", merchantAccountInfo.length)
        append(merchantAccountInfoLength)
        append(merchantAccountInfo)
        
        // Merchant Category Code (52)
        append("52")
        append("04") // Length
        append("0000") // General
        
        // Transaction Currency (53)
        append("53")
        append("03") // Length
        append("764") // THB (Thai Baht)
        
        // Country Code (58)
        append("58")
        append("02") // Length
        append("TH") // Thailand
    }
    
    // Calculate CRC16-CCITT
    // CRC is calculated from the entire payload including tag 63 and length 04, but excluding CRC value
    val payloadWithoutCRC = payload.toString()
    val payloadForCRC = payloadWithoutCRC + "6304"
    val crc = calculateCRC16(payloadForCRC)
    val crcHex = String.format("%04X", crc)
    
    // Append CRC to complete the payload
    return payloadWithoutCRC + "63" + "04" + crcHex
}

/**
 * Calculate CRC16-CCITT checksum for EMV QR Code
 * Uses CRC-16-CCITT (polynomial 0x1021, initial value 0xFFFF)
 */
private fun calculateCRC16(data: String): Int {
    var crc = 0xFFFF
    val polynomial = 0x1021
    
    for (byte in data.toByteArray(Charsets.ISO_8859_1)) {
        val unsignedByte = byte.toInt() and 0xFF
        crc = crc xor (unsignedByte shl 8)
        for (i in 0 until 8) {
            if ((crc and 0x8000) != 0) {
                crc = ((crc shl 1) xor polynomial) and 0xFFFF
            } else {
                crc = (crc shl 1) and 0xFFFF
            }
        }
    }
    
    return crc and 0xFFFF
}

/**
 * Generate QR code bitmap from text
 */
private fun generateQRCode(text: String, size: Int): Bitmap? {
    return try {
        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Paper Size Dialog
 */
@Composable
private fun PaperSizeDialog(
    currentSize: String,
    onDismiss: () -> Unit,
    onSizeSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "เลือกขนาดกระดาษ",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Column {
                listOf("58", "80").forEach { size ->
                    val displayText = when (size) {
                        "58" -> "58 มม"
                        "80" -> "80 มม"
                        else -> size
                    }
                    TextButton(
                        onClick = {
                            onSizeSelected(size)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayText,
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = if (currentSize == size) PrimaryButton else PrimaryText
                        )
                    }
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

