package com.indybrain.indypos_Android.presentation.addonmanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

/**
 * AddOn Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOnManagementScreen(
    onBackClick: () -> Unit = {},
    onAddAddonClick: () -> Unit = {},
    onEditAddonClick: (String) -> Unit = {},
    viewModel: AddOnManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedAddon by remember { mutableStateOf<AddonEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var addonToDelete by remember { mutableStateOf<AddonEntity?>(null) }
    var showMultipleDeleteConfirmation by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    
    // Pull to refresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading)
    
    // Refresh addons when screen becomes visible (returns from AddEditAddonScreen)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAddons()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Load sync statistics when dialog opens
    LaunchedEffect(showSyncDialog) {
        if (showSyncDialog) {
            viewModel.loadSyncStatistics()
        }
    }
    
    // Update search when query changes
    LaunchedEffect(searchQuery) {
        viewModel.searchAddons(searchQuery)
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isSelectionMode) 
                            stringResource(id = R.string.addon_management_select_title)
                        else 
                            stringResource(id = R.string.addon_management_title),
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
                actions = {
                    // Sync button
                    // if (!uiState.isSelectionMode) {
                    //     IconButton(onClick = { showSyncDialog = true }) {
                    //         Icon(
                    //             imageVector = Icons.Filled.Sync,
                    //             contentDescription = "Sync",
                    //             tint = GreenComplete
                    //         )
                    //     }
                    // }
                    // Edit/Cancel button
                    TextButton(
                        onClick = { viewModel.toggleSelectionMode() }
                    ) {
                        Text(
                            text = if (uiState.isSelectionMode) 
                                stringResource(id = R.string.addon_group_management_cancel)
                            else 
                                stringResource(id = R.string.addon_group_management_edit),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = if (uiState.isSelectionMode) PrimaryText else PrimaryButton
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
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar - Hide in selection mode
                if (!uiState.isSelectionMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.addon_management_search_placeholder),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = PlaceholderText
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = PlaceholderText,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFE6F2FF),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                }
                
                // Addon List with Pull to Refresh
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshAddons() }
                ) {
                    // Calculate addons to show
                    val addonsToShow = uiState.filteredAddons ?: emptyList()
                    // Show loading only for initial load when data hasn't been loaded yet (addons is null)
                    // Avoid showing full-screen loading during actions like delete to prevent flicker
                    val shouldShowLoading = uiState.isLoading && uiState.addons == null
                    
                    when {
                        shouldShowLoading -> {
                            // Show loading indicator when loading or data hasn't been loaded yet
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = PrimaryButton)
                            }
                        }
                        addonsToShow.isEmpty() -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 600.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.addon_management_empty),
                                            style = FontUtils.mainFont(
                                                style = AppFontStyle.Regular,
                                                size = FontSize.Medium
                                            ),
                                            color = SecondaryText
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 8.dp,
                                    end = 16.dp,
                                    bottom = if (uiState.isSelectionMode) 80.dp else 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(addonsToShow) { addon ->
                                    AddonItem(
                                        addon = addon,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isSelected = uiState.selectedAddonIds.contains(addon.id),
                                        onClick = { 
                                            if (uiState.isSelectionMode) {
                                                viewModel.toggleAddonSelection(addon.id)
                                            } else {
                                                selectedAddon = addon
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Action Bar - Show different UI based on selection mode
            if (uiState.isSelectionMode) {
                // Selection Mode - Show selection actions
                val addonsToShow = uiState.filteredAddons ?: emptyList()
                SelectionModeBottomBar(
                    selectedCount = uiState.selectedAddonIds.size,
                    totalCount = addonsToShow.size,
                    onSelectAll = { viewModel.selectAllAddons() },
                    onDeselectAll = { viewModel.deselectAllAddons() },
                    onDelete = {
                        if (uiState.selectedAddonIds.isNotEmpty()) {
                            showMultipleDeleteConfirmation = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // Normal Mode - Show Add Addon Button
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddAddonClick),
                    color = PrimaryButton
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.addon_management_add),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Bold,
                                size = FontSize.Medium
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
        
        // Addon Action Sheet
        selectedAddon?.let { addon ->
            AddonActionSheet(
                addon = addon,
                onDismiss = { selectedAddon = null },
                onToggleStatus = {
                    viewModel.toggleAddonStatus(addon.id, addon.isActive)
                    selectedAddon = null
                },
                onEdit = {
                    onEditAddonClick(addon.id)
                    selectedAddon = null
                },
                onDelete = {
                    addonToDelete = selectedAddon
                    showDeleteConfirmation = true
                    selectedAddon = null
                }
            )
        }
        
        // Delete Confirmation Dialog (Single)
        if (showDeleteConfirmation && addonToDelete != null) {
            DeleteConfirmationDialog(
                addonName = addonToDelete!!.name,
                onConfirm = {
                    addonToDelete?.id?.let { addonId ->
                        viewModel.deleteAddon(addonId)
                    }
                    showDeleteConfirmation = false
                    addonToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmation = false
                    addonToDelete = null
                }
            )
        }
        
        // Multiple Delete Confirmation Dialog
        if (showMultipleDeleteConfirmation) {
            MultipleDeleteConfirmationDialog(
                selectedCount = uiState.selectedAddonIds.size,
                onConfirm = {
                    viewModel.deleteSelectedAddons()
                    showMultipleDeleteConfirmation = false
                },
                onDismiss = {
                    showMultipleDeleteConfirmation = false
                }
            )
        }
        
        // Sync Status Dialog
        if (showSyncDialog) {
            SyncStatusDialog(
                statistics = uiState.syncStatistics,
                onDismiss = { showSyncDialog = false },
                onSyncNow = {
                    viewModel.syncAddons()
                    showSyncDialog = false
                }
            )
        }
        
        // Toggle Success Dialog
        uiState.toggleSuccessMessage?.let { message ->
            ToggleSuccessDialog(
                message = message,
                onDismiss = {
                    viewModel.clearToggleSuccessMessage()
                }
            )
        }
        
        // Delete Success Dialog
        uiState.deleteSuccessMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearDeleteSuccessMessage() },
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
                        text = message,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearDeleteSuccessMessage() }) {
                        Text(stringResource(id = R.string.dialog_button_ok), color = PrimaryButton)
                    }
                }
            )
        }
        
        // Sync Success Dialog
        uiState.syncSuccessMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSyncSuccess() },
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
                        text = message,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissSyncSuccess() }) {
                        Text(stringResource(id = R.string.dialog_button_ok), color = PrimaryButton)
                    }
                }
            )
        }
        
        // Error Message Dialog
        uiState.errorMessage?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
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
                        Text(stringResource(id = R.string.dialog_button_ok), color = PrimaryButton)
                    }
                }
            )
        }
    }
}

/**
 * Addon Item Row
 */
