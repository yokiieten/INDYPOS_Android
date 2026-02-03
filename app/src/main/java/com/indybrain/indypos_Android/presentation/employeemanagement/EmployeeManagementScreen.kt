package com.indybrain.indypos_Android.presentation.employeemanagement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import kotlinx.coroutines.launch

/**
 * Employee Management Screen
 * This screen allows users with proper permissions to manage employees
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    viewModel: EmployeeManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error message if present
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.employee_management_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() }
        ) {
            when {
                uiState.isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.employees.isEmpty() -> {
                    // Empty state
                    EmptyEmployeeListContent()
                }
                else -> {
                    // Employee list
                    EmployeeListContent(
                        employees = uiState.employees
                    )
                }
            }
        }
    }
}

/**
 * Empty state when there are no employees
 */
@Composable
private fun EmptyEmployeeListContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.employee_management_empty),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
        }
    }
}

/**
 * Employee list content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeListContent(
    employees: List<User>
) {
    var selectedEmployee by remember { mutableStateOf<User?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 20.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(employees, key = { it.id }) { employee ->
            EmployeeListItem(
                employee = employee,
                onClick = {
                    selectedEmployee = employee
                    showBottomSheet = true
                }
            )
        }
    }
    
    // Bottom Sheet for actions
    if (showBottomSheet && selectedEmployee != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            EmployeeActionBottomSheet(
                employee = selectedEmployee!!,
                onEdit = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                    // TODO: Navigate to edit screen
                },
                onDelete = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                        showDeleteDialog = true
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && selectedEmployee != null) {
        DeleteConfirmationDialog(
            employeeName = "${selectedEmployee?.firstName} ${selectedEmployee?.lastName}".trim()
                .ifEmpty { selectedEmployee?.username ?: "" },
            onConfirm = {
                // TODO: Implement delete
                showDeleteDialog = false
                selectedEmployee = null
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}

/**
 * Individual employee list item
 */
@Composable
private fun EmployeeListItem(
    employee: User,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(1.dp, Color(0xFFD0D0D0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Name (ชื่อ-นามสกุล)
            Text(
                text = "${employee.firstName ?: ""} ${employee.lastName ?: ""}".trim()
                    .ifEmpty { "ไม่ระบุชื่อ" },
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Email
            Text(
                text = employee.email,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = PrimaryText.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Phone
            employee.phone?.let { phone ->
                Text(
                    text = phone,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = PrimaryText.copy(alpha = 0.7f)
                )
            }
            
            // Role Name (ตำแหน่ง) - ใช้จาก API
            employee.roleName?.let { roleName ->
                Text(
                    text = roleName,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = PrimaryText.copy(alpha = 0.8f)
                )
            }
            
            // Shop Name (ร้าน)
            employee.shopName?.let { shopName ->
                Text(
                    text = shopName,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Bottom sheet for employee actions
 */
@Composable
private fun EmployeeActionBottomSheet(
    employee: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.employee_management_action_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.employee_management_action_subtitle),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = PrimaryText.copy(alpha = 0.7f)
            )
        }
        
        HorizontalDivider(color = Color(0xFFE0E0E0))
        
        // Edit action
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.employee_management_action_edit),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            }
        }
        
        HorizontalDivider(color = Color(0xFFE0E0E0))
        
        // Delete action
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDelete),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.employee_management_action_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = Color(0xFFD32F2F)
                )
            }
        }
        
        HorizontalDivider(color = Color(0xFFE0E0E0))
        
        // Cancel action
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDismiss),
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.employee_management_action_cancel),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Delete confirmation dialog
 */
@Composable
private fun DeleteConfirmationDialog(
    employeeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.employee_management_delete_confirm_title),
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
                    text = stringResource(
                        R.string.employee_management_delete_confirm_message,
                        employeeName
                    ),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.employee_management_delete_warning),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = Color(0xFFD32F2F)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.employee_management_action_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = Color(0xFFD32F2F)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.employee_management_action_cancel),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText.copy(alpha = 0.6f)
                )
            }
        }
    )
}
