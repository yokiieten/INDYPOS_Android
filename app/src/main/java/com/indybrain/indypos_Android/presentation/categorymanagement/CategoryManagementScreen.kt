package com.indybrain.indypos_Android.presentation.categorymanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
 * Category Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    onBackClick: () -> Unit = {},
    onAddCategoryClick: () -> Unit = {},
    onEditCategoryClick: (String) -> Unit = {},
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<com.indybrain.indypos_Android.data.local.entity.CategoryEntity?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<com.indybrain.indypos_Android.data.local.entity.CategoryEntity?>(null) }
    var showMultipleDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Pull to refresh state
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isLoading)
    
    // Calculate categories to show
    val categoriesToShow = if (uiState.searchQuery.isNotBlank()) {
        uiState.filteredCategories
    } else {
        uiState.categories
    }
    
    // Update search when query changes
    LaunchedEffect(searchQuery) {
        viewModel.searchCategories(searchQuery)
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) 
                            stringResource(id = R.string.category_management_select_category)
                        else 
                            stringResource(id = R.string.category_management_title),
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
                    // Edit/Cancel button
                    TextButton(
                        onClick = { viewModel.toggleEditMode() }
                    ) {
                        Text(
                            text = if (uiState.isEditMode) 
                                stringResource(id = R.string.category_management_action_cancel)
                            else 
                                stringResource(id = R.string.category_management_edit),
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
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.category_management_search_placeholder),
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
                
                // Category List with Pull to Refresh
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshCategories() }
                ) {
                    if (categoriesToShow.isEmpty() && !uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.category_management_empty),
                                style = FontUtils.mainFont(
                                    style = AppFontStyle.Regular,
                                    size = FontSize.Medium
                                ),
                                color = SecondaryText
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                                bottom = if (uiState.isEditMode) 80.dp else 80.dp // Space for bottom button
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categoriesToShow) { category ->
                                CategoryItem(
                                    category = category,
                                    isEditMode = uiState.isEditMode,
                                    isSelected = uiState.selectedCategoryIds.contains(category.id),
                                    onUseClick = { /* TODO: Handle use click */ },
                                    onClick = { 
                                        if (uiState.isEditMode) {
                                            viewModel.toggleCategorySelection(category.id)
                                        } else {
                                            selectedCategory = category
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom Action Bar - Show different UI based on edit mode
            if (uiState.isEditMode) {
                // Edit Mode - Show selection actions
                EditModeBottomBar(
                    selectedCount = uiState.selectedCategoryIds.size,
                    totalCount = categoriesToShow.size,
                    onSelectAll = { viewModel.selectAllCategories() },
                    onDelete = {
                        if (uiState.selectedCategoryIds.isNotEmpty()) {
                            showMultipleDeleteConfirmation = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // Normal Mode - Show Add Category Button
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onAddCategoryClick),
                    color = PrimaryButton
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.category_management_add_category),
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
        
        // Category Action Sheet
        selectedCategory?.let { category ->
            CategoryActionSheet(
                category = category,
                onDismiss = { selectedCategory = null },
                onDeactivate = {
                    viewModel.toggleCategoryStatus(category.id, category.isActive)
                    selectedCategory = null
                },
                onEdit = {
                    selectedCategory?.id?.let { categoryId ->
                        onEditCategoryClick(categoryId)
                    }
                    selectedCategory = null
                },
                onDelete = {
                    categoryToDelete = selectedCategory
                    showDeleteConfirmation = true
                    selectedCategory = null
                }
            )
        }
        
        // Toggle Status Success Dialog
        uiState.toggleSuccessMessage?.let { message ->
            ToggleStatusSuccessDialog(
                message = message,
                onOkClick = {
                    viewModel.dismissToggleSuccess()
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
        if (showDeleteConfirmation && categoryToDelete != null) {
            DeleteConfirmationDialog(
                categoryName = categoryToDelete!!.name,
                onConfirm = {
                    categoryToDelete?.id?.let { categoryId ->
                        viewModel.deleteCategory(categoryId)
                    }
                    showDeleteConfirmation = false
                    categoryToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmation = false
                    categoryToDelete = null
                }
            )
        }
        
        // Multiple Delete Confirmation Dialog
        if (showMultipleDeleteConfirmation) {
            MultipleDeleteConfirmationDialog(
                selectedCount = uiState.selectedCategoryIds.size,
                onConfirm = {
                    viewModel.deleteSelectedCategories()
                    showMultipleDeleteConfirmation = false
                },
                onDismiss = {
                    showMultipleDeleteConfirmation = false
                }
            )
        }
    }
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
 * Delete Confirmation Dialog (Single Category)
 */
@Composable
private fun DeleteConfirmationDialog(
    categoryName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.category_management_action_delete),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
        },
        text = {
            Text(
                text = "คุณต้องการลบหมวดหมู่ '$categoryName' ใช่หรือไม่?",
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
                    text = stringResource(id = R.string.category_management_action_cancel),
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
                text = stringResource(id = R.string.category_management_confirm_delete_title),
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
                    text = stringResource(id = R.string.category_management_confirm_delete_message),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.category_management_confirm_delete_warning),
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
                    text = stringResource(id = R.string.category_management_action_delete),
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
                    text = stringResource(id = R.string.category_management_action_cancel),
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
            // Select All Button
            TextButton(
                onClick = onSelectAll
            ) {
                Text(
                    text = stringResource(id = R.string.category_management_select_all),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    ),
                    color = PrimaryButton
                )
            }
            
            // Selected Count
            Text(
                text = stringResource(
                    id = R.string.category_management_select_items,
                    selectedCount
                ),
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
                    text = stringResource(id = R.string.category_management_action_delete),
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
 * Category Item Row
 */
@Composable
private fun CategoryItem(
    category: com.indybrain.indypos_Android.data.local.entity.CategoryEntity,
    isEditMode: Boolean,
    isSelected: Boolean,
    onUseClick: () -> Unit,
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
            // Radio Button (in edit mode)
            if (isEditMode) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PrimaryButton,
                        unselectedColor = SecondaryText
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Category Name
                Text(
                    text = category.name,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Product Count
                Text(
                    text = stringResource(
                        id = R.string.category_management_items,
                        category.productCount ?: 0
                    ),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = Color(0xFF87CEEB) // Light blue color
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Status Badge or Use Button
            if (category.isActive) {
                // Active - Show Use Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onUseClick),
                    color = GreenComplete
                ) {
                    Text(
                        text = stringResource(id = R.string.category_management_use),
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
                        text = stringResource(id = R.string.category_management_inactive),
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
 * Category Action Sheet
 */
@Composable
private fun CategoryActionSheet(
    category: com.indybrain.indypos_Android.data.local.entity.CategoryEntity,
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
                    text = stringResource(id = R.string.category_management_action_title),
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
                    text = stringResource(id = R.string.category_management_action_subtitle),
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
                            text = stringResource(
                                id = if (category.isActive) 
                                    R.string.category_management_action_deactivate 
                                else 
                                    R.string.category_management_action_activate
                            ),
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
                            text = stringResource(id = R.string.category_management_action_edit),
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
                            text = stringResource(id = R.string.category_management_action_delete),
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
                                text = stringResource(id = R.string.category_management_action_cancel),
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

