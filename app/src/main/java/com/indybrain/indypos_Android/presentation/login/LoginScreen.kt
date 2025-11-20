package com.indybrain.indypos_Android.presentation.login

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.ui.theme.*

/**
 * Login Screen implementing MVI pattern
 * Designed to match the provided UI mockup
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {}
) {
    // Collect UI state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 48.dp else 24.dp
    val scrollState = rememberScrollState()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BaseBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = horizontalPadding)
                .padding(vertical = if (isLandscape) 24.dp else 0.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 40.dp))
            
            // App Logo
            LogoSection()
            
            Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 40.dp))
            
            // Login Title
            Text(
                text = stringResource(R.string.login_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Largester
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Email Field
            EmailField(
                email = uiState.email,
                onEmailChange = { email ->
                    viewModel.handleIntent(LoginIntent.UpdateEmail(email))
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // Password Field
            PasswordField(
                password = uiState.password,
                onPasswordChange = { password ->
                    viewModel.handleIntent(LoginIntent.UpdatePassword(password))
                },
                isPasswordVisible = uiState.isPasswordVisible,
                onTogglePasswordVisibility = {
                    viewModel.handleIntent(LoginIntent.TogglePasswordVisibility)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            // Login Button
            Button(
                onClick = {
                    viewModel.handleIntent(LoginIntent.Login)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
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
                        text = stringResource(R.string.login_button),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Forgot Password Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.forgot_password),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = PlaceholderText,
                    modifier = Modifier.clickable(
                        enabled = !uiState.isLoading,
                        onClick = onForgotPasswordClick
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 48.dp))
            
            // Create Account Link
            CreateAccountLink(
                onCreateAccountClick = onCreateAccountClick,
                modifier = Modifier.padding(bottom = if (isLandscape) 24.dp else 40.dp)
            )
        }
    }

    // Error dialog
    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                viewModel.handleIntent(LoginIntent.ClearError)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(LoginIntent.ClearError)
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
                    text = errorMessage.ifBlank {
                        stringResource(R.string.dialog_error_message_generic)
                    },
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    )
                )
            }
        )
    }

    // Success dialog
    if (uiState.isLoginSuccess) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(LoginIntent.AcknowledgeSuccess)
                        onLoginSuccess()
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
                    text = stringResource(R.string.dialog_success_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    )
                )
            },
            text = {
                Text(
                    text = uiState.successMessage ?: stringResource(R.string.dialog_success_message),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    )
                )
            }
        )
    }
}

@Composable
fun LogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo Image (using app logo)
        Image(
            painter = painterResource(id = R.drawable.logo_appstore),
            contentDescription = "App Logo",
            modifier = Modifier.size(80.dp)
        )
    }
}

@Composable
fun EmailField(
    email: String,
    onEmailChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Label
        Text(
            text = stringResource(R.string.email_label),
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            ),
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Input Field
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.email_placeholder),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PlaceholderText
                )
            },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText,
                focusedBorderColor = PrimaryButton,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = PrimaryText,
                unfocusedLabelColor = PrimaryText
            ),
            textStyle = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            )
        )
    }
}

@Composable
fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Label
        Text(
            text = stringResource(R.string.password_label),
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            ),
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Input Field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.password_placeholder),
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
                                R.drawable.ic_close_eye
                            } else {
                                R.drawable.ic_open_eye
                            }
                        ),
                        contentDescription = if (isPasswordVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        },
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText,
                focusedBorderColor = PrimaryButton,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = PrimaryText,
                unfocusedLabelColor = PrimaryText
            ),
            textStyle = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            )
        )
    }
}

@Composable
fun CreateAccountLink(
    onCreateAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noAccountText = stringResource(R.string.no_account)
    val createAccountText = stringResource(R.string.create_account)
    
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(color = PlaceholderText)) {
                append(noAccountText)
                append(" ")
            }
            withStyle(style = SpanStyle(color = PrimaryButton)) {
                append(createAccountText)
            }
        },
        style = FontUtils.mainFont(
            style = AppFontStyle.Regular,
            size = FontSize.Medium
        ),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCreateAccountClick)
    )
}
