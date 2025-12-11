package com.indybrain.indypos_Android.presentation.datamanagement

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.AddonDao
import com.indybrain.indypos_Android.data.local.dao.AddonGroupDao
import com.indybrain.indypos_Android.data.local.dao.CategoryDao
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.data.remote.api.ProductsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val addonDao: AddonDao,
    private val addonGroupDao: AddonGroupDao,
    private val orderDao: OrderDao,
    private val productsApi: ProductsApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()
    
    init {
        refreshDataStats()
    }
    
    fun refreshDataStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Check network availability
            if (networkConnectivityChecker.isConnected()) {
                Log.d("DataManagement", "🌐 Network available, fetching statistics from API...")
                
                try {
                    // Get local unsynced counts only (for online mode - will combine with API counts)
                    val localProductCount = productDao.getUnsyncedProducts().size
                    val localCategoryCount = categoryDao.getUnsyncedCategories().size
                    val localAddonCount = addonDao.getUnsyncedAddonsCount()
                    val localAddonGroupCount = addonGroupDao.getUnsyncedAddonGroupsCount()
                    val localOrderCount = orderDao.getUnsyncedOrdersCount()
                    
                    // Call Statistics API
                    val response = productsApi.getStatistics()
                    
                    if (response.status == 200 && response.data != null) {
                        val data = response.data!!
                        Log.d("DataManagement", "✅ Statistics API success")
                        Log.d("DataManagement", "   📊 API counts - Products: ${data.productCount ?: 0}, Categories: ${data.categoryCount ?: 0}, Addons: ${data.addonCount ?: 0}, AddonGroups: ${data.addonGroupCount ?: 0}, Orders: ${data.orderCount ?: 0}")
                        Log.d("DataManagement", "   📱 Local unsynced counts - Products: $localProductCount, Categories: $localCategoryCount, Addons: $localAddonCount, AddonGroups: $localAddonGroupCount, Orders: $localOrderCount")
                        
                        // Combine API counts with local unsynced counts
                        val totalProductCount = (data.productCount ?: 0) + localProductCount
                        val totalCategoryCount = (data.categoryCount ?: 0) + localCategoryCount
                        val totalAddonCount = (data.addonCount ?: 0) + localAddonCount
                        val totalAddonGroupCount = (data.addonGroupCount ?: 0) + localAddonGroupCount
                        val totalOrderCount = (data.orderCount ?: 0) + localOrderCount
                        
                        Log.d("DataManagement", "   ✅ Total counts - Products: $totalProductCount, Categories: $totalCategoryCount, Addons: $totalAddonCount, AddonGroups: $totalAddonGroupCount, Orders: $totalOrderCount")
                        
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                productCount = totalProductCount,
                                categoryCount = totalCategoryCount,
                                addonCount = totalAddonCount,
                                addonGroupCount = totalAddonGroupCount,
                                orderCount = totalOrderCount
                            )
                        }
                    } else {
                        Log.w("DataManagement", "⚠️ Statistics API returned error, using local counts only")
                        // Fallback to local counts (all data, not just unsynced)
                        refreshDataStatsOffline()
                    }
                } catch (e: Exception) {
                    Log.e("DataManagement", "❌ Statistics API failed: ${e.message}, using local counts only")
                    // Fallback to local counts (all data, not just unsynced)
                    refreshDataStatsOffline()
                }
            } else {
                Log.d("DataManagement", "📴 No network available, using local counts only")
                // No network, use original logic (all data)
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
}

