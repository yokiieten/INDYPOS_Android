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
import android.net.Uri

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeUser()
        observeOrders()
    }
    
    /**
     * Refresh data when screen appears (like viewWillAppear in iOS)
     * Uses the new orders list endpoint (non-paginated)
     */
    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            orderRepository.refreshOrdersList()
        }
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
    
    private fun observeOrders() {
        // Observe orders from local database
        viewModelScope.launch {
            orderRepository.getOrders().collect { result ->
                result.onSuccess { orders ->
                    val statistics = buildStatistics(orders)
                    _uiState.update { current ->
                        current.copy(
                            statistics = statistics,
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = error.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }
    
    private fun buildStatistics(orders: List<com.indybrain.indypos_Android.data.local.entity.OrderEntity>): HomeStatistics {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        
        // Filter today's orders (exclude cancelled orders - statusRaw == 5)
        val todayOrders = orders.filter { order ->
            val orderCalendar = Calendar.getInstance().apply {
                time = order.orderDate
            }
            orderCalendar.get(Calendar.DAY_OF_YEAR) == today &&
            orderCalendar.get(Calendar.YEAR) == year &&
            order.statusRaw != 5 // Exclude cancelled orders
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
    
    fun showImagePicker() {
        _uiState.update { it.copy(showImagePickerDialog = true) }
    }
    
    fun dismissImagePicker() {
        _uiState.update { it.copy(showImagePickerDialog = false) }
    }
    
    fun showEditStoreNameDialog() {
        _uiState.update { it.copy(showEditStoreNameDialog = true) }
    }
    
    fun dismissEditStoreNameDialog() {
        _uiState.update { it.copy(showEditStoreNameDialog = false) }
    }
    
    fun showEditDescriptionDialog() {
        _uiState.update { it.copy(showEditDescriptionDialog = true) }
    }
    
    fun dismissEditDescriptionDialog() {
        _uiState.update { it.copy(showEditDescriptionDialog = false) }
    }
    
    fun updateStoreName(newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            authRepository.updateShopName(newName)
                .onSuccess { updatedUser ->
                    // Update UI state with new shop name
                    // The observeUser() flow will also pick up the change automatically
                    // but we update immediately for better UX
                    _uiState.update { 
                        it.copy(
                            shopName = updatedUser.shopName ?: newName,
                            shopDescription = updatedUser.shopDescription ?: it.shopDescription,
                            isLoading = false,
                            errorMessage = null,
                            successMessage = "home_store_name_updated"
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        ) 
                    }
                }
        }
    }
    
    fun updateShopDescription(newDescription: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            authRepository.updateShopDescription(newDescription)
                .onSuccess { updatedUser ->
                    // Update UI state with new description
                    // The observeUser() flow will also pick up the change automatically
                    // but we update immediately for better UX
                    _uiState.update { 
                        it.copy(
                            shopDescription = updatedUser.shopDescription.orEmpty(),
                            shopName = updatedUser.shopName ?: it.shopName,
                            isLoading = false,
                            errorMessage = null,
                            successMessage = "home_store_description_updated"
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        ) 
                    }
                }
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    fun updateShopImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.updateShopImage(imageUri)
            result
                .onSuccess { updatedUser ->
                    _uiState.update {
                        it.copy(
                            shopImageUrl = updatedUser.shopImageUrl,
                            isLoading = false,
                            errorMessage = null,
                            successMessage = "home_store_image_updated"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "ไม่สามารถอัปเดตรูปหน้าร้านได้"
                        )
                    }
                }
        }
    }
    
    private data class ProductStats(
        val name: String,
        var quantity: Int = 0,
        var amount: Double = 0.0
    )
}

