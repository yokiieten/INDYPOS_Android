package com.indybrain.indypos_Android.presentation.addongroupmanagement

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Addon Group Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddonGroupManagementScreen(
    onBackClick: () -> Unit = {},
    onAddAddonGroupClick: () -> Unit = {},
    onEditAddonGroupClick: (String) -> Unit = {},
    viewModel: AddonGroupManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedAddonGroup by remember { mutableStateOf<com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var addonGroupToDelete by remember { mutableStateOf<com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity?>(null) }
    var showMultipleDeleteConfirmation by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    
    // Load sync statistics when dialog opens
    LaunchedEffect(showSyncDialog) {
        if (showSyncDialog) {
            viewModel.loadSyncStatistics()
        }
    }
    
    // Pull to refresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Refresh addon groups when screen becomes visible (returns from AddEditAddonGroupScreen)
    // Reset search, reload API, and scroll to top so new/edited addon group appears in list
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAddonGroupsAndClearSearch()
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Load more when scrolling near end - use snapshotFlow to react to scroll changes
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }.collect { nearEnd ->
            if (nearEnd) {
                viewModel.loadMoreAddonGroups()
            }
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) 
                            stringResource(id = R.string.addon_group_management_select_title)
                        else 
                            stringResource(id = R.string.addon_group_management_title),
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
                    // if (!uiState.isEditMode) {
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
                        onClick = { viewModel.toggleEditMode() }
                    ) {
                        Text(
                            text = if (uiState.isEditMode) 
                                stringResource(id = R.string.addon_group_management_cancel)
                            else 
                                stringResource(id = R.string.addon_group_management_edit),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Medium
                            ),
                            color = if (uiState.isEditMode) PrimaryText else GreenComplete
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
                // Search Bar - Hide in edit mode
                if (!uiState.isEditMode) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.searchAddonGroups(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.addon_group_management_search_placeholder),
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
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                }
                
                // Addon Group List with Pull to Refresh
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshAddonGroups() }
                ) {
                    // Display list (filteredAddonGroups is the accumulated paginated list)
                    val groupsToShow = uiState.filteredAddonGroups ?: uiState.addonGroups ?: emptyList()
                    // Show loading only for initial load when data hasn't been loaded yet (addonGroups is null)
                    // Avoid showing full-screen loading during actions like delete to prevent flicker
                    val shouldShowLoading = uiState.isLoading && uiState.addonGroups == null
                    
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
                        groupsToShow.isEmpty() -> {
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
                                            text = stringResource(id = R.string.addon_group_management_empty),
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
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    top = 8.dp,
                                    end = 16.dp,
                                    bottom = if (uiState.isEditMode) 80.dp else 80.dp // Space for bottom button
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(groupsToShow) { addonGroup ->
                                    val addonCount = uiState.addonCounts[addonGroup.id]
                                    AddonGroupItem(
                                        addonGroup = addonGroup,
                                        addonCount = addonCount,
                                        isEditMode = uiState.isEditMode,
                                        isSelected = uiState.selectedAddonGroupIds.contains(addonGroup.id),
                                        onClick = { 
                                            if (uiState.isEditMode) {
                                                viewModel.toggleAddonGroupSelection(addonGroup.id)
                                            } else {
                                                selectedAddonGroup = addonGroup
                                            }
                                        }
                                    )
                                }
                                // Load more indicator
                                if (uiState.isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = PrimaryButton
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Action Bar - Show different UI based on edit mode
            if (uiState.isEditMode) {
                // Edit Mode - Show selection actions
                val groupsToShow = uiState.filteredAddonGroups ?: uiState.addonGroups ?: emptyList()
                val allSelected = uiState.selectedAddonGroupIds.size == groupsToShow.size && groupsToShow.isNotEmpty()
                EditModeBottomBar(
                    selectedCount = uiState.selectedAddonGroupIds.size,
                    totalCount = groupsToShow.size,
                    allSelected = allSelected,
                    onSelectAll = { 
                        if (allSelected) {
                            viewModel.deselectAllAddonGroups()
                        } else {
                            // Select only visible/filtered addon groups
                            val visibleAddonGroupIds = groupsToShow.map { it.id }.toSet()
                            viewModel.selectAddonGroups(visibleAddonGroupIds)
                        }
                    },
                    onDelete = {
                        if (uiState.selectedAddonGroupIds.isNotEmpty()) {
                            showMultipleDeleteConfirmation = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // Normal Mode - Show Add Addon Group Button
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onAddAddonGroupClick),
                    color = PrimaryButton
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.addon_group_management_add),
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
        }
        
        // Addon Group Action Sheet
        selectedAddonGroup?.let { addonGroup ->
            AddonGroupActionSheet(
                addonGroup = addonGroup,
                onDismiss = { selectedAddonGroup = null },
                onDeactivate = {
                    viewModel.toggleAddonGroupStatus(addonGroup.id, addonGroup.isActive)
                    selectedAddonGroup = null
                },
                onEdit = {
                    selectedAddonGroup?.id?.let { addonGroupId ->
                        onEditAddonGroupClick(addonGroupId)
                    }
                    selectedAddonGroup = null
                },
                onDelete = {
                    addonGroupToDelete = selectedAddonGroup
                    showDeleteConfirmation = true
                    selectedAddonGroup = null
                }
            )
        }
        
        // Toggle Status Success Dialog
        uiState.toggleSuccessMessage?.let { message ->
            ToggleStatusSuccessDialog(
                message = message,
                onOkClick = {
                    viewModel.dismissToggleSuccess()
                    viewModel.refreshAddonGroups()
                }
            )
        }
        
        // Delete Success Dialog
        uiState.deleteSuccessMessage?.let { message ->
            DeleteSuccessDialog(
                message = message,
                onOkClick = {
                    viewModel.dismissDeleteSuccess()
                }
            )
        }
        
        // Delete Confirmation Dialog (Single)
        if (showDeleteConfirmation && addonGroupToDelete != null) {
            DeleteConfirmationDialog(
                addonGroupName = addonGroupToDelete!!.name,
                onConfirm = {
                    addonGroupToDelete?.id?.let { addonGroupId ->
                        viewModel.deleteAddonGroup(addonGroupId)
                    }
                    showDeleteConfirmation = false
                    addonGroupToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmation = false
                    addonGroupToDelete = null
                }
            )
        }
        
        // Multiple Delete Confirmation Dialog
        if (showMultipleDeleteConfirmation) {
            MultipleDeleteConfirmationDialog(
                selectedCount = uiState.selectedAddonGroupIds.size,
                onConfirm = {
                    viewModel.deleteSelectedAddonGroups()
                    showMultipleDeleteConfirmation = false
                },
                onDismiss = {
                    showMultipleDeleteConfirmation = false
                }
            )
        }
        
        // Sync Status Dialog
        if (showSyncDialog) {
            AddonGroupSyncStatusDialog(
                statistics = uiState.syncStatistics,
                onDismiss = { showSyncDialog = false },
                onSyncNow = {
                    viewModel.syncAddonGroups()
                    showSyncDialog = false
                }
            )
        }
        
        // Sync Success Dialog
        uiState.syncSuccessMessage?.let { message ->
            SyncSuccessDialog(
                message = message,
                onDismiss = {
                    viewModel.dismissSyncSuccess()
                }
            )
        }
        
        // Error Dialog - Show API errors as popup
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
    }
}

/**
 * Addon Group Sync Status Dialog
 */
@Composable
private fun AddonGroupSyncStatusDialog(
    statistics: com.indybrain.indypos_Android.domain.repository.AddonGroupSyncStatistics?,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.addon_group_management_sync_status_title),
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
                        text = stringResource(id = R.string.addon_group_management_sync_total, statistics.total),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.addon_group_management_sync_synced, statistics.synced),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.addon_group_management_sync_pending, statistics.unsynced),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.addon_group_management_sync_deleted, statistics.deleted),
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
            if (statistics != null) {
                TextButton(onClick = onSyncNow) {
                    Text(
                        text = stringResource(id = R.string.addon_group_management_sync_now),
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
 * Sync Success Dialog
 */
@Composable
private fun SyncSuccessDialog(
    message: String,
    onDismiss: () -> Unit
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
                text = message,
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

/**
 * Toggle Status Success Dialog
 */
@Composable
private fun ToggleStatusSuccessDialog(
    message: String,
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
                text = message,
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

/**
 * Delete Success Dialog
 */
@Composable
private fun DeleteSuccessDialog(
    message: String,
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
                text = message,
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

/**
 * Delete Confirmation Dialog (Single Addon Group)
 */
@Composable
private fun DeleteConfirmationDialog(
    addonGroupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.addon_group_management_delete_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Text(
                text = stringResource(id = R.string.addon_group_management_delete_confirm_single, addonGroupName),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_button_ok),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = RedFailure
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
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
                    text = stringResource(id = R.string.addon_group_management_delete_confirm_multiple, selectedCount),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.addon_group_management_delete_warning),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
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
            TextButton(
                onClick = onDismiss
            ) {
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
 * Edit Mode Bottom Action Bar
 */
@Composable
private fun EditModeBottomBar(
    selectedCount: Int,
    totalCount: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
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
                onClick = onSelectAll
            ) {
                Text(
                    text = if (allSelected) 
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
                text = stringResource(id = R.string.addon_group_management_select_items, selectedCount),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PrimaryButton
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
 * Addon Group Item Row
 */
@Composable
private fun AddonGroupItem(
    addonGroup: com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity,
    addonCount: Int?,
    isEditMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isEditMode && isSelected) Color(0xFFF5F5F5) else Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (in edit mode)
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryButton,
                        uncheckedColor = SecondaryText
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Addon Group Name
                Text(
                    text = addonGroup.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Additional info (required/optional + selection mode)
                val requiredText = stringResource(
                    id = if (addonGroup.isRequired) {
                        R.string.addon_group_required
                    } else {
                        R.string.addon_group_optional
                    }
                )

                val selectionText = stringResource(
                    id = if (addonGroup.isSingleSelection) {
                        R.string.addon_group_single_selection
                    } else {
                        R.string.addon_group_multiple_selection
                    }
                )

                val baseInfoText = "$requiredText \u2022 $selectionText"

                val infoText = if (addonCount != null && addonCount > 0) {
                    val itemsText = stringResource(
                        id = R.string.addon_group_items_count,
                        addonCount
                    )
                    "$baseInfoText \u2022 $itemsText"
                } else {
                    baseInfoText
                }

                Text(
                    text = infoText,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = Color(0xFF87CEEB) // Light blue color
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Status Badge - Hide in edit mode
            if (!isEditMode) {
                if (addonGroup.isActive) {
                    // Active - Show Green Badge
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp)),
                        color = GreenComplete
                    ) {
                        Text(
                            text = stringResource(id = R.string.addon_group_management_status_active),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    // Inactive - Show Red Badge
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp)),
                        color = RedFailure
                    ) {
                        Text(
                            text = stringResource(id = R.string.addon_group_management_status_inactive),
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
}

/**
 * Addon Group Action Sheet
 */
@Composable
private fun AddonGroupActionSheet(
    addonGroup: com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity,
    onDismiss: () -> Unit,
    onDeactivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        // Action Sheet Content
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = stringResource(id = R.string.addon_group_management_action_title),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Large
                    ),
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = stringResource(id = R.string.addon_group_management_action_subtitle),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Toggle Status (Activate/Deactivate)
                    TextButton(
                        onClick = onDeactivate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (addonGroup.isActive) 
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
                            color = Color(0xFFE83808) // Red color
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
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

