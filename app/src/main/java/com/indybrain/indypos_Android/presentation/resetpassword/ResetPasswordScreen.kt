package com.indybrain.indypos_Android.presentation.resetpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.ui.theme.*

/**
 * Reset Password Screen implementing MVI pattern
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    token: String,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    // Set token and verify when screen is first composed
    LaunchedEffect(token) {
        viewModel.setTokenAndVerify(token)
    }
    
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show loading indicator while verifying token
            if (uiState.isVerifyingToken) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.logo_appstore),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(80.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = PrimaryButton,
                        strokeWidth = 4.dp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(R.string.reset_password_verifying_token),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = PlaceholderText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (uiState.isTokenVerified) {
                // Show password reset form only after token is verified
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                        text = stringResource(R.string.reset_password_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Largester
                        ),
                        color = PrimaryText,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Description
                    Text(
                        text = stringResource(R.string.reset_password_description),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = PlaceholderText,
                        modifier = Modifier.padding(bottom = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
            
            // New Password Label
            Text(
                text = stringResource(R.string.reset_password_new_password_label),
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
            
            // New Password Field
            PasswordField(
                password = uiState.newPassword,
                onPasswordChange = { password ->
                    viewModel.handleIntent(ResetPasswordIntent.UpdateNewPassword(password))
                },
                isPasswordVisible = uiState.isNewPasswordVisible,
                onTogglePasswordVisibility = {
                    viewModel.handleIntent(ResetPasswordIntent.ToggleNewPasswordVisibility)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // Confirm Password Label
            Text(
                text = stringResource(R.string.reset_password_confirm_password_label),
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
            
                    // Confirm Password Field
                    PasswordField(
                        password = uiState.confirmPassword,
                        onPasswordChange = { password ->
                            viewModel.handleIntent(ResetPasswordIntent.UpdateConfirmPassword(password))
                        },
                        isPasswordVisible = uiState.isConfirmPasswordVisible,
                        onTogglePasswordVisibility = {
                            viewModel.handleIntent(ResetPasswordIntent.ToggleConfirmPasswordVisibility)
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                    
                    // Submit Button
                    Button(
                        onClick = {
                            viewModel.handleIntent(ResetPasswordIntent.Submit)
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
                                text = stringResource(R.string.reset_password_submit_button),
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
                        text = stringResource(R.string.reset_password_back_to_login),
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
        }
    }
    
    // Error dialog - dismiss only when user taps OK (not auto-dismiss)
    uiState.errorMessage?.let { errorMessage ->
        val shouldNavigateBack = uiState.shouldNavigateBack
        AlertDialog(
            onDismissRequest = {
                if (!shouldNavigateBack) {
                    viewModel.handleIntent(ResetPasswordIntent.ClearError)
                }
            },
            properties = DialogProperties(
                dismissOnClickOutside = !shouldNavigateBack,
                dismissOnBackPress = !shouldNavigateBack
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(ResetPasswordIntent.ClearError)
                        if (shouldNavigateBack) {
                            onBackClick()
                        }
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
                    text = stringResource(R.string.reset_password_success_message),
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
                        viewModel.handleIntent(ResetPasswordIntent.ClearError)
                        onSuccess()
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
private fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        placeholder = {
            Text(
                text = stringResource(R.string.reset_password_password_placeholder),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PlaceholderText
            )
        },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(
                onClick = onTogglePasswordVisibility,
                enabled = enabled
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPasswordVisible) {
                            R.drawable.ic_open_eye
                        } else {
                            R.drawable.ic_close_eye
                        }
                    ),
                    contentDescription = if (isPasswordVisible) {
                        "Hide password"
                    } else {
                        "Show password"
                    },
                    tint = Color.Unspecified
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
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
}

@Composable
private fun getLocalizedErrorMessage(errorMessage: String): String {
    // Get localized string if errorMessage is a resource key
    val resourceId = when (errorMessage) {
        "reset_password_error_password_required" -> R.string.reset_password_error_password_required
        "reset_password_error_password_too_short" -> R.string.reset_password_error_password_too_short
        "reset_password_error_confirm_password_required" -> R.string.reset_password_error_confirm_password_required
        "reset_password_error_passwords_not_match" -> R.string.reset_password_error_passwords_not_match
        "reset_password_error_token_missing" -> R.string.reset_password_error_token_missing
        "reset_password_error_token_expired" -> R.string.reset_password_error_token_expired
        "reset_password_error_token_not_verified" -> R.string.reset_password_error_token_not_verified
        "reset_password_error_generic" -> R.string.reset_password_error_generic
        else -> null
    }
    
    return if (resourceId != null) {
        stringResource(resourceId)
    } else {
        errorMessage
    }
}

