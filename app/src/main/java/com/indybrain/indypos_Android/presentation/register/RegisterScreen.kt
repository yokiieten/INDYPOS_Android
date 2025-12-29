package com.indybrain.indypos_Android.presentation.register

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Register Screen implementing MVI pattern
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onRegistrationSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Navigate to home when registration is successful
    LaunchedEffect(uiState.isRegistrationSuccess) {
        if (uiState.isRegistrationSuccess) {
            onRegistrationSuccess()
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
                Text(
                    text = stringResource(R.string.register_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Guidance text
            Text(
                text = stringResource(R.string.register_guidance),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = PlaceholderText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // User Registration Section
            SectionHeader(text = stringResource(R.string.register_user_registration))
            
            // Username
            FormField(
                label = stringResource(R.string.register_username),
                value = uiState.username,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateUsername(it)) },
                placeholder = stringResource(R.string.register_username_placeholder),
                isRequired = true,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Text
            )
            
            // Password
            PasswordFormField(
                label = stringResource(R.string.register_password),
                value = uiState.password,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdatePassword(it)) },
                placeholder = stringResource(R.string.register_password_placeholder),
                isPasswordVisible = uiState.isPasswordVisible,
                onTogglePasswordVisibility = {
                    viewModel.handleIntent(RegisterIntent.TogglePasswordVisibility)
                },
                isRequired = true,
                enabled = !uiState.isLoading
            )
            
            // Confirm Password
            PasswordFormField(
                label = stringResource(R.string.register_confirm_password),
                value = uiState.confirmPassword,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateConfirmPassword(it)) },
                placeholder = stringResource(R.string.register_confirm_password_placeholder),
                isPasswordVisible = uiState.isConfirmPasswordVisible,
                onTogglePasswordVisibility = {
                    viewModel.handleIntent(RegisterIntent.ToggleConfirmPasswordVisibility)
                },
                isRequired = true,
                enabled = !uiState.isLoading
            )
            
            // First Name
            FormField(
                label = stringResource(R.string.register_first_name),
                value = uiState.firstName,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateFirstName(it)) },
                placeholder = stringResource(R.string.register_first_name_placeholder),
                isRequired = true,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Text
            )
            
            // Last Name
            FormField(
                label = stringResource(R.string.register_last_name),
                value = uiState.lastName,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateLastName(it)) },
                placeholder = stringResource(R.string.register_last_name_placeholder),
                isRequired = true,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Text
            )
            
            // Email
            FormField(
                label = stringResource(R.string.register_email),
                value = uiState.email,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateEmail(it)) },
                placeholder = stringResource(R.string.register_email_placeholder),
                isRequired = true,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Email
            )
            
            // Phone
            PhoneFormField(
                label = stringResource(R.string.register_phone),
                value = uiState.phone,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdatePhone(it)) },
                placeholder = stringResource(R.string.register_phone_placeholder),
                isRequired = true,
                enabled = !uiState.isLoading
            )
            
            // Gender
            GenderFormField(
                label = stringResource(R.string.register_gender),
                selectedGender = uiState.gender,
                onGenderSelected = { viewModel.handleIntent(RegisterIntent.UpdateGender(it)) },
                isRequired = true,
                enabled = !uiState.isLoading
            )
            
            // Birth Date
            BirthDateFormField(
                label = stringResource(R.string.register_birth_date),
                value = uiState.birthDate,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateBirthDate(it)) },
                placeholder = stringResource(R.string.register_birth_date_placeholder),
                isRequired = true,
                enabled = !uiState.isLoading
            )
            
            // Store Information Section
            SectionHeader(text = stringResource(R.string.register_store_information))
            
            // Shop Name
            FormField(
                label = stringResource(R.string.register_store_name_title),
                value = uiState.shopName,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateShopName(it)) },
                placeholder = stringResource(R.string.register_store_name_placeholder),
                isRequired = true,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Text
            )
            
            // Shop Description
            ShopDescriptionFormField(
                label = stringResource(R.string.register_store_description_optional),
                value = uiState.shopDescription,
                onValueChange = { viewModel.handleIntent(RegisterIntent.UpdateShopDescription(it)) },
                placeholder = stringResource(R.string.register_description_placeholder),
                isRequired = false,
                enabled = !uiState.isLoading
            )
            
            // Terms & Privacy Section
            CheckboxFormField(
                label = stringResource(R.string.register_terms_acceptance),
                isChecked = uiState.termsAccepted,
                onCheckedChange = { viewModel.handleIntent(RegisterIntent.UpdateTermsAccepted(it)) },
                linkText = stringResource(R.string.register_terms_link_text),
                onLinkClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/indypos-termsofuse"))
                    context.startActivity(intent)
                },
                enabled = !uiState.isLoading
            )
            
            CheckboxFormField(
                label = stringResource(R.string.register_privacy_acceptance),
                isChecked = uiState.privacyAccepted,
                onCheckedChange = { viewModel.handleIntent(RegisterIntent.UpdatePrivacyAccepted(it)) },
                linkText = stringResource(R.string.register_privacy_link_text),
                onLinkClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/indypos-privacy"))
                    context.startActivity(intent)
                },
                enabled = !uiState.isLoading
            )
            
            // Register Button
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.handleIntent(RegisterIntent.Register)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2), // Light blue color matching the image
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
                        text = stringResource(R.string.register_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Error dialog
    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                viewModel.handleIntent(RegisterIntent.ClearError)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleIntent(RegisterIntent.ClearError)
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
}

