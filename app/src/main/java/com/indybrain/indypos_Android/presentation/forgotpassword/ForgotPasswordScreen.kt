package com.indybrain.indypos_Android.presentation.forgotpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.ui.theme.*

/**
 * Forgot Password Screen implementing MVI pattern
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    // Track if we should show success dialog
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Listen to success state
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            showSuccessDialog = true
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BaseBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "กลับ",
                        tint = PrimaryText
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.logo_appstore),
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = stringResource(R.string.forgot_password_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Largester
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Description
            Text(
                text = stringResource(R.string.forgot_password_description),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = PlaceholderText,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Email Label
            Text(
                text = stringResource(R.string.forgot_password_email_label),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Small
                ),
                color = PrimaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )
            
            // Email Field
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { email ->
                    viewModel.handleIntent(ForgotPasswordIntent.UpdateEmail(email))
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.forgot_password_email_placeholder),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PlaceholderText
                    )
                },
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = SecondaryButton,
                    focusedLabelColor = PrimaryText,
                    unfocusedLabelColor = PrimaryText
                ),
                textStyle = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                )
            )
            
            // Submit Button
            Button(
                onClick = {
                    viewModel.handleIntent(ForgotPasswordIntent.Submit)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButton,
                    contentColor = Color.White,
                    disabledContainerColor = SecondaryButton
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.forgot_password_submit_button),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Back to Login Button
            Text(
                text = stringResource(R.string.forgot_password_back_to_login),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Small
                ),
                color = SecondaryButton,
                modifier = Modifier.clickable(
                    enabled = !uiState.isLoading,
                    onClick = onBackClick
                )
            )
        }
    }
    
    // Error dialog
    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                viewModel.handleIntent(ForgotPasswordIntent.ClearError)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(ForgotPasswordIntent.ClearError)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_button_ok),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        )
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_error_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    )
                )
            },
            text = {
                Text(
                    text = getLocalizedErrorMessage(errorMessage),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    )
                )
            }
        )
    }
    
    // Success dialog - แสดงเมื่อ API สำเร็จ
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                // Don't dismiss on outside click
            },
            title = {
                Text(
                    text = stringResource(R.string.home_success_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.forgot_password_success_message),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.handleIntent(ForgotPasswordIntent.ClearError)
                        onBackClick()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.dialog_button_ok),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun getLocalizedErrorMessage(errorMessage: String): String {
    // Get localized string if errorMessage is a resource key
    val resourceId = when (errorMessage) {
        "forgot_password_error_email_required" -> R.string.forgot_password_error_email_required
        "forgot_password_error_email_invalid" -> R.string.forgot_password_error_email_invalid
        "forgot_password_error_generic" -> R.string.forgot_password_error_generic
        else -> null
    }
    
    return if (resourceId != null) {
        stringResource(resourceId)
    } else {
        errorMessage
    }
}

