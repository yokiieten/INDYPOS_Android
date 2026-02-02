package com.indybrain.indypos_Android.presentation.orderdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val authRepository: AuthRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()
    
    init {
        observeUserPermissions()
    }
    
    private fun observeUserPermissions() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                val canCancelOrder = user?.permissions?.contains("order.cancel") ?: false
                _uiState.update { it.copy(canCancelOrder = canCancelOrder) }
            }
        }
    }
    
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val order = orderRepository.getOrderById(orderId)
                var orderItems = orderRepository.getOrderItems(orderId)
                
                // If order exists but items are empty, try to refresh from API
                if (order != null && orderItems.isEmpty() && networkConnectivityChecker.isConnected()) {
                    try {
                        // Refresh orders list from API to get items
                        orderRepository.refreshOrdersList()
                        // Reload items after refresh
                        orderItems = orderRepository.getOrderItems(orderId)
                    } catch (e: Exception) {
                        // Silently fail - use empty items list
                    }
                }
                
                if (order != null) {
                    _uiState.update { 
                        it.copy(
                            order = order,
                            orderItems = orderItems,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "ไม่พบออเดอร์"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                    )
                }
            }
        }
    }
    
    fun cancelOrder(orderId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, errorMessage = null) }
            
            try {
                val result = orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED.code)
                
                result.onSuccess {
                    _uiState.update { it.copy(isCancelling = false) }
                    onSuccess()
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isCancelling = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการยกเลิกออเดอร์"
                        )
                    }
                    onError(error.message ?: "เกิดข้อผิดพลาดในการยกเลิกออเดอร์")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isCancelling = false,
                        errorMessage = e.message ?: "เกิดข้อผิดพลาดในการยกเลิกออเดอร์"
                    )
                }
                onError(e.message ?: "เกิดข้อผิดพลาดในการยกเลิกออเดอร์")
            }
        }
    }
}


