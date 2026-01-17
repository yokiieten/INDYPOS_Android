package com.indybrain.indypos_Android.presentation.addongroupmanagement

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Add/Edit Addon Group Screen
 * @param addonGroupId If provided, screen is in edit mode. If null, screen is in add mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAddonGroupScreen(
    addonGroupId: String? = null,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    viewModel: AddEditAddonGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditMode = addonGroupId != null
    var showAddAddonDialog by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    
    // Initialize for edit mode
    LaunchedEffect(addonGroupId) {
        if (addonGroupId != null) {
            viewModel.initializeForEdit(addonGroupId)
        }
    }
    
    // Navigate back when user clicks OK on success dialog
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
                        text = if (isEditMode) "แก้ไขแอดออนกรุ๊ป" else "เพิ่มแอดออนกรุ๊ป",
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 16.dp,
                        bottom = 80.dp // Space for bottom button
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Group Name Section
                    item {
                        FormTextFieldSection(
                            title = "ชื่อกลุ่ม",
                            value = uiState.formState.groupName,
                            onValueChange = viewModel::updateGroupName,
                            placeholder = "ความเผ็ด",
                            isRequired = true
                        )
                    }
                    
                    // Is Required Section
                    item {
                        RequiredCheckboxSection(
                            title = "จำเป็นต้องเลือก",
                            isChecked = uiState.formState.isRequired,
                            onCheckedChange = { viewModel.toggleIsRequired() }
                        )
                    }
                    
                    // Max Selection Section
                    item {
                        FormTextFieldSection(
                            title = "เลือกได้สูงสุด",
                            value = uiState.formState.maxSelection,
                            onValueChange = viewModel::updateMaxSelection,
                            placeholder = "1",
                            keyboardType = KeyboardType.Number,
                            isRequired = false
                        )
                    }
                    
                    // Addons Section Header
                    item {
                        AddonsSectionHeader(
                            title = "แอดออน",
                            onAddNewAddonClick = { showAddAddonDialog = true }
                        )
                    }
                    
                    // Addon Items
                    items(uiState.availableAddons.size) { index ->
                        val addon = uiState.availableAddons[index]
                        AddonItem(
                            addon = addon,
                            isSelected = uiState.formState.selectedAddonIds.contains(addon.id),
                            onToggle = { viewModel.toggleAddonSelection(addon.id) }
                        )
                    }
                    
                    // Empty state
                    if (uiState.availableAddons.isEmpty()) {
                        item {
                            Text(
                                text = "ไม่มี Addon",
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Small
                                ),
                                color = SecondaryText
                            )
                        }
                    }
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
                        onClick = { viewModel.saveAddonGroup() }
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
                            text = if (isEditMode) "บันทึกการแก้ไข" else "เพิ่มแอดออนกรุ๊ป",
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
        
        // Success Dialog for Addon Group
        if (uiState.isSuccess) {
            SuccessDialog(
                isEditMode = isEditMode,
                isOffline = uiState.isOfflineSuccess,
                onOkClick = {
                    shouldNavigateBack = true
                }
            )
        }
        
        // Success Dialog for Addon Creation
        uiState.successMessage?.takeIf { !uiState.isSuccess }?.let { successMessage ->
            AlertDialog(
                onDismissRequest = { /* Prevent dismissing */ },
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
                        text = successMessage,
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
        uiState.errorMessage?.let { errorMessage ->
            ErrorDialog(
                errorMessage = errorMessage,
                onDismiss = {
                    viewModel.clearErrorMessage()
                }
            )
        }
        
        // Add New Addon Dialog
        if (showAddAddonDialog) {
            // Use key to reset dialog state when it opens
            key(showAddAddonDialog) {
                AddAddonDialog(
                    onConfirm = { name, price ->
                        viewModel.createAddon(name, price)
                        // Don't close dialog here - let ViewModel handle it for API errors
                    },
                    onDismiss = {
                        showAddAddonDialog = false
                        viewModel.clearErrorMessage()
                        viewModel.clearSuccessMessage()
                    },
                    isLoading = uiState.isLoading,
                    errorMessage = uiState.errorMessage
                )
            }
        }
        
        // Close dialog when any error occurs or success
        LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
            // If error occurs while dialog is open, close dialog and show error popup
            if (showAddAddonDialog && uiState.errorMessage != null) {
                showAddAddonDialog = false
            }
            // Close dialog on success (when successMessage is set and not for addon group creation)
            if (showAddAddonDialog && uiState.successMessage != null && !uiState.isSuccess) {
                showAddAddonDialog = false
            }
        }
    }
}

