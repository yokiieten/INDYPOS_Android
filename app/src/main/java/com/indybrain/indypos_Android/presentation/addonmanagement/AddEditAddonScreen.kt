package com.indybrain.indypos_Android.presentation.addonmanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Add/Edit Addon Screen
 * @param addonId If provided, screen is in edit mode. If null, screen is in add mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddonScreen(
    addonId: String? = null,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    viewModel: AddEditAddonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditMode = addonId != null
    var priceText by remember { mutableStateOf(uiState.addonPrice) }
    val coroutineScope = rememberCoroutineScope()
    
    // Load addon data if in edit mode
    LaunchedEffect(addonId) {
        if (addonId != null) {
            viewModel.loadAddon(addonId)
        }
    }
    
    // Update price text when state changes
    LaunchedEffect(uiState.addonPrice) {
        priceText = uiState.addonPrice
    }
    
    // Track if user clicked OK on success dialog
    var shouldNavigateBack by remember { mutableStateOf(false) }
    
    // Navigate back when user clicks OK
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            onSaveSuccess()
            viewModel.dismissSuccessDialog()
            shouldNavigateBack = false
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) 
                            "แก้ไข Addon"
                        else 
                            "เพิ่ม Addon",
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
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Addon Name Label with red asterisk
                    val nameLabelText = "ชื่อ Addon"
                    val annotatedNameLabel = buildAnnotatedString {
                        append(nameLabelText)
                        append(" ")
                        withStyle(style = SpanStyle(color = RedFailure)) {
                            append("*")
                        }
                    }
                    Text(
                        text = annotatedNameLabel,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Addon Name Input Field
                    OutlinedTextField(
                        value = uiState.addonName,
                        onValueChange = { viewModel.updateAddonName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "กรุณากรอกชื่อ Addon",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PlaceholderText
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Addon Price Label (no asterisk - optional)
                    Text(
                        text = "ราคา",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Addon Price Input Field
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { newValue ->
                            // Allow only numbers and decimal point
                            val filtered = newValue.filter { 
                                it.isDigit() || it == '.' 
                            }
                            // Ensure only one decimal point
                            val parts = filtered.split('.')
                            val finalValue = if (parts.size > 2) {
                                parts[0] + "." + parts.drop(1).joinToString("")
                            } else {
                                filtered
                            }
                            priceText = finalValue
                            viewModel.updateAddonPrice(finalValue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "กรุณากรอกราคา",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PlaceholderText
                            )
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        enabled = !uiState.isLoading
                    )
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
                        onClick = { viewModel.saveAddon {} }
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = if (isEditMode) 
                                "บันทึกการแก้ไข"
                            else 
                                "เพิ่ม Addon",
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
        
        // Success Dialog - Outside Box but inside Scaffold
        if (uiState.isSuccess) {
            SuccessDialog(
                isEditMode = isEditMode,
                isOffline = uiState.isOfflineSuccess,
                onOkClick = {
                    // Trigger navigation via LaunchedEffect
                    shouldNavigateBack = true
                }
            )
        }
        
        // Error Dialog - Show all errors as popup (except free plan limit)
        uiState.errorMessage?.takeIf { 
            !it.contains("free_plan_limit_exceeded", ignoreCase = true) 
        }?.let { errorMessage ->
            ErrorDialog(
                errorMessage = errorMessage,
                onDismiss = {
                    viewModel.clearError()
                }
            )
        }
        
        // Free Plan Limit Dialog - Outside Box but inside Scaffold
        uiState.errorMessage?.takeIf { 
            it.contains("free_plan_limit_exceeded", ignoreCase = true) 
        }?.let {
            FreePlanLimitDialog(
                onDismiss = {
                    viewModel.clearError()
                },
                onContactUs = {
                    // TODO: Navigate to contact us screen
                    viewModel.clearError()
                }
            )
        }
    }
}

/**
 * Success Dialog for Add/Edit Addon
 */
@Composable
private fun SuccessDialog(
    isEditMode: Boolean,
    isOffline: Boolean,
    onOkClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing by clicking outside */ },
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
                text = when {
                    isEditMode && isOffline -> "แก้ไข Addon สำเร็จ (บันทึกในเครื่อง)"
                    isEditMode && !isOffline -> "แก้ไข Addon สำเร็จ"
                    !isEditMode && isOffline -> "เพิ่ม Addon สำเร็จ (บันทึกในเครื่อง)"
                    else -> "เพิ่ม Addon สำเร็จ"
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
        },
        confirmButton = {
            TextButton(
                onClick = onOkClick
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

/**
 * Error Dialog for Add/Edit Addon
 */
@Composable
private fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = errorMessage,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
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

/**
 * Free Plan Limit Dialog
 */
@Composable
private fun FreePlanLimitDialog(
    onDismiss: () -> Unit,
    onContactUs: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ถึงขีดจำกัด",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Text(
                text = "คุณถึงขีดจำกัดของแผนฟรีแล้ว กรุณาติดต่อเราเพื่ออัปเกรดแผน",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
        },
        confirmButton = {
            TextButton(
                onClick = onContactUs
            ) {
                Text(
                    text = "ติดต่อเรา",
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
                onClick = onDismiss
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

