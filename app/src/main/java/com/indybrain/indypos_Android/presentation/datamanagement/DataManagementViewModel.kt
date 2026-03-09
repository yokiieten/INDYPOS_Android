package com.indybrain.indypos_Android.presentation.datamanagement

import android.util.Log
import com.indybrain.indypos_Android.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.CategoryDao
import com.indybrain.indypos_Android.data.export.ExportDataType
import com.indybrain.indypos_Android.data.export.ExportFormat
import com.indybrain.indypos_Android.data.export.ExportService
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.data.local.dao.OrderItemDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.remote.api.ProductsApi
import com.indybrain.indypos_Android.R
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val productRepository: ProductRepository,
    private val addonGroupRepository: AddonGroupRepository,
    private val addonRepository: AddonRepository,
    private val categoryDao: CategoryDao,
    private val addonDao: AddonDao,
    private val addonGroupDao: AddonGroupDao,
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val productsApi: ProductsApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val exportService: ExportService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    fun refreshDataStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Check network availability
            if (networkConnectivityChecker.isConnected()) {
                if (BuildConfig.DEBUG) Log.d("DataManagement", "Network available, fetching statistics from API...")

                try {
                    val response = productsApi.getStatistics()
                    
                    if (response.status == 200 && response.data != null) {
                        val data = response.data!!
                        
                        // Show API values as-is
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                productCount = data.productCount ?: 0,
                                categoryCount = data.categoryCount ?: 0,
                                addonCount = data.addonCount ?: 0,
                                addonGroupCount = data.addonGroupCount ?: 0,
                                orderCount = data.orderCount ?: 0
                            )
                        }
                    } else {
                        val apiError = response.error?.takeIf { it.isNotBlank() } ?: response.message
                        _uiState.update { it.copy(errorMessage = apiError.ifBlank { null }) }
                        refreshDataStatsOffline()
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.e("DataManagement", "Statistics API failed: ${e.message}")
                    _uiState.update { it.copy(errorMessage = e.message) }
                    refreshDataStatsOffline()
                }
            } else {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.logout_no_internet_title)) }
                refreshDataStatsOffline()
            }
        }
    }
    
    private suspend fun refreshDataStatsOffline() {
        try {
            // Get all local counts (original logic for offline mode)
            val productCount = productDao.getTotalProductCount()
            val categoryCount = categoryDao.getAllCategories().size
            val addonCount = addonDao.getAllAddons().size
            val addonGroupCount = addonGroupDao.getAllAddonGroups().size
            val orderCount = orderDao.getOrderCount()
            
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    productCount = productCount,
                    categoryCount = categoryCount,
                    addonCount = addonCount,
                    addonGroupCount = addonGroupCount,
                    orderCount = orderCount
                )
            }
        } catch (e: Exception) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun exportData(dataType: ExportDataType, format: ExportFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null, exportSuccess = false) }
            cleanupOldExportFiles()
            try {
                // Sync from API before export when online (ensures local DB has data)
                if (networkConnectivityChecker.isConnected()) {
                    when (dataType) {
                        ExportDataType.PRODUCTS -> {
                            productRepository.fetchAndSaveProducts()
                        }
                        ExportDataType.CATEGORIES -> {
                            productRepository.fetchAndSyncCategories()
                        }
                        ExportDataType.ADDON_GROUPS -> {
                            addonGroupRepository.fetchAndSyncAddonGroups()
                        }
                        ExportDataType.ADDONS -> {
                            addonRepository.fetchAndSyncAddons()
                        }
                        else -> Unit
                    }
                }
                
                val result = exportService.exportDataByType(dataType, format)
                
                result.fold(
                    onSuccess = { files ->
                        val uris = files.mapNotNull { file ->
                            getUriForFile(file)
                        }
                        _uiState.update { 
                            it.copy(
                                isExporting = false,
                                exportSuccess = true,
                                exportedFiles = uris
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                isExporting = false,
                                exportError = error.message ?: context.getString(R.string.data_export_error_failed)
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isExporting = false,
                        exportError = e.message ?: context.getString(R.string.data_export_error_failed)
                    )
                }
            }
        }
    }
    
    fun clearExportState() {
        _uiState.update { 
            it.copy(
                exportSuccess = false,
                exportError = null,
                exportedFiles = null
            )
        }
    }

    fun clearStatsError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun getUriForFile(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanupOldExportFiles() {
        try {
            val exportExtensions = setOf("csv", "xlsx")
            val exportPrefixes = listOf("INDYPOS_", "data_type_", "Sales_Report_")
            context.cacheDir.listFiles()
                ?.filter { file ->
                    file.isFile &&
                    file.extension in exportExtensions &&
                    exportPrefixes.any { file.name.startsWith(it) }
                }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("DataManagement", "Error cleaning up export files: ${e.message}", e)
        }
    }
}

