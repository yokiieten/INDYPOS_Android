package com.indybrain.indypos_Android.presentation.addongroupmanagement

import androidx.lifecycle.ViewModel
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Addon Group screen
 */
@HiltViewModel
class AddEditAddonGroupViewModel @Inject constructor(
    private val addonGroupRepository: AddonGroupRepository,
    private val addonRepository: AddonRepository,
    private val languageLocalDataSource: LanguageLocalDataSource,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Returns string in the user's selected language (respects language change in Settings) */
    private fun getLocalizedString(resId: Int): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId)
    }

    private fun getLocalizedString(resId: Int, vararg formatArgs: Any): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId, *formatArgs)
    }
    
    data class FormState(
        val groupName: String = "",
        val isRequired: Boolean = false,
        val maxSelection: String = "",
        val selectedAddonIds: Set<String> = emptySet()
    )
    
    data class UiState(
        val formState: FormState = FormState(),
        val availableAddons: List<AddonEntity> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val isEditMode: Boolean = false,
        val editingAddonGroupId: String? = null,
        /** Preserved from server when opening edit — needed for PUT without a pre-save GET */
        val editingAddonGroupIsActive: Boolean = true,
        val isSuccess: Boolean = false,
        val isOfflineSuccess: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            loadAvailableAddonsFromApi()
        }
    }
    
    /**
     * Initialize for edit mode — loads group + linked addons from API only.
     */
    fun initializeForEdit(addonGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            addonGroupRepository.getAddonGroupWithAddonsFromApi(addonGroupId).fold(
                onSuccess = { addonGroupWithAddons ->
                    _uiState.update { current ->
                        current.copy(
                            isEditMode = true,
                            editingAddonGroupId = addonGroupWithAddons.addonGroup.id,
                            editingAddonGroupIsActive = addonGroupWithAddons.addonGroup.isActive,
                            formState = FormState(
                                groupName = addonGroupWithAddons.addonGroup.name,
                                isRequired = addonGroupWithAddons.addonGroup.isRequired,
                                maxSelection = if (addonGroupWithAddons.addonGroup.maxSelection != null && addonGroupWithAddons.addonGroup.maxSelection!! > 0) {
                                    addonGroupWithAddons.addonGroup.maxSelection.toString()
                                } else "",
                                selectedAddonIds = addonGroupWithAddons.addons.mapNotNull { it.id }.toSet()
                            ),
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: getLocalizedString(R.string.addon_group_form_error_not_found)
                        )
                    }
                }
            )
        }
    }
    
    private suspend fun loadAvailableAddonsFromApi() {
        addonRepository.getAllAddonsFromApi().fold(
            onSuccess = { addons ->
                _uiState.update { it.copy(availableAddons = addons) }
            },
            onFailure = { e ->
                val reason = e.message ?: getLocalizedString(R.string.common_error)
                _uiState.update {
                    it.copy(
                        errorMessage = getLocalizedString(
                            R.string.addon_group_form_error_load_addons_with_reason,
                            reason
                        )
                    )
                }
            }
        )
    }
    
    /**
     * Update form fields
     */
    fun updateGroupName(name: String) {
        _uiState.update { it.copy(formState = it.formState.copy(groupName = name), errorMessage = null) }
    }
    
    fun toggleIsRequired() {
        _uiState.update { it.copy(formState = it.formState.copy(isRequired = !it.formState.isRequired)) }
    }
    
    fun updateMaxSelection(maxSelection: String) {
        _uiState.update { 
            it.copy(
                formState = it.formState.copy(maxSelection = maxSelection),
                errorMessage = null // Clear error when user updates the field
            ) 
        }
    }
    
    fun toggleAddonSelection(addonId: String) {
        _uiState.update { current ->
            val selectedIds = current.formState.selectedAddonIds.toMutableSet()
            if (selectedIds.contains(addonId)) {
                selectedIds.remove(addonId)
            } else {
                selectedIds.add(addonId)
            }
            current.copy(formState = current.formState.copy(selectedAddonIds = selectedIds))
        }
    }
    
    /**
     * Validate form
     */
    private fun validateForm(): String? {
        val formState = _uiState.value.formState
        
        if (formState.groupName.trim().isEmpty()) {
            return getLocalizedString(R.string.addon_group_form_validation_name_required)
        }
        
        if (formState.selectedAddonIds.isEmpty()) {
            return getLocalizedString(R.string.addon_group_form_validation_addons_select_required)
        }
        
        // Validate maxSelection - cannot be 0
        if (formState.maxSelection.isNotEmpty()) {
            val maxSelectionValue = formState.maxSelection.toIntOrNull()
            if (maxSelectionValue != null && maxSelectionValue == 0) {
                return getLocalizedString(R.string.addon_group_max_selection_zero_error)
            }
        }
        
        return null
    }
    
    /**
     * Save addon group
     */
    fun saveAddonGroup() {
        val validationError = validateForm()
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        
        val formState = _uiState.value.formState
        val maxSelection = if (formState.maxSelection.isEmpty()) 1 else formState.maxSelection.toIntOrNull() ?: 1
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val selectedAddonIds = formState.selectedAddonIds.toList()

            if (_uiState.value.isEditMode) {
                if (_uiState.value.editingAddonGroupId == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                updateAddonGroup(formState, maxSelection, selectedAddonIds)
            } else {
                createAddonGroup(formState, maxSelection, selectedAddonIds)
            }
        }
    }
    
    /**
     * Create addon group
     */
    private suspend fun createAddonGroup(formState: FormState, maxSelection: Int, selectedAddonIds: List<String>) {
        val result = addonGroupRepository.createAddonGroup(
            name = formState.groupName,
            isRequired = formState.isRequired,
            isSingleSelection = maxSelection == 1,
            maxSelection = maxSelection,
            minSelection = 0,
            sortOrder = 1,
            selectedAddonIds = selectedAddonIds
        )
        
        result.fold(
            onSuccess = { _ ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isOfflineSuccess = false,
                        successMessage = getLocalizedString(R.string.addon_group_form_success_add)
                    )
                }
            },
            onFailure = { error ->
                handleCreateError(error)
            }
        )
    }
    
    /**
     * Update addon group
     */
    private suspend fun updateAddonGroup(formState: FormState, maxSelection: Int, selectedAddonIds: List<String>) {
        val editingGroupId = _uiState.value.editingAddonGroupId ?: return
        val isActive = _uiState.value.editingAddonGroupIsActive

        val result = addonGroupRepository.updateAddonGroup(
            addonGroupId = editingGroupId,
            name = formState.groupName,
            isRequired = formState.isRequired,
            isSingleSelection = maxSelection == 1,
            maxSelection = maxSelection,
            minSelection = 0,
            sortOrder = 1,
            isActive = isActive,
            selectedAddonIds = selectedAddonIds
        )
        
        result.fold(
            onSuccess = { _ ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isOfflineSuccess = false,
                        successMessage = getLocalizedString(R.string.addon_group_form_success_edit)
                    )
                }
            },
            onFailure = { error ->
                handleUpdateError(error)
            }
        )
    }
    
    /**
     * Handle create error
     */
    private fun handleCreateError(error: Throwable) {
        val errorMessage = error.message ?: getLocalizedString(R.string.addon_group_form_error_create)
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = errorMessage
            )
        }
    }
    
    /**
     * Handle update error
     */
    private fun handleUpdateError(error: Throwable) {
        val errorMessage = error.message ?: getLocalizedString(R.string.addon_group_form_error_update)
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = errorMessage
            )
        }
    }
    
    /**
     * Create new addon
     */
    fun createAddon(name: String, price: Double) {
        // Validation - close dialog and show as popup
        if (name.trim().isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = getLocalizedString(R.string.addon_form_validation_name_required)
                )
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = addonRepository.createAddon(name, price)
            result.fold(
                onSuccess = {
                    loadAvailableAddonsFromApi()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = getLocalizedString(R.string.addon_form_success_add_single)
                        )
                    }
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: getLocalizedString(R.string.addon_form_error_create_addon)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage,
                            successMessage = null
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Clear messages
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    fun dismissSuccessDialog() {
        _uiState.update { it.copy(isSuccess = false, isOfflineSuccess = false) }
    }
}


