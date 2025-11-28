package com.indybrain.indypos_Android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.LanguageOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Language Settings screen
 */
@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val languageLocalDataSource: LanguageLocalDataSource
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LanguageSettingsUiState())
    val uiState: StateFlow<LanguageSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentLanguage()
    }
    
    /**
     * Load current language preference
     */
    private fun loadCurrentLanguage() {
        val currentLanguage = languageLocalDataSource.getCurrentLanguageOption()
        _uiState.update { it.copy(selectedLanguage = currentLanguage) }
    }
    
    /**
     * Handle language selection
     * Returns true if language was changed (different from current)
     */
    fun selectLanguage(languageOption: LanguageOption): Boolean {
        val currentLanguage = languageLocalDataSource.getCurrentLanguageOption()
        val isChanged = currentLanguage != languageOption
        
        if (isChanged) {
            viewModelScope.launch {
                // Save language preference
                languageLocalDataSource.saveLanguageLocale(languageOption.localeCode)
                
                // Update UI state
                _uiState.update { it.copy(selectedLanguage = languageOption) }
            }
        }
        
        return isChanged
    }
}

/**
 * UI State for Language Settings screen
 */
data class LanguageSettingsUiState(
    val selectedLanguage: LanguageOption = LanguageOption.Thai
)

