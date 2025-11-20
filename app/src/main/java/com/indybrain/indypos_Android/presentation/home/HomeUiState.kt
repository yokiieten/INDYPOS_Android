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
    val statistics: HomeStatistics = HomeStatistics(),
    val shortcuts: List<HomeShortcut> = HomeShortcut.defaults(),
    val errorMessage: String? = null
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
        fun defaults(): List<HomeShortcut> = listOf(
            HomeShortcut(
                id = "start_order",
                title = "เริ่มออเดอร์",
                subtitle = "เพิ่มสินค้าเข้าบิล",
                icon = Icons.Outlined.Add,
                iconBackground = Color(0xFFEDF5FE)
            ),
            HomeShortcut(
                id = "orders",
                title = "ออเดอร์",
                subtitle = "ติดตามทุกสถานะ",
                icon = Icons.Outlined.ShoppingCart,
                iconBackground = Color(0xFFFFF3E5)
            ),
            HomeShortcut(
                id = "scan",
                title = "สแกน",
                subtitle = "บาร์โค้ดสินค้า",
                icon = Icons.Outlined.QrCodeScanner,
                iconBackground = Color(0xFFEFF9F0)
            ),
            HomeShortcut(
                id = "products",
                title = "สินค้า",
                subtitle = "จัดการสต็อก",
                icon = Icons.Outlined.ViewList,
                iconBackground = Color(0xFFFAF0F2)
            ),
            HomeShortcut(
                id = "dashboard",
                title = "กราฟ",
                subtitle = "ยอดขายภาพรวม",
                icon = Icons.Outlined.Analytics,
                iconBackground = Color(0xFFE9F3FF)
            ),
            HomeShortcut(
                id = "shop_profile",
                title = "หน้าร้าน",
                subtitle = "แก้ไขข้อมูล",
                icon = Icons.Outlined.Storefront,
                iconBackground = Color(0xFFF2F2F2)
            )
        )
    }
}

