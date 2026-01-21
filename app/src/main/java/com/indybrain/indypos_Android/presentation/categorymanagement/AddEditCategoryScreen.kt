package com.indybrain.indypos_Android.presentation.categorymanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Add/Edit Category Screen
 * @param categoryId If provided, screen is in edit mode. If null, screen is in add mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    categoryId: String? = null,
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    viewModel: AddEditCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditMode = categoryId != null
    var isSaveInProgress by remember { mutableStateOf(false) }
    
    // Load category data if in edit mode
    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            viewModel.loadCategory(categoryId)
        }
    }

    // Reset save-in-progress flag when loading finishes
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isSaveInProgress = false
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            id = if (isEditMode) 
                                R.string.category_management_edit_title 
                            else 
                                R.string.category_management_add_category
                        ),
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category Name Label with red asterisk
                val labelText = stringResource(id = R.string.category_management_name_label)
                val annotatedLabel = buildAnnotatedString {
                    // Extract base text (remove asterisk if present)
                    val baseText = if (labelText.endsWith(" *")) {
                        labelText.substring(0, labelText.length - 2)
                    } else {
                        labelText
                    }
                    append(baseText)
                    append(" ")
                    withStyle(style = SpanStyle(color = Color(0xFFE83808))) {
                        append("*")
                    }
                }
                Text(
                    text = annotatedLabel,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Category Name Input Field
                OutlinedTextField(
                    value = uiState.categoryName,
                    onValueChange = { viewModel.updateCategoryName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.category_management_name_placeholder),
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
                                viewModel.saveCategory {}
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
                    Text(
                        text = stringResource(
                            id = if (isEditMode) 
                                R.string.category_management_save_changes 
                            else 
                                R.string.category_management_add_category
                        ),
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
        
        // Success Dialog
        if (uiState.isSuccess) {
            SuccessDialog(
                isEditMode = isEditMode,
                onOkClick = {
                    viewModel.dismissSuccessDialog()
                    onSaveSuccess()
                }
            )
        }
        
        // Error Dialog - Show API errors as popup
        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
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
                        text = error,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
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
}

/**
 * Success Dialog for Add/Edit Category
 */
@Composable
private fun SuccessDialog(
    isEditMode: Boolean,
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
                text = stringResource(
                    id = if (isEditMode) 
                        R.string.category_management_edit_success 
                    else 
                        R.string.category_management_add_success
                ),
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

