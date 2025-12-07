package com.indybrain.indypos_Android.presentation.categorymanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Category screen
 */
@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEditCategoryUiState())
    val uiState: StateFlow<AddEditCategoryUiState> = _uiState.asStateFlow()
    
    private var categoryId: String? = null
    
    /**
     * Load category data for editing
     */
    fun loadCategory(categoryId: String) {
        this.categoryId = categoryId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val category = productRepository.getCategoryById(categoryId)
            _uiState.update { 
                it.copy(
                    categoryName = category?.name ?: "",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Update category name
     */
    fun updateCategoryName(name: String) {
        _uiState.update { it.copy(categoryName = name, errorMessage = null) }
    }
    
    /**
     * Dismiss success dialog and reset success state
     */
    fun dismissSuccessDialog() {
        _uiState.update { it.copy(isSuccess = false) }
    }
    
    /**
     * Save category (add new or update existing)
     */
    fun saveCategory(onSuccess: () -> Unit) {
        val name = _uiState.value.categoryName.trim()
        
        // Validation
        if (name.isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = "กรุณากรอกชื่อหมวดหมู่")
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val userId = productRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "ไม่พบข้อมูลผู้ใช้ กรุณาเข้าสู่ระบบใหม่"
                    )
                }
                return@launch
            }
            
            val result = categoryId?.let { id ->
                // Update existing category - use updateCategory which handles API call and Room save
                val existingCategory = productRepository.getCategoryById(id)
                if (existingCategory == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "ไม่พบหมวดหมู่ที่ต้องการแก้ไข"
                        )
                    }
                    return@launch
                }
                
                productRepository.updateCategory(
                    categoryId = id,
                    name = name,
                    sortOrder = existingCategory.sortOrder ?: 0, // Handle nullable sortOrder
                    isActive = existingCategory.isActive
                ).map { Unit }
            } ?: run {
                // Add new category - use createCategory which handles API call and Room save
                val maxSortOrder = productRepository.getAllCategories()
                    .mapNotNull { it.sortOrder } // Filter out null values
                    .maxOrNull() ?: 0
                productRepository.createCategory(
                    name = name,
                    sortOrder = maxSortOrder + 1,
                    isActive = true
                ).map { Unit }
            }
            
            result.onSuccess {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                // Don't call onSuccess() here - let the dialog handle navigation
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "เกิดข้อผิดพลาดในการบันทึก"
                    )
                }
            }
        }
    }
}

