package com.indybrain.indypos_Android.presentation.addonmanagement

import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import com.indybrain.indypos_Android.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Addon screen
 */
@HiltViewModel
class AddEditAddonViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val languageLocalDataSource: LanguageLocalDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Returns string in the user's selected language (respects language change in Settings) */
    private fun getLocalizedString(resId: Int): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId)
    }
    
    private val _uiState = MutableStateFlow(AddEditAddonUiState())
    val uiState: StateFlow<AddEditAddonUiState> = _uiState.asStateFlow()
    
    private var addonId: String? = null
    
    /**
     * Load addon data for editing
     */
    fun loadAddon(addonId: String) {
        this.addonId = addonId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            addonRepository.getAddonFromApi(addonId).fold(
                onSuccess = { addon ->
                    val formattedPrice = formatPriceForDisplay(addon.price.toString())
                    _uiState.update {
                        it.copy(
                            addonName = addon.name,
                            addonPrice = formattedPrice,
                            editSortOrder = addon.sortOrder ?: 1,
                            editIsActive = addon.isActive,
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            addonName = "",
                            addonPrice = "0",
                            errorMessage = e.message
                                ?: getLocalizedString(R.string.addon_management_error_loading)
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Update addon name
     */
    fun updateAddonName(name: String) {
        _uiState.update { it.copy(addonName = name, errorMessage = null) }
    }
    
    /**
     * Filter price input to only allow numbers and decimal point
     * No limit on decimal places while typing - user can type unlimited digits
     * Rounding to 2 decimal places happens when user finishes input (on unfocus or Done)
     * Example: User can type 5000.533555, rounding (5000.533555 -> 5000.53, 5000.535555 -> 5000.54) happens on unfocus
     */
    private fun filterPriceInput(input: String): String {
        if (input.isBlank()) return input
        
        // Allow only digits and one decimal point
        val filtered = input.filter { it.isDigit() || it == '.' }
        
        // If empty after filtering, return empty
        if (filtered.isEmpty()) return ""
        
        // Check for multiple decimal points - keep only the first one
        val parts = filtered.split('.')
        val result = if (parts.size > 2) {
            // Multiple decimal points - keep first part + first decimal point + second part
            parts[0] + "." + parts[1]
        } else {
            filtered
        }
        
        // No limit on decimal places while typing - allow unlimited digits
        // Rounding will happen in formatPriceOnUnfocus() when user finishes input
        return result
    }
    
    /**
     * Format price when user finishes input (on unfocus): Round to 2 decimal places
     * Example: 5000.533555 -> 5000.53, 5000.535555 -> 5000.54
     */
    private fun formatPriceOnUnfocus(input: String): String {
        if (input.isBlank()) return input
        
        try {
            // Use BigDecimal for precise rounding (round half up)
            val bigDecimal = BigDecimal(input.trim())
            val rounded = bigDecimal.setScale(2, RoundingMode.HALF_UP)
            // Format to 2 decimal places
            return String.format("%.2f", rounded.toDouble())
        } catch (e: Exception) {
            // If parsing fails, return as is
            return input
        }
    }
    
    /**
     * Format price for display: Hide .00, show 2 decimal places otherwise
     * Example: 5000.00 -> 5000, 5000.50 -> 5000.50
     */
    fun formatPriceForDisplay(price: String): String {
        if (price.isBlank()) return price
        
        val parsed = price.trim().toDoubleOrNull()
        return if (parsed != null) {
            // If decimal is .00, don't show decimals
            if (parsed % 1.0 == 0.0) {
                parsed.toInt().toString()
            } else {
                // Show 2 decimal places
                String.format("%.2f", parsed)
            }
        } else {
            price
        }
    }
    
    /**
     * Update addon price with input filtering
     */
    fun updateAddonPrice(price: String) {
        val filtered = filterPriceInput(price)
        _uiState.update { it.copy(addonPrice = filtered, errorMessage = null) }
    }
    
    /**
     * Format addon price when user finishes input
     * Rounds to 2 decimal places and applies display formatting (hides .00 in edit mode)
     */
    fun formatAddonPriceOnUnfocus() {
        val currentPrice = _uiState.value.addonPrice
        if (currentPrice.isBlank()) return
        
        // First round to 2 decimal places
        val rounded = formatPriceOnUnfocus(currentPrice)
        // Then apply display formatting (hide .00 if applicable)
        val displayFormatted = formatPriceForDisplay(rounded)
        _uiState.update { it.copy(addonPrice = displayFormatted, errorMessage = null) }
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
     * Save addon (add new or update existing)
     */
    fun saveAddon(onSuccess: () -> Unit) {
        val name = _uiState.value.addonName.trim()
        val priceString = _uiState.value.addonPrice.trim()
        
        // Validation
        if (name.isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.addon_form_validation_name_required))
            }
            return
        }
        
        if (priceString.isEmpty()) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.addon_form_validation_price_required))
            }
            return
        }
        
        // Parse price
        val price = try {
            val parsedPrice = priceString.toDouble()
            if (parsedPrice < 0) {
                _uiState.update { 
                    it.copy(errorMessage = getLocalizedString(R.string.addon_form_validation_price_non_negative))
                }
                return
            }
            parsedPrice
        } catch (e: NumberFormatException) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.addon_form_validation_price_invalid))
            }
            return
        }
        
        // Save addon
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val result = addonId?.let { id ->
                addonRepository.updateAddon(
                    addonId = id,
                    name = name,
                    price = price,
                    sortOrder = _uiState.value.editSortOrder,
                    isActive = _uiState.value.editIsActive
                )
            } ?: run {
                // Create new addon
                addonRepository.createAddon(name, price)
            }
            
            result.onSuccess { _ ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isOfflineSuccess = false
                    )
                }
                // Don't call onSuccess() here - let the dialog handle navigation
            }.onFailure { error ->
                val errorMessage = error.message ?: getLocalizedString(R.string.addon_form_error_save_generic)
                // Handle special error codes
                val finalErrorMessage = when {
                    errorMessage.contains("free_plan_limit_exceeded", ignoreCase = true) -> {
                        "free_plan_limit_exceeded"
                    }
                    errorMessage.contains("ชื่อ Addon นี้มีอยู่แล้ว", ignoreCase = true) ||
                    errorMessage.contains("ชื่อแอดออนนี้มีอยู่แล้ว", ignoreCase = true) ||
                    errorMessage.contains("This addon name already exists", ignoreCase = true) -> {
                        getLocalizedString(R.string.addon_form_error_duplicate_name)
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

