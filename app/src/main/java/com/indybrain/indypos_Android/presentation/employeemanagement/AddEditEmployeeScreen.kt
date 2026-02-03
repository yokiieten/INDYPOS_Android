package com.indybrain.indypos_Android.presentation.employeemanagement

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmployeeScreen(
    onBackClick: () -> Unit = {},
    onSaveSuccess: () -> Unit = {},
    viewModel: AddEditEmployeeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEditMode = uiState.employeeId != null
    var saveInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) saveInProgress = false
    }

    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            id = if (isEditMode) R.string.employee_add_edit_title_edit
                            else R.string.employee_add_edit_title_add
                        ),
                        style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Large),
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BaseBackground, titleContentColor = PrimaryText)
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (!isEditMode) {
                    fieldLabel(stringResource(R.string.employee_add_edit_username))
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.employee_add_edit_username_placeholder), style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium), color = PlaceholderText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = textFieldColors(),
                        enabled = !uiState.isLoading
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    fieldLabel(stringResource(R.string.employee_add_edit_username))
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = textFieldColors(),
                        enabled = false
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                fieldLabel(stringResource(R.string.employee_add_edit_first_name))
                OutlinedTextField(
                    value = uiState.firstName,
                    onValueChange = { viewModel.updateFirstName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.employee_add_edit_first_name_placeholder), style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium), color = PlaceholderText) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = textFieldColors(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                fieldLabel(stringResource(R.string.employee_add_edit_last_name))
                OutlinedTextField(
                    value = uiState.lastName,
                    onValueChange = { viewModel.updateLastName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.employee_add_edit_last_name_placeholder), style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium), color = PlaceholderText) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = textFieldColors(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                fieldLabel(stringResource(R.string.employee_add_edit_email))
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.employee_add_edit_email_placeholder), style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium), color = PlaceholderText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(8.dp),
                    colors = textFieldColors(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                fieldLabel(stringResource(R.string.employee_add_edit_phone))
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = { newValue ->
                        if (newValue.length <= 10) {
                            viewModel.updatePhone(newValue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.employee_add_edit_phone_placeholder), style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium), color = PlaceholderText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(8.dp),
                    colors = textFieldColors(),
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (!isEditMode) {
                    fieldLabel(stringResource(R.string.employee_add_edit_password))
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.employee_add_edit_password_placeholder), style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium), color = PlaceholderText) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = textFieldColors(),
                        enabled = !uiState.isLoading
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = stringResource(R.string.employee_add_edit_permissions),
                    style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
                    color = PrimaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                viewModel.availablePermissions.forEach { permission ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isLoading) { viewModel.togglePermission(permission) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = permission in uiState.selectedPermissions,
                            onCheckedChange = { viewModel.togglePermission(permission) },
                            enabled = !uiState.isLoading,
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryButton)
                        )
                        Text(
                            text = getPermissionLabel(permission),
                            style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium),
                            color = PrimaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        enabled = !uiState.isLoading && !saveInProgress,
                        onClick = {
                            if (!saveInProgress) {
                                saveInProgress = true
                                viewModel.save()
                            }
                        }
                    ),
                color = if (uiState.isLoading) PrimaryButton.copy(alpha = 0.6f) else PrimaryButton
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(24.dp))
                    } else {
                        Text(
                            text = stringResource(R.string.employee_add_edit_save),
                            style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (uiState.isSuccess) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_success_title),
                        style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Large),
                        color = PrimaryText
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            id = if (isEditMode) R.string.employee_add_edit_success_updated
                            else R.string.employee_add_edit_success_created
                        ),
                        style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissSuccess()
                        onSaveSuccess()
                    }) {
                        Text(stringResource(R.string.dialog_button_ok), style = FontUtils.mainFont(style = AppFontStyle.Medium, size = FontSize.Medium), color = PrimaryButton)
                    }
                }
            )
        }

        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError(); saveInProgress = false },
                title = {
                    Text(stringResource(R.string.dialog_error_title), style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Large), color = PrimaryText)
                },
                text = {
                    Text(error, style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium), color = SecondaryText)
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError(); saveInProgress = false }) {
                        Text(stringResource(R.string.dialog_button_ok), style = FontUtils.mainFont(style = AppFontStyle.Medium, size = FontSize.Medium), color = PrimaryButton)
                    }
                }
            )
        }
    }
}

@Composable
private fun fieldLabel(text: String) {
    Text(
        text = text,
        style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium),
        color = PrimaryText,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White,
    unfocusedBorderColor = PrimaryButton.copy(alpha = 0.5f),
    focusedBorderColor = PrimaryButton
)

@Composable
private fun getPermissionLabel(permission: String): String {
    return when (permission) {
        "order.create" -> stringResource(R.string.permission_order_create)
        "order.cancel" -> stringResource(R.string.permission_order_cancel)
        "report.view" -> stringResource(R.string.permission_report_view)
        "role.manage" -> stringResource(R.string.permission_role_manage)
        else -> permission
    }
}
