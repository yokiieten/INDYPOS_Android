package com.indybrain.indypos_Android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.User
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeUser()
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
                        shopImageUrl = user?.shopImageUrl,
                        statistics = buildStatistics(user)
                    )
                }
            }
        }
    }
    
    private fun buildStatistics(user: User?): HomeStatistics {
        // Placeholder logic until dashboard API is connected
        val todaysOrders = user?.orderCount ?: 0
        val mockAmount = if (todaysOrders == 0) 0.0 else todaysOrders * 250.0
        return HomeStatistics(
            todaysSales = mockAmount,
            ordersToday = todaysOrders,
            topProductName = "เทส BBสูตร",
            topProductQuantity = if (todaysOrders == 0) 0 else todaysOrders,
            topProductAmount = if (todaysOrders == 0) 0.0 else mockAmount
        )
    }
}