/**
 * Form Text Field Section
 */
@Composable
private fun FormTextFieldSection(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isRequired: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        val annotatedTitle = if (isRequired) {
            buildAnnotatedString {
                append(title)
                append(" ")
                withStyle(style = SpanStyle(color = RedFailure)) {
                    append("*")
                }
            }
        } else {
            AnnotatedString(title)
        }
        
        Text(
            text = annotatedTitle,
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
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
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType
            ),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )
    }
}

/**
 * Required Checkbox Section
 */
@Composable
private fun RequiredCheckboxSection(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            )
        )
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryButton,
                uncheckedColor = SecondaryText
            )
        )
    }
}

/**
 * Addons Section Header
 */
@Composable
private fun AddonsSectionHeader(
    title: String,
    onAddNewAddonClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val annotatedTitle = buildAnnotatedString {
            append(title)
            append(" ")
            withStyle(style = SpanStyle(color = RedFailure)) {
                append("*")
            }
        }
        
        Text(
            text = annotatedTitle,
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            )
        )
        
        TextButton(onClick = onAddNewAddonClick) {
            Text(
                text = "+ เพิ่ม",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Medium
                ),
                color = PrimaryButton
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

/**
 * Addon Item
 */
@Composable
private fun AddonItem(
    addon: AddonEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = addon.name,
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            )
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryButton,
                uncheckedColor = SecondaryText
            )
        )
    }
}

/**
 * Success Dialog
 */
@Composable
private fun SuccessDialog(
    isEditMode: Boolean,
    isOffline: Boolean,
    onOkClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing */ },
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
                    isEditMode && isOffline -> "แก้ไขกลุ่ม Addon สำเร็จ (บันทึกในเครื่อง)"
                    isEditMode && !isOffline -> "แก้ไขกลุ่ม Addon สำเร็จ"
                    !isEditMode && isOffline -> "เพิ่มกลุ่ม Addon สำเร็จ (บันทึกในเครื่อง)"
                    else -> "เพิ่มกลุ่ม Addon สำเร็จ"
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
        },
        confirmButton = {
            TextButton(onClick = onOkClick) {
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
 * Error Dialog
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
            TextButton(onClick = onDismiss) {
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
 * Add Addon Dialog
 */
@Composable
private fun AddAddonDialog(
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    var addonName by remember { mutableStateOf("") }
    var addonPrice by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }
    
    // Show validation error dialog
    if (showValidationError) {
        AlertDialog(
            onDismissRequest = { showValidationError = false },
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
                    text = "กรุณากรอกข้อมูลให้ครบ",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            },
            confirmButton = {
                TextButton(onClick = { showValidationError = false }) {
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "เพิ่ม Addon",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Column {
                // Secondary instruction text
                Text(
                    text = "กรุณาใส่ชื่อและราคาแอดออน",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = addonName,
                    onValueChange = { addonName = it },
                    placeholder = {
                        Text(
                            text = "ระบุชื่อแอดออน",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PlaceholderText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = PrimaryButton
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = addonPrice,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { 
                            it.isDigit() || it == '.' 
                        }
                        val parts = filtered.split('.')
                        val finalValue = if (parts.size > 2) {
                            parts[0] + "." + parts.drop(1).joinToString("")
                        } else {
                            filtered
                        }
                        addonPrice = finalValue
                    },
                    placeholder = {
                        Text(
                            text = "ราคา",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PlaceholderText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = PrimaryButton
                    )
                )
                // Don't show errors inline - all errors (including validation) are shown as popup
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = addonName.trim()
                    val priceStr = addonPrice.trim()
                    
                    // Validate both fields are filled
                    if (name.isEmpty() || priceStr.isEmpty()) {
                        showValidationError = true
                    } else {
                        val price = priceStr.toDoubleOrNull()
                        if (price == null || price <= 0) {
                            showValidationError = true
                        } else {
                            onConfirm(name, price)
                            addonName = ""
                            addonPrice = ""
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = "เพิ่ม",
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

