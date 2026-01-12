package com.indybrain.indypos_Android.presentation.settings.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.indybrain.indypos_Android.core.printer.PrinterManager
import com.indybrain.indypos_Android.core.printer.PrinterType
import com.indybrain.indypos_Android.core.printer.LabelPrinterService
import com.indybrain.indypos_Android.domain.repository.PrinterSettingsRepository
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Printer Settings Screen
 */
@HiltViewModel
class PrinterSettingsViewModel @Inject constructor(
    private val printerManager: PrinterManager,
    private val printerSettingsRepository: PrinterSettingsRepository,
    private val labelPrinterService: LabelPrinterService,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    data class PrinterInfo(
        val type: PrinterType,
        val title: String,
        val printerName: String?,
        val macAddress: String?,
        val isConnected: Boolean,
        val isEnabled: Boolean
    )
    
    data class UiState(
        val receiptPrinter: PrinterInfo? = null,
        val labelPrinter: PrinterInfo? = null
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        initializePrinterSettings()
    }
    
    private fun initializePrinterSettings() {
        viewModelScope.launch {
            // Initialize default settings for all printer types if not exists
            printerSettingsRepository.initializeDefaultSettings()
            
            // Load printer settings
            loadPrinterSettings()
        }
    }
    
    private fun loadPrinterSettings() {
        viewModelScope.launch {
            // Observe both printer settings
            combine(
                printerSettingsRepository.getPrinterSettings(PrinterType.RECEIPT),
                printerSettingsRepository.getPrinterSettings(PrinterType.LABEL)
            ) { receiptSettings, labelSettings ->
                UiState(
                    receiptPrinter = PrinterInfo(
                        type = PrinterType.RECEIPT,
                        title = "เครื่องพิมพ์ใบเสร็จ (Receipt Printer)",
                        printerName = receiptSettings?.printerName,
                        macAddress = receiptSettings?.printerMacAddress,
                        isConnected = printerManager.isConnected(PrinterType.RECEIPT),
                        isEnabled = receiptSettings?.enabled ?: false
                    ),
                    labelPrinter = PrinterInfo(
                        type = PrinterType.LABEL,
                        title = "เครื่องพิมพ์สติกเกอร์ (Label Printer)",
                        printerName = labelSettings?.printerName,
                        macAddress = labelSettings?.printerMacAddress,
                        isConnected = printerManager.isConnected(PrinterType.LABEL),
                        isEnabled = labelSettings?.enabled ?: false
                    )
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun togglePrinter(type: PrinterType, enabled: Boolean) {
        viewModelScope.launch {
            printerSettingsRepository.updateEnabled(type, enabled)
            loadPrinterSettings()
        }
    }
    
    /**
     * Test print for label printer
     * Returns true if print successful, false otherwise
     */
    fun testLabelPrint() {
        viewModelScope.launch {
            try {
                // Get shop name from auth repository
                val user = authRepository.getCurrentUser().firstOrNull()
                val shopName = user?.shopName?.takeUnless { it.isBlank() } 
                    ?: user?.firstName 
                    ?: "Test Shop"
                
                android.util.Log.d("PrinterSettings", "Testing label print with shop name: $shopName")
                val success = labelPrinterService.testPrint(shopName)
                
                if (success) {
                    android.util.Log.d("PrinterSettings", "Test print successful")
                } else {
                    android.util.Log.e("PrinterSettings", "Test print failed")
                }
            } catch (e: Exception) {
                android.util.Log.e("PrinterSettings", "Error testing label print", e)
            }
        }
    }
}

