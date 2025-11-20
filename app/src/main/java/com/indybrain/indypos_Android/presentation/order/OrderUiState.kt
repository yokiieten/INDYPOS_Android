package com.indybrain.indypos_Android.presentation.order

import com.indybrain.indypos_Android.domain.model.Order

/**
 * UI data holder for the order screen
 */
data class OrderUiState(
    val isLoading: Boolean = false,
    val completedOrders: List<Order> = emptyList(),
    val cancelledOrders: List<Order> = emptyList(),
    val selectedTab: OrderTab = OrderTab.COMPLETED,
    val filterOption: OrderFilter = OrderFilter.ALL,
    val sortOption: OrderSort = OrderSort.LATEST,
    val errorMessage: String? = null
)

enum class OrderTab {
    COMPLETED,      // เสร็จสิ้น
    CANCELLED       // ยกเลิก/ล้มเหลว
}

enum class OrderFilter {
    ALL,            // ทั้งหมด
    TODAY,          // วันนี้
    THIS_WEEK,      // สัปดาห์นี้
    THIS_MONTH,     // เดือนนี้
    SELECT_DATE,    // เลือกวันที่
}

enum class OrderSort {
    LATEST,         // ล่าสุด
    OLDEST,         // เก่าสุด
    HIGHEST_AMOUNT  // ยอดสูงสุด
}

