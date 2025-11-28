package com.indybrain.indypos_Android.presentation.categorymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Category Management screen
 */
@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()
    
    init {
        // Observe categories from Room database
        observeCategories()
        // Load categories when ViewModel is created
        loadCategories()
    }
    
    /**
     * Load categories - check internet and fetch from API or load from Room
     */
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            // Check internet connectivity
            if (networkConnectivityChecker.isConnected()) {
                // Has internet - fetch from API and sync with Room
                val result = productRepository.fetchAndSyncCategories()
                result.onSuccess {
                    // Data will be updated via observeCategories() Flow
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
            } else {
                // No internet - data will be loaded from Room via Flow
                // isLoading will be set to false by observeCategories() when data arrives
            }
        }
    }
    
    /**
     * Observe categories from Room database
     */
    private fun observeCategories() {
        viewModelScope.launch {
            productRepository.getAllCategoriesFlow().collect { categories ->
                _uiState.update { current ->
                    current.copy(
                        categories = categories.sortedBy { it.sortOrder },
                        isLoading = false // Clear loading state once we have data from Room
                    )
                }
            }
        }
    }
    
    /**
     * Refresh categories
     */
    fun refreshCategories() {
        loadCategories()
    }
    
    /**
     * Search categories
     */
    fun searchCategories(query: String) {
        _uiState.update { current ->
            val filteredCategories = if (query.isBlank()) {
                current.categories
            } else {
                current.categories.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
            }
            current.copy(searchQuery = query, filteredCategories = filteredCategories)
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", filteredCategories = emptyList()) }
    }
}

