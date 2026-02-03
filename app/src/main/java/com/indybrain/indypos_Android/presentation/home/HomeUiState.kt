package com.indybrain.indypos_Android.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * UI data holder for the home screen
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val shopName: String = "",
    val shopDescription: String = "",
    val shopImageUrl: String? = null,
    val canViewReports: Boolean = false,
    val statistics: HomeStatistics = HomeStatistics(),
    val shortcuts: List<HomeShortcut> = HomeShortcut.defaults(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showImagePickerDialog: Boolean = false,
    val showEditStoreNameDialog: Boolean = false,
    val showEditDescriptionDialog: Boolean = false
)

data class HomeStatistics(
    val todaysSales: Double = 0.0,
    val ordersToday: Int = 0,
    val topProductName: String = "",
    val topProductQuantity: Int = 0,
    val topProductAmount: Double = 0.0
)

data class HomeShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBackground: Color
) {
    companion object {
        fun defaults(): List<HomeShortcut> = emptyList() // Will be created in Composable with string resources
    }
}

