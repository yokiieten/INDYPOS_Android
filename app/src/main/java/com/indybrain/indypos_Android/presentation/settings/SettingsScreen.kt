package com.indybrain.indypos_Android.presentation.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.presentation.navigation.HomeBottomDestination
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Settings Screen displaying all available settings options
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSettingsItemClick: (SettingsItem) -> Unit = {},
    onLogoutSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    // Navigate to login when logout is successful
    LaunchedEffect(uiState.isLogoutSuccess) {
        if (uiState.isLogoutSuccess) {
            viewModel.resetLogoutSuccess()
            onLogoutSuccess()
        }
    }
    
    Scaffold(
        containerColor = BaseBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = stringResource(id = R.string.settings_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Settings List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SettingsItem.values().forEach { item ->
                    SettingsItemRow(
                        item = item,
                        onClick = {
                            if (item == SettingsItem.Logout) {
                                viewModel.onLogoutClick()
                            } else {
                                onSettingsItemClick(item)
                            }
                        }
                    )
                }
            }
            
            // Add bottom padding to ensure logout button is visible above bottom navigation
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
    
    // Logout Confirmation Dialog
    if (uiState.showLogoutConfirmationDialog) {
        LogoutConfirmationDialog(
            isLoggingOut = uiState.isLoggingOut,
            onConfirm = { viewModel.confirmLogout() },
            onDismiss = { viewModel.cancelLogout() }
        )
    }
    
    // No Internet Dialog
    if (uiState.showNoInternetDialog) {
        NoInternetDialog(
            onDismiss = { viewModel.dismissNoInternetDialog() }
        )
    }
    
    // Logout Success Dialog
    if (uiState.showLogoutSuccessDialog) {
        LogoutSuccessDialog(
            onOkClick = { viewModel.onLogoutSuccessDialogOk() }
        )
    }
    
    // Error Dialog
    uiState.errorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearError() }
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
}

/**
 * Individual settings item row
 */
@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Text
            Text(
                text = stringResource(id = item.labelRes),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.weight(1f)
            )
            
            // Chevron
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = PlaceholderText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Enum class representing all settings items
 */
enum class SettingsItem(
    val icon: ImageVector,
    val labelRes: Int
) {
    Account(
        icon = Icons.Outlined.AccountCircle,
        labelRes = R.string.settings_account
    ),
    ChangePassword(
        icon = Icons.Outlined.Key,
        labelRes = R.string.settings_change_password
    ),
    Language(
        icon = Icons.Outlined.Language,
        labelRes = R.string.settings_language
    ),
    SalesSettings(
        icon = Icons.Outlined.ShoppingCart,
        labelRes = R.string.settings_sales
    ),
    PrinterSettings(
        icon = Icons.Outlined.Print,
        labelRes = R.string.settings_printer
    ),
    ManageStock(
        icon = Icons.Outlined.Inventory2,
        labelRes = R.string.settings_manage_stock
    ),
    ReceiptSettings(
        icon = Icons.Outlined.Receipt,
        labelRes = R.string.settings_receipt
    ),
    ManageData(
        icon = Icons.Outlined.Cloud,
        labelRes = R.string.settings_manage_data
    ),
    ContactUs(
        icon = Icons.Outlined.ContactSupport,
        labelRes = R.string.settings_contact_us
    ),
    Logout(
        icon = Icons.Outlined.Logout,
        labelRes = R.string.settings_logout
    )
}

/**
 * Logout Confirmation Dialog
 */
@Composable
private fun LogoutConfirmationDialog(
    isLoggingOut: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoggingOut) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.logout_confirm_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.logout_confirm_message),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Warning message with icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9500), // Orange warning color
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.logout_warning_message),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = SecondaryText,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            if (isLoggingOut) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = RedFailure,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.logout_button),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = RedFailure
                    )
                }
            } else {
                TextButton(
                    onClick = onConfirm
                ) {
                    Text(
                        text = stringResource(R.string.logout_button),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = RedFailure
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoggingOut
            ) {
                Text(
                    text = stringResource(R.string.logout_cancel),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        }
    )
}

/**
 * No Internet Connection Dialog
 */
@Composable
private fun NoInternetDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.logout_no_internet_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Text(
                text = stringResource(R.string.logout_no_internet_message),
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
                    text = stringResource(R.string.dialog_button_ok),
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
 * Logout Success Dialog
 */
@Composable
private fun LogoutSuccessDialog(
    onOkClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing by clicking outside */ },
        title = {
            Text(
                text = stringResource(R.string.logout_success_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Text(
                text = stringResource(R.string.logout_success_message),
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
                    text = stringResource(R.string.dialog_button_ok),
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