@Composable
private fun AddonItem(
    addon: AddonEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelectionMode && isSelected) Color(0xFFF5F5F5) else Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (in selection mode)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryButton,
                        uncheckedColor = SecondaryText
                    )
                )
            }
            
            // Addon Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Addon Name
                Text(
                    text = addon.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Price
                Text(
                    text = formatCurrency(addon.price),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText
                )
            }
            
            // Active Status Badge (right side)
            if (!isSelectionMode) {
                Surface(
                    color = if (addon.isActive) GreenComplete else RedFailure,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (addon.isActive) 
                            stringResource(id = R.string.product_status_active) 
                        else 
                            stringResource(id = R.string.product_status_inactive),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Selection Mode Bottom Bar
 */
@Composable
private fun SelectionModeBottomBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Select All / Deselect All Button
            TextButton(
                onClick = if (selectedCount == totalCount) onDeselectAll else onSelectAll
            ) {
                Text(
                    text = if (selectedCount == totalCount)
                        stringResource(id = R.string.category_management_deselect_all)
                    else
                        stringResource(id = R.string.category_management_select_all),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryButton
                )
            }
            
            // Selected Count
            Text(
                text = stringResource(id = R.string.addon_management_select_items, selectedCount),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            // Delete Button
            TextButton(
                onClick = onDelete,
                enabled = selectedCount > 0
            ) {
                Text(
                    text = stringResource(id = R.string.addon_group_management_action_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = if (selectedCount > 0) RedFailure else SecondaryText
                )
            }
        }
    }
}

/**
 * Addon Action Sheet
 */
@Composable
private fun AddonActionSheet(
    addon: AddonEntity,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.addon_management_action_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(id = R.string.addon_management_action_subtitle),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Toggle Status
                    TextButton(
                        onClick = onToggleStatus,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (addon.isActive) 
                                stringResource(id = R.string.addon_group_management_action_deactivate) 
                            else 
                                stringResource(id = R.string.addon_group_management_action_activate),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                    
                    // Edit
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.addon_group_management_action_edit),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                    
                    // Delete
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.addon_group_management_action_delete),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = RedFailure
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cancel Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss),
                        color = PrimaryButton
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.addon_group_management_cancel),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Bold,
                                    size = FontSize.Medium
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Delete Confirmation Dialog
 */
@Composable
private fun DeleteConfirmationDialog(
    addonName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.product_management_confirm_delete_title),
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
                    text = stringResource(id = R.string.addon_management_delete_confirm_single, addonName),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.addon_management_delete_warning),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.addon_group_management_action_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = RedFailure
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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

/**
 * Multiple Delete Confirmation Dialog
 */
@Composable
private fun MultipleDeleteConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.product_management_confirm_delete_title),
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
                    text = stringResource(id = R.string.addon_management_delete_confirm_multiple, selectedCount),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.addon_management_delete_warning),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(id = R.string.addon_group_management_action_delete),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = RedFailure
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.addon_group_management_cancel),
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
 * Sync Status Dialog
 */
@Composable
private fun SyncStatusDialog(
    statistics: com.indybrain.indypos_Android.domain.repository.AddonSyncStatistics?,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.addon_management_sync_status_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Column {
                if (statistics != null) {
                    Text(
                        text = stringResource(id = R.string.addon_management_sync_total, statistics.total),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.addon_management_sync_synced, statistics.synced),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.addon_management_sync_pending, statistics.unsynced),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.addon_management_sync_deleted, statistics.deleted),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryButton
                    )
                }
            }
        },
        confirmButton = {
            if (statistics != null && statistics.unsynced > 0) {
                TextButton(onClick = onSyncNow) {
                    Text(
                        text = stringResource(id = R.string.addon_management_sync_now),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.dialog_button_ok),
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

/**
 * Toggle Success Dialog
 */
@Composable
private fun ToggleSuccessDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                text = message,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.dialog_button_ok),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryButton
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Format currency
 */
private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

