package com.indybrain.indypos_Android.presentation.settings.printer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.indybrain.indypos_Android.core.printer.PrinterManager
import com.indybrain.indypos_Android.presentation.settings.printer.ConnectionType
import com.indybrain.indypos_Android.presentation.settings.printer.ConnectionTypeEnum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Printer Settings Screen
 */
@HiltViewModel
class PrinterSettingsViewModel @Inject constructor(
    private val printerManager: PrinterManager
) : ViewModel() {
    
    data class UiState(
        val connectionTypes: List<ConnectionType> = emptyList()
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun updateConnectionTypes() {
        viewModelScope.launch {
            val bluetoothStatus = if (printerManager.isConnected()) {
                "On"
            } else {
                "Off"
            }
            
            _uiState.value = _uiState.value.copy(
                connectionTypes = listOf(
                    ConnectionType(
                        type = ConnectionTypeEnum.Bluetooth,
                        title = "Bluetooth",
                        subtitle = bluetoothStatus,
                        icon = Icons.Outlined.Bluetooth
                    )
                    // Add WiFi and USB later if needed
                    // ConnectionType(
                    //     type = ConnectionTypeEnum.WiFi,
                    //     title = "WiFi",
                    //     subtitle = "Available",
                    //     icon = Icons.Outlined.Wifi
                    // ),
                    // ConnectionType(
                    //     type = ConnectionTypeEnum.USB,
                    //     title = "USB",
                    //     subtitle = "Connected",
                    //     icon = Icons.Outlined.Usb
                    // )
                )
            )
        }
    }
}

