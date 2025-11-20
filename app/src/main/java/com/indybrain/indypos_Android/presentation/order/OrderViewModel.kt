package com.indybrain.indypos_Android.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.Order
import com.indybrain.indypos_Android.domain.model.OrderStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
    
    init {
        loadMockOrders()
    }
    
    private fun loadMockOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Mock data for completed orders
            val completedOrders = listOf(
                Order(
                    id = "1",
                    orderId = "ORD20251120001386",
                    createdAt = createDate(2025, 11, 20, 16, 28),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 1821.00
                ),
                Order(
                    id = "2",
                    orderId = "ORD20251115001280",
                    createdAt = createDate(2025, 11, 15, 14, 42),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 120.00
                ),
                Order(
                    id = "3",
                    orderId = "ORD20251115001278",
                    createdAt = createDate(2025, 11, 15, 14, 33),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 727.00
                ),
                Order(
                    id = "4",
                    orderId = "ORD20251115001276",
                    createdAt = createDate(2025, 11, 15, 14, 33),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 1214.00
                ),
                Order(
                    id = "5",
                    orderId = "ORD20251115001275",
                    createdAt = createDate(2025, 11, 15, 14, 32),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 607.00
                ),
                Order(
                    id = "7",
                    orderId = "ORD20251114001250",
                    createdAt = createDate(2025, 11, 14, 10, 15),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 2500.50
                ),
                Order(
                    id = "8",
                    orderId = "ORD20251114001245",
                    createdAt = createDate(2025, 11, 14, 9, 30),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 890.25
                ),
                Order(
                    id = "9",
                    orderId = "ORD20251113001230",
                    createdAt = createDate(2025, 11, 13, 18, 45),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 3456.75
                ),
                Order(
                    id = "10",
                    orderId = "ORD20251113001225",
                    createdAt = createDate(2025, 11, 13, 15, 20),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 567.00
                ),
                Order(
                    id = "11",
                    orderId = "ORD20251112001210",
                    createdAt = createDate(2025, 11, 12, 12, 10),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 1234.56
                ),
                Order(
                    id = "12",
                    orderId = "ORD20251112001205",
                    createdAt = createDate(2025, 11, 12, 11, 5),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 789.00
                ),
                Order(
                    id = "13",
                    orderId = "ORD20251111001190",
                    createdAt = createDate(2025, 11, 11, 16, 30),
                    status = OrderStatus.COMPLETED,
                    totalAmount = 2345.67
                )
            )
            
            // Mock data for cancelled orders
            val cancelledOrders = listOf(
                Order(
                    id = "6",
                    orderId = "ORD20251120001386",
                    createdAt = createDate(2025, 11, 20, 16, 28),
                    cancelledAt = createDate(2025, 11, 20, 16, 58),
                    status = OrderStatus.CANCELLED,
                    totalAmount = 1821.00
                ),
                Order(
                    id = "14",
                    orderId = "ORD20251119001180",
                    createdAt = createDate(2025, 11, 19, 14, 20),
                    cancelledAt = createDate(2025, 11, 19, 14, 35),
                    status = OrderStatus.CANCELLED,
                    totalAmount = 450.00
                ),
                Order(
                    id = "15",
                    orderId = "ORD20251118001170",
                    createdAt = createDate(2025, 11, 18, 10, 15),
                    cancelledAt = createDate(2025, 11, 18, 10, 25),
                    status = OrderStatus.CANCELLED,
                    totalAmount = 1230.50
                ),
                Order(
                    id = "16",
                    orderId = "ORD20251117001160",
                    createdAt = createDate(2025, 11, 17, 13, 40),
                    cancelledAt = createDate(2025, 11, 17, 13, 55),
                    status = OrderStatus.FAILED,
                    totalAmount = 678.90
                ),
                Order(
                    id = "17",
                    orderId = "ORD20251116001150",
                    createdAt = createDate(2025, 11, 16, 9, 10),
                    cancelledAt = createDate(2025, 11, 16, 9, 20),
                    status = OrderStatus.CANCELLED,
                    totalAmount = 234.00
                )
            )
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    completedOrders = completedOrders,
                    cancelledOrders = cancelledOrders
                )
            }
        }
    }
    
    fun selectTab(tab: OrderTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
    
    fun selectFilter(filter: OrderFilter) {
        _uiState.update { it.copy(filterOption = filter) }
    }
    
    fun selectSort(sort: OrderSort) {
        _uiState.update { it.copy(sortOption = sort) }
    }
    
    fun refreshOrders() {
        loadMockOrders()
    }
    
    private fun createDate(year: Int, month: Int, day: Int, hour: Int, minute: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, hour, minute, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}

