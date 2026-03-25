package com.indybrain.indypos_Android.presentation.datamanagement

import android.util.Log
import com.indybrain.indypos_Android.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.export.ExportDataType
import com.indybrain.indypos_Android.data.export.ExportFormat
import com.indybrain.indypos_Android.data.export.ExportService
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.api.ProductsApi
import com.indybrain.indypos_Android.data.remote.dto.*
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
    private val productsApi: ProductsApi,
    private val ordersApi: OrdersApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    private val exportService: ExportService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    fun refreshDataStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            if (!networkConnectivityChecker.isConnected()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.logout_no_internet_title)
                    )
                }
                return@launch
            }

            try {
                val response = productsApi.getStatistics()
                if (response.status == 200 && response.data != null) {
                    val data = response.data!!
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
                    _uiState.update { it.copy(isLoading = false, errorMessage = apiError.ifBlank { null }) }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e("DataManagement", "Statistics API failed: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
    
    fun exportData(dataType: ExportDataType, format: ExportFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportError = null, exportSuccess = false) }
            cleanupOldExportFiles()

            if (!networkConnectivityChecker.isConnected()) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = context.getString(R.string.logout_no_internet_title)
                    )
                }
                return@launch
            }

            try {
                val files: List<File> = when (dataType) {
                    ExportDataType.PRODUCTS -> {
                        val products = fetchAllProducts()
                        val categories = fetchAllCategories()
                        val addonGroups = fetchAllAddonGroups()
                        listOfNotNull(exportService.exportProducts(products, categories, addonGroups, format))
                    }
                    ExportDataType.CATEGORIES -> {
                        val categories = fetchAllCategories()
                        listOfNotNull(exportService.exportCategories(categories, format))
                    }
                    ExportDataType.ADDON_GROUPS -> {
                        val addonGroups = fetchAllAddonGroups()
                        listOfNotNull(exportService.exportAddonGroups(addonGroups, format))
                    }
                    ExportDataType.ADDONS -> {
                        val addons = fetchAllAddons()
                        listOfNotNull(exportService.exportAddons(addons, format))
                    }
                    ExportDataType.ORDERS -> {
                        val orders = fetchAllOrders()
                        listOfNotNull(exportService.exportOrders(orders, format))
                    }
                    ExportDataType.SALES_REPORT -> {
                        val orders = fetchAllOrders()
                        exportService.exportSalesReport(orders, format) ?: emptyList()
                    }
                    ExportDataType.STOCK_REPORT -> {
                        val products = fetchAllProducts()
                        val categories = fetchAllCategories()
                        exportService.exportStockReport(products, categories, format) ?: emptyList()
                    }
                }

                val uris = files.mapNotNull { getUriForFile(it) }
                _uiState.update {
                    it.copy(isExporting = false, exportSuccess = true, exportedFiles = uris)
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e("DataManagement", "Export failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = e.message ?: context.getString(R.string.data_export_error_failed)
                    )
                }
            }
        }
    }

    // ──────────────────────── API fetchers ────────────────────────

    private suspend fun fetchAllProducts(): List<ProductDto> {
        val response = productsApi.getMyProductsAll()
        if (response.status != 200 || response.data == null) {
            throw RuntimeException(response.error?.takeIf { it.isNotBlank() } ?: "Failed to fetch products")
        }
        return response.data!!
    }

    private suspend fun fetchAllCategories(): List<CategoryDto> {
        val response = productsApi.getCategories()
        if (response.status != 200 || response.data == null) {
            throw RuntimeException(response.error?.takeIf { it.isNotBlank() } ?: "Failed to fetch categories")
        }
        return response.data!!
    }

    private suspend fun fetchAllAddonGroups(): List<AddonGroupDto> {
        val response = productsApi.getAddonGroups()
        if (response.status != 200 || response.data == null) {
            throw RuntimeException(response.error?.takeIf { it.isNotBlank() } ?: "Failed to fetch addon groups")
        }
        return response.data!!
    }

    private suspend fun fetchAllAddons(): List<AddonDto> {
        val response = productsApi.getAddons()
        if (response.status != 200 || response.data == null) {
            throw RuntimeException(response.error?.takeIf { it.isNotBlank() } ?: "Failed to fetch addons")
        }
        return response.data!!
    }

    private suspend fun fetchAllOrders(): List<OrderDto> {
        val allOrders = mutableListOf<OrderDto>()
        var page = 1
        val pageSize = 100
        while (true) {
            val response = ordersApi.getOrders(limit = pageSize, page = page)
            val pageData = response.data ?: break
            val orders = pageData.orders ?: break
            allOrders.addAll(orders)
            if (pageData.pagination?.hasNext != true) break
            page++
        }
        return allOrders
    }

    // ──────────────────────── Utility ────────────────────────

    fun clearExportState() {
        _uiState.update { it.copy(exportSuccess = false, exportError = null, exportedFiles = null) }
    }

    fun clearStatsError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun getUriForFile(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    private fun cleanupOldExportFiles() {
        try {
            val exportExtensions = setOf("csv", "xlsx")
            val exportPrefixes = listOf("INDYPOS_", "data_type_", "Sales_Report_")
            context.cacheDir.listFiles()
                ?.filter { file ->
                    file.isFile && file.extension in exportExtensions && exportPrefixes.any { file.name.startsWith(it) }
                }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("DataManagement", "Error cleaning up export files: ${e.message}", e)
        }
    }
}