@Composable
private fun getLocalizedErrorMessage(errorMessage: String): String {
    // Get localized string if errorMessage is a resource key
    val resourceId = when (errorMessage) {
        "register_validation_username_required" -> R.string.register_validation_username_required
        "register_validation_username_too_short" -> R.string.register_validation_username_too_short
        "register_validation_username_too_long" -> R.string.register_validation_username_too_long
        "register_validation_password_required" -> R.string.register_validation_password_required
        "register_validation_password_too_short" -> R.string.register_validation_password_too_short
        "register_validation_password_too_long" -> R.string.register_validation_password_too_long
        "register_validation_confirm_password_required" -> R.string.register_validation_confirm_password_required
        "register_validation_passwords_not_match" -> R.string.register_validation_passwords_not_match
        "register_validation_first_name_required" -> R.string.register_validation_first_name_required
        "register_validation_first_name_too_short" -> R.string.register_validation_first_name_too_short
        "register_validation_last_name_required" -> R.string.register_validation_last_name_required
        "register_validation_email_required" -> R.string.register_validation_email_required
        "register_validation_email_invalid" -> R.string.register_validation_email_invalid
        "register_validation_phone_required" -> R.string.register_validation_phone_required
        "register_validation_phone_invalid" -> R.string.register_validation_phone_invalid
        "register_validation_birth_date_required" -> R.string.register_validation_birth_date_required
        "register_validation_birth_date_under_16" -> R.string.register_validation_birth_date_under_16
        "register_validation_gender_required" -> R.string.register_validation_gender_required
        "register_validation_shop_name_required" -> R.string.register_validation_shop_name_required
        "register_validation_shop_name_too_short" -> R.string.register_validation_shop_name_too_short
        "register_validation_shop_name_too_long" -> R.string.register_validation_shop_name_too_long
        "register_validation_description_too_short" -> R.string.register_validation_description_too_short
        "register_validation_description_too_long" -> R.string.register_validation_description_too_long
        "register_validation_terms_required" -> R.string.register_validation_terms_required
        "register_validation_privacy_required" -> R.string.register_validation_privacy_required
        "register_error_validation_failed" -> R.string.register_error_validation_failed
        "register_error_generic" -> R.string.register_error_generic
        "register_error_email_exists" -> R.string.register_error_email_exists
        "register_error_phone_exists" -> R.string.register_error_phone_exists
        "register_error_username_exists" -> R.string.register_error_username_exists
        "register_error_profile_verification" -> R.string.register_error_profile_verification
        else -> null
    }
    
    return if (resourceId != null) {
        stringResource(resourceId)
    } else {
        errorMessage
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = FontUtils.mainFont(
            style = AppFontStyle.Bold,
            size = FontSize.Large
        ),
        color = PrimaryText,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isRequired: Boolean,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isRequired) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = PrimaryText)) {
                        append(label)
                    }
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append(" *")
                    }
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = label,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
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
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
private fun PasswordFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isRequired: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isRequired) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = PrimaryText)) {
                        append(label)
                    }
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append(" *")
                    }
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = label,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
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
private fun PhoneFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isRequired: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isRequired) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = PrimaryText)) {
                        append(label)
                    }
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append(" *")
                    }
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = label,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        var formattedValue by remember(value) { mutableStateOf(formatPhoneNumber(value)) }
        
        OutlinedTextField(
            value = formattedValue,
            onValueChange = { newValue ->
                formattedValue = formatPhoneNumber(newValue)
                onValueChange(formattedValue.replace("-", ""))
            },
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
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

private fun formatPhoneNumber(phone: String): String {
    val digitsOnly = phone.replace("-", "").filter { it.isDigit() }
    val limitedDigits = digitsOnly.take(10)
    
    return buildString {
        limitedDigits.forEachIndexed { index, digit ->
            if (index == 3 || index == 6) {
                append("-")
            }
            append(digit)
        }
    }
}

@Composable
private fun GenderFormField(
    label: String,
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    isRequired: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isRequired) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = PrimaryText)) {
                        append(label)
                    }
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append(" *")
                    }
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = label,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            val maleText = stringResource(R.string.register_gender_male)
            val femaleText = stringResource(R.string.register_gender_female)
            
            RadioButtonOption(
                text = maleText,
                isSelected = selectedGender == maleText,
                onClick = { onGenderSelected(maleText) },
                enabled = enabled
            )
            
            RadioButtonOption(
                text = femaleText,
                isSelected = selectedGender == femaleText,
                onClick = { onGenderSelected(femaleText) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun RadioButtonOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            enabled = enabled
        )
        Text(
            text = text,
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Large
            ),
            color = if (isSelected) PrimaryButton else PrimaryText
        )
    }
}

