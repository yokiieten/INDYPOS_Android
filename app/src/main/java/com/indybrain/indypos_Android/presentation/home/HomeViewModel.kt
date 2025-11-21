package com.indybrain.indypos_Android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeUser()
        fetchOrders()
    }
    
    private fun observeUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        shopName = user?.shopName?.takeUnless { it.isBlank() }
                            ?: user?.firstName
                            ?: "INDYPOS",
                        shopDescription = user?.shopDescription.orEmpty(),
                        shopImageUrl = user?.shopImageUrl
                    )
                }
            }
        }
    }
    
    private fun fetchOrders() {
        // Refresh orders from API first
        viewModelScope.launch {
            orderRepository.refreshOrders()
        }
        
        // Observe orders from local database
        viewModelScope.launch {
            orderRepository.getOrders().collect { result ->
                result.onSuccess { orders ->
                    val statistics = buildStatistics(orders)
                    _uiState.update { current ->
                        current.copy(statistics = statistics)
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(errorMessage = error.message)
                    }
                }
            }
        }
    }
    
    private fun buildStatistics(orders: List<com.indybrain.indypos_Android.data.local.entity.OrderEntity>): HomeStatistics {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        
        // Filter today's orders
        val todayOrders = orders.filter { order ->
            val orderCalendar = Calendar.getInstance().apply {
                time = order.orderDate
            }
            orderCalendar.get(Calendar.DAY_OF_YEAR) == today &&
            orderCalendar.get(Calendar.YEAR) == year
        }
        
        val todaysSales = todayOrders.sumOf { it.total }
        val ordersToday = todayOrders.size
        
        // Find top product
        val productMap = mutableMapOf<String, ProductStats>()
        todayOrders.forEach { order ->
            // Note: We'd need to fetch order items to get product details
            // For now, using order data as placeholder
        }
        
        val topProduct = productMap.values.maxByOrNull { it.quantity }
        
        return HomeStatistics(
            todaysSales = todaysSales,
            ordersToday = ordersToday,
            topProductName = topProduct?.name ?: "",
            topProductQuantity = topProduct?.quantity ?: 0,
            topProductAmount = topProduct?.amount ?: 0.0
        )
    }
    
    private data class ProductStats(
        val name: String,
        var quantity: Int = 0,
        var amount: Double = 0.0
    )
}

