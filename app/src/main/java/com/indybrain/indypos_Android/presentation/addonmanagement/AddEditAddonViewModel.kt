package com.indybrain.indypos_Android.presentation.addonmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Addon screen
 */
@HiltViewModel
class AddEditAddonViewModel @Inject constructor(
    private val addonRepository: AddonRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEditAddonUiState())
    val uiState: StateFlow<AddEditAddonUiState> = _uiState.asStateFlow()
    
    private var addonId: String? = null
    
    /**
     * Load addon data for editing
     */
    fun loadAddon(addonId: String) {
        this.addonId = addonId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val addon = addonRepository.getAddonById(addonId)
            _uiState.update { 
                it.copy(
                    addonName = addon?.name ?: "",
                    addonPrice = formatPrice(addon?.price ?: 0.0),
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Update addon name
     */
    fun updateAddonName(name: String) {
        _uiState.update { it.copy(addonName = name, errorMessage = null) }
    }
    
    /**
     * Update addon price
     */
    fun updateAddonPrice(price: String) {
        _uiState.update { it.copy(addonPrice = price, errorMessage = null) }
    }
    
    /**
     * Dismiss success dialog and reset success state
     */
    fun dismissSuccessDialog() {
        _uiState.update { it.copy(isSuccess = false, isOfflineSuccess = false) }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Format price: if decimal is .00 show integer, otherwise show 2 decimals
     */
    private fun formatPrice(price: Double): String {
        val roundedPrice = price.toInt().toDouble()
        return if (kotlin.math.abs(price - roundedPrice) < 0.001) {
            // Decimal is .00 - show as integer
            String.format("%.0f", roundedPrice)
        } else {
            // Show 2 decimals
            String.format("%.2f", price)
        }
    }
    
    /**
     * Save addon (add new or update existing)
     */
    fun saveAddon(onSuccess: () -> Unit) {
        val name = _uiState.value.addonName.trim()
        val priceString = _uiState.value.addonPrice.trim()
        
        // Validation
        if (name.isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณากรอกชื่อ Addon")
            }
            return
        }
        
        // Parse price - if empty, use 0
        val price = if (priceString.isEmpty()) {
            0.0
        } else {
            try {
                val parsedPrice = priceString.toDouble()
                if (parsedPrice < 0) {
                    _uiState.update { 
                        it.copy(errorMessage = "ราคาต้องเป็นตัวเลขที่มากกว่าหรือเท่ากับ 0")
                    }
                    return
                }
                parsedPrice
            } catch (e: NumberFormatException) {
                _uiState.update { 
                    it.copy(errorMessage = "กรุณากรอกราคาเป็นตัวเลขที่ถูกต้อง")
                }
                return
            }
        }
        
        // Save addon
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = addonId?.let { id ->
                // Update existing addon
                addonRepository.updateAddon(id, name, price)
            } ?: run {
                // Create new addon
                addonRepository.createAddon(name, price)
            }
            
            result.onSuccess { addonEntity ->
                // Check if addon was created/updated offline (not synced)
                val isOffline = !addonEntity.isSynced || !addonEntity.isFromServer
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isOfflineSuccess = isOffline
                    )
                }
                // Don't call onSuccess() here - let the dialog handle navigation
            }.onFailure { error ->
                val errorMessage = error.message ?: "เกิดข้อผิดพลาดในการบันทึก"
                // Handle special error codes
                val finalErrorMessage = when {
                    errorMessage.contains("free_plan_limit_exceeded", ignoreCase = true) -> {
                        "free_plan_limit_exceeded"
                    }
                    errorMessage.contains("ชื่อ Addon นี้มีอยู่แล้ว") -> {
                        "ชื่อ Addon นี้มีอยู่แล้ว"
                    }
                    else -> errorMessage
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = finalErrorMessage
                    )
                }
            }
        }
    }
}