@Composable
private fun BirthDateFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isRequired: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(modifier = modifier) {
        if (isRequired) {
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = PrimaryText)) {
                        append(label)
                    }
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append(" *")
                    }
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = label,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    if (enabled) {
                        showNativeDatePicker(
                            context = context,
                            currentValue = value,
                            onDateSelected = { year, month, dayOfMonth ->
                                val dateString = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                onValueChange(dateString)
                            }
                        )
                    }
                },
            color = Color.Transparent
        ) {
            OutlinedTextField(
                value = if (value.isNotEmpty()) formatBirthDateForDisplay(value) else "",
                onValueChange = { },
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
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Select date",
                        tint = PrimaryText
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PrimaryText,
                    unfocusedTextColor = PrimaryText,
                    focusedBorderColor = PrimaryButton,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedLabelColor = PrimaryText,
                    unfocusedLabelColor = PrimaryText,
                    disabledTextColor = PrimaryText,
                    disabledBorderColor = Color(0xFFE0E0E0),
                    disabledPlaceholderColor = PlaceholderText
                ),
                textStyle = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                )
            )
        }
    }
}

private fun showNativeDatePicker(
    context: android.content.Context,
    currentValue: String,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    
    // Parse current value if exists, otherwise use current date
    if (currentValue.isNotEmpty()) {
        try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = formatter.parse(currentValue)
            if (date != null) {
                calendar.time = date
            }
        } catch (e: Exception) {
            // If parsing fails, use current date
        }
    }
    
    val initialYear = calendar.get(Calendar.YEAR)
    val initialMonth = calendar.get(Calendar.MONTH)
    val initialDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    
    // Calculate max and min dates (16 years old minimum, 120 years old maximum)
    val maxDate = Calendar.getInstance().apply {
        add(Calendar.YEAR, -16)
    }
    val minDate = Calendar.getInstance().apply {
        add(Calendar.YEAR, -120)
    }
    
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(year, month, dayOfMonth)
        },
        initialYear,
        initialMonth,
        initialDayOfMonth
    )
    
    datePickerDialog.datePicker.maxDate = maxDate.timeInMillis
    datePickerDialog.datePicker.minDate = minDate.timeInMillis
    
    datePickerDialog.show()
}


private fun formatBirthDateForDisplay(isoDate: String): String {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = formatter.parse(isoDate) ?: return isoDate
        val displayFormatter = SimpleDateFormat("d MMMM yyyy", Locale("th", "TH"))
        displayFormatter.format(date)
    } catch (e: Exception) {
        isoDate
    }
}

@Composable
private fun ShopDescriptionFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isRequired: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = FontUtils.mainFont(
                style = AppFontStyle.Bold,
                size = FontSize.Medium
            ),
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
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
            maxLines = 5,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
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
private fun CheckboxFormField(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    linkText: String,
    onLinkClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        
        val fullText = label
        val linkStartIndex = fullText.indexOf(linkText)
        
        if (linkStartIndex >= 0) {
            Text(
                buildAnnotatedString {
                    append(fullText.substring(0, linkStartIndex))
                    withStyle(style = SpanStyle(color = PrimaryButton)) {
                        append(linkText)
                    }
                    append(fullText.substring(linkStartIndex + linkText.length))
                },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = enabled) {
                        onLinkClick()
                    }
            )
        } else {
            Text(
                text = fullText,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

