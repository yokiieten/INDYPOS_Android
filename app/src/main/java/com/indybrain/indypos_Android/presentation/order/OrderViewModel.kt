package com.indybrain.indypos_Android.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.Order
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.repository.OrderRepository
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
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
    
    init {
        observeOrders()
        refreshOrders()
    }
    
    private fun observeOrders() {
        viewModelScope.launch {
            orderRepository.getOrders().collect { result ->
                result.onSuccess { entities ->
                    val orders = entities.map { entity ->
                        val status = OrderStatus.fromCode(entity.orderStatus)
                        Order(
                            id = entity.id,
                            orderId = entity.orderNumber,
                            createdAt = entity.orderDate,
                            cancelledAt = null,
                            status = status,
                            totalAmount = entity.total
                        )
                    }
                    
                    // แท็บ "เสร็จสิ้น" แสดงทุกสถานะที่ไม่ใช่ยกเลิก
                    val completedOrders = orders.filter { it.status != OrderStatus.CANCELLED }
                    // แท็บ "ยกเลิก" แสดงเฉพาะสถานะยกเลิก (code = 5)
                    val cancelledOrders = orders.filter { it.status == OrderStatus.CANCELLED }
                    
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            completedOrders = completedOrders,
                            cancelledOrders = cancelledOrders
                        )
                    }
                }.onFailure {
                    _uiState.update { current ->
                        current.copy(isLoading = false)
                    }
                }
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            orderRepository.refreshOrders()
        }
    }
    
    private fun createDate(year: Int, month: Int, day: Int, hour: Int, minute: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, hour, minute, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}

