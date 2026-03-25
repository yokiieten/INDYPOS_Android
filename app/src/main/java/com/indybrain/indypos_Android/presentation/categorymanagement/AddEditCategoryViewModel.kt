package com.indybrain.indypos_Android.presentation.categorymanagement

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Category screen
 */
@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val languageLocalDataSource: LanguageLocalDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Returns string in the user's selected language (respects language change in Settings) */
    private fun getLocalizedString(resId: Int): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId)
    }
    
    private val _uiState = MutableStateFlow(AddEditCategoryUiState())
    val uiState: StateFlow<AddEditCategoryUiState> = _uiState.asStateFlow()
    
    private var categoryId: String? = null
    
    /**
     * Load category data for editing
     */
    fun loadCategory(categoryId: String) {
        this.categoryId = categoryId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            productRepository.getCategoryByIdFromApi(categoryId)
                .onSuccess { category ->
                    _uiState.update {
                        it.copy(
                            categoryName = category.name,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            categoryName = "",
                            isLoading = false,
                            errorMessage = e.message
                                ?: getLocalizedString(R.string.category_form_validation_edit_failed)
                        )
                    }
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
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Save category (add new or update existing)
     */
    fun saveCategory(onSuccess: () -> Unit) {
        val name = _uiState.value.categoryName.trim()
        
        // Validation
        if (name.isBlank()) {
            _uiState.update { 
                it.copy(errorMessage = getLocalizedString(R.string.category_form_validation_name_required))
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
                        errorMessage = getLocalizedString(R.string.api_error_unauthorized)
                    )
                }
                return@launch
            }
            
            val result = categoryId?.let { id ->
                val existingResult = productRepository.getCategoryByIdFromApi(id)
                val existing = existingResult.getOrNull()
                if (existing == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = existingResult.exceptionOrNull()?.message
                                ?: getLocalizedString(R.string.category_form_validation_edit_failed)
                        )
                    }
                    return@launch
                }
                productRepository.updateCategory(
                    categoryId = id,
                    name = name,
                    sortOrder = existing.sortOrder ?: 0,
                    isActive = existing.isActive
                ).map { Unit }
            } ?: run {
                val listResult = productRepository.getAllCategoriesFromApi()
                val list = listResult.getOrNull()
                if (list == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = listResult.exceptionOrNull()?.message
                                ?: getLocalizedString(R.string.category_management_error_loading)
                        )
                    }
                    return@launch
                }
                val maxSortOrder = list.mapNotNull { it.sortOrder }.maxOrNull() ?: 0
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
                        errorMessage = error.message ?: if (categoryId != null) {
                            getLocalizedString(R.string.category_form_validation_edit_failed)
                        } else {
                            getLocalizedString(R.string.category_management_error_loading)
                        }
                    )
                }
            }
        }
    }
}

