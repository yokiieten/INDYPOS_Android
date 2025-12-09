package com.indybrain.indypos_Android.presentation.ordersummary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.dao.ReceiptSettingsDao
import com.indybrain.indypos_Android.data.local.entity.ReceiptSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderSummaryViewModel @Inject constructor(
    private val receiptSettingsDao: ReceiptSettingsDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderSummaryUiState())
    val uiState: StateFlow<OrderSummaryUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = receiptSettingsDao.getFirstSync()
                _uiState.update {
                    it.copy(
                        isPrintReceiptSelected = settings?.printAfterFinish ?: false,
                        isOpenCashDrawerSelected = settings?.openCashDrawer ?: false
                    )
                }
            } catch (e: Exception) {
                // Use defaults if error
            }
        }
    }
    
    fun togglePrintReceipt() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isPrintReceiptSelected
            _uiState.update { it.copy(isPrintReceiptSelected = newValue) }
            savePrintReceiptSetting(newValue)
        }
    }
    
    fun toggleOpenCashDrawer() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isOpenCashDrawerSelected
            _uiState.update { it.copy(isOpenCashDrawerSelected = newValue) }
            saveOpenCashDrawerSetting(newValue)
        }
    }
    
    private suspend fun savePrintReceiptSetting(isEnabled: Boolean) {
        try {
            val settings = receiptSettingsDao.getFirstSync()
            val now = java.util.Date()
            if (settings != null) {
                receiptSettingsDao.insertOrUpdate(
                    settings.copy(
                        printAfterFinish = isEnabled,
                        updatedAt = now
                    )
                )
            } else {
                receiptSettingsDao.insertOrUpdate(
                    ReceiptSettingsEntity(
                        id = "1",
                        printShopLogo = false,
                        printAfterFinish = isEnabled,
                        showQRCode = false,
                        openCashDrawer = false,
                        paperSize = null,
                        footer = null,
                        shopLogoImagePath = null,
                        promptPayType = null,
                        promptPayIdentifier = null,
                        taxIdentificationNumber = false,
                        tinNumber = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun saveOpenCashDrawerSetting(isEnabled: Boolean) {
        try {
            val settings = receiptSettingsDao.getFirstSync()
            val now = java.util.Date()
            if (settings != null) {
                receiptSettingsDao.insertOrUpdate(
                    settings.copy(
                        openCashDrawer = isEnabled,
                        updatedAt = now
                    )
                )
            } else {
                receiptSettingsDao.insertOrUpdate(
                    ReceiptSettingsEntity(
                        id = "1",
                        printShopLogo = false,
                        printAfterFinish = false,
                        showQRCode = false,
                        openCashDrawer = isEnabled,
                        paperSize = null,
                        footer = null,
                        shopLogoImagePath = null,
                        promptPayType = null,
                        promptPayIdentifier = null,
                        taxIdentificationNumber = false,
                        tinNumber = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

