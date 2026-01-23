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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.saveable.rememberSaveable
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
    var isSaveInProgress by remember { mutableStateOf(false) }

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

    // Reset save-in-progress flag when loading finishes
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isSaveInProgress = false
        }
    }

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
                            stringResource(id = R.string.addon_edit_title)
                        else 
                            stringResource(id = R.string.addon_add_title),
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
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Addon Name Label with red asterisk
                    val nameLabelText = stringResource(id = R.string.addon_form_name_label)
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
                                text = stringResource(id = R.string.addon_form_name_placeholder),
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
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
                            focusedBorderColor = PrimaryButton
                        ),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Addon Price Label (no asterisk - optional)
                    Text(
                        text = stringResource(id = R.string.addon_form_price_label),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
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
                        modifier = Modifier
                            .fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.addon_form_price_placeholder),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PlaceholderText
                            )
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(),
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
                        enabled = !uiState.isLoading && !isSaveInProgress,
                        onClick = {
                            if (!isSaveInProgress) {
                                isSaveInProgress = true
                                viewModel.saveAddon {}
                            }
                        }
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
                                stringResource(id = R.string.addon_form_save_edit)
                            else 
                                stringResource(id = R.string.addon_form_save_add),
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
                    isSaveInProgress = false
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
                    isSaveInProgress = false
                },
                onContactUs = {
                    // TODO: Navigate to contact us screen
                    viewModel.clearError()
                    isSaveInProgress = false
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
                text = stringResource(id = R.string.success_title),
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
                    isEditMode && isOffline -> stringResource(id = R.string.addon_form_success_edit_offline)
                    isEditMode && !isOffline -> stringResource(id = R.string.addon_form_success_edit)
                    !isEditMode && isOffline -> stringResource(id = R.string.addon_form_success_add_offline)
                    else -> stringResource(id = R.string.addon_form_success_add)
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
                text = stringResource(id = R.string.dialog_error_title),
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

/**
 * Format price for display
 */
private fun formatPriceForDisplay(price: String): String {
    if (price.isBlank()) return price
    
    val endsWithDot = price.trim().endsWith(".")
    val priceToParse = if (endsWithDot) price.trim().dropLast(1) else price.trim()
    
    val parsed = priceToParse.toDoubleOrNull()
    return if (parsed != null) {
        val formatted = if (parsed % 1.0 == 0.0) {
            parsed.toInt().toString()
        } else {
            String.format("%.2f", parsed)
        }
        if (endsWithDot) "$formatted." else formatted
    } else {
        price
    }
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
                text = stringResource(id = R.string.addon_form_free_plan_limit_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.addon_form_free_plan_limit_message),
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
                    text = stringResource(id = R.string.addon_form_contact_us),
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
                    text = stringResource(id = R.string.addon_group_management_cancel),
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

