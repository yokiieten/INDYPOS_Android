package com.indybrain.indypos_Android.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.domain.model.CreateEmployeeRequest
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText

private data class RoleOption(val id: Int, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEmployeeScreen(
    onBackClick: () -> Unit = {},
    onCreateSuccess: () -> Unit = {},
    viewModel: CreateEmployeeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var username by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    var selectedRoleIndex by rememberSaveable { mutableStateOf(0) }

    val roleOptions = listOf(
        RoleOption(2, stringResource(id = R.string.create_employee_role_staff)),
        RoleOption(1, stringResource(id = R.string.create_employee_role_manager))
    )
    val selectedRole = roleOptions[selectedRoleIndex.coerceIn(roleOptions.indices)]
    val isFormValid = username.isNotBlank() &&
        firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        email.isNotBlank() &&
        phone.isNotBlank() &&
        password.isNotBlank()

    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.create_employee_title),
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                text = stringResource(id = R.string.create_employee_subtitle),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.create_employee_required_hint),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(text = stringResource(id = R.string.create_employee_section_account))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FormField(
                        label = stringResource(id = R.string.create_employee_username_label),
                        value = username,
                        onValueChange = { username = it },
                        placeholder = stringResource(id = R.string.create_employee_username_placeholder),
                        isRequired = true
                    )

                    PasswordField(
                        label = stringResource(id = R.string.create_employee_password_label),
                        value = password,
                        onValueChange = { password = it },
                        placeholder = stringResource(id = R.string.create_employee_password_placeholder),
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(text = stringResource(id = R.string.create_employee_section_profile))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormField(
                            label = stringResource(id = R.string.create_employee_first_name_label),
                            value = firstName,
                            onValueChange = { firstName = it },
                            placeholder = stringResource(id = R.string.create_employee_first_name_placeholder),
                            isRequired = true,
                            modifier = Modifier.weight(1f)
                        )

                        FormField(
                            label = stringResource(id = R.string.create_employee_last_name_label),
                            value = lastName,
                            onValueChange = { lastName = it },
                            placeholder = stringResource(id = R.string.create_employee_last_name_placeholder),
                            isRequired = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FormField(
                        label = stringResource(id = R.string.create_employee_email_label),
                        value = email,
                        onValueChange = { email = it },
                        placeholder = stringResource(id = R.string.create_employee_email_placeholder),
                        isRequired = true
                    )

                    FormField(
                        label = stringResource(id = R.string.create_employee_phone_label),
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = stringResource(id = R.string.create_employee_phone_placeholder),
                        isRequired = true
                    )

                    Column {
                        RequiredLabel(text = stringResource(id = R.string.create_employee_role_label))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedRole.label,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = {
                                    Text(
                                        text = stringResource(id = R.string.create_employee_role_placeholder),
                                        style = FontUtils.mainFont(
                                            style = AppFontStyle.Regular,
                                            size = FontSize.Medium
                                        ),
                                        color = PlaceholderText
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (roleExpanded) {
                                            Icons.Filled.KeyboardArrowUp
                                        } else {
                                            Icons.Filled.KeyboardArrowDown
                                        },
                                        contentDescription = null,
                                        tint = SecondaryText
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { roleExpanded = !roleExpanded },
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

                            DropdownMenu(
                                expanded = roleExpanded,
                                onDismissRequest = { roleExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                roleOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                style = FontUtils.mainFont(
                                                    style = AppFontStyle.Regular,
                                                    size = FontSize.Medium
                                                ),
                                                color = PrimaryText
                                            )
                                        },
                                        onClick = {
                                            selectedRoleIndex = index
                                            roleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        viewModel.createEmployee(
                            CreateEmployeeRequest(
                                username = username,
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone,
                                password = password,
                                roleId = selectedRole.id
                            )
                        )
                    }
                },
                enabled = isFormValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryButton,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    contentColor = Color.White,
                    disabledContentColor = PlaceholderText
                )
            ) {
                Text(
                    text = stringResource(id = R.string.create_employee_submit),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text(
                        text = stringResource(id = R.string.dialog_button_ok),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            },
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
            }
        )
    }

    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccess() },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissSuccess()
                        username = ""
                        firstName = ""
                        lastName = ""
                        email = ""
                        phone = ""
                        password = ""
                        selectedRoleIndex = 0
                        onCreateSuccess()
                    }
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
            },
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
                    text = stringResource(id = R.string.create_employee_success_message),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        )
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
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun RequiredLabel(text: String) {
    Text(
        text = "$text *",
        style = FontUtils.mainFont(
            style = AppFontStyle.Bold,
            size = FontSize.Medium
        ),
        color = PrimaryText,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isRequired: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isRequired) {
            RequiredLabel(text = label)
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
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit
) {
    Column {
        RequiredLabel(text = label)
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
            visualTransformation = if (isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        painter = painterResource(
                            id = if (isPasswordVisible) {
                                R.drawable.ic_open_eye
                            } else {
                                R.drawable.ic_close_eye
                            }
                        ),
                        contentDescription = null,
                        tint = Color.Unspecified
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
