package com.indybrain.indypos_Android.presentation.addongroupmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : ViewModel() {
    
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
        val isSuccess: Boolean = false,
        val isOfflineSuccess: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadAvailableAddons()
    }
    
    /**
     * Initialize for edit mode
     */
    fun initializeForEdit(addonGroupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val addonGroupWithAddons = addonGroupRepository.getAddonGroupWithAddonsById(addonGroupId)
            if (addonGroupWithAddons != null) {
                _uiState.update { current ->
                    current.copy(
                        isEditMode = true,
                        editingAddonGroupId = addonGroupWithAddons.addonGroup.id,
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
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "ไม่พบกลุ่ม Addon ที่ต้องการแก้ไข"
                    )
                }
            }
        }
    }
    
    /**
     * Load available addons
     */
    private fun loadAvailableAddons() {
        viewModelScope.launch {
            addonRepository.getAllAddonsForManagementFlow()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to load addons: ${e.message}") }
                }
                .collect { addons ->
                    _uiState.update { it.copy(availableAddons = addons) }
                }
        }
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
        _uiState.update { it.copy(formState = it.formState.copy(maxSelection = maxSelection)) }
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
            return "กรุณากรอกชื่อกลุ่ม Addon"
        }
        
        if (formState.selectedAddonIds.isEmpty()) {
            return "กรุณาเลือก Addon อย่างน้อย 1 รายการ"
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
            // Check for duplicate name
            val excludeId = if (_uiState.value.isEditMode) {
                _uiState.value.editingAddonGroupId
            } else {
                null
            }
            
            if (addonGroupRepository.isDuplicateName(formState.groupName, excludeId)) {
                _uiState.update { it.copy(errorMessage = "ชื่อกลุ่ม Addon นี้มีอยู่แล้ว") }
                return@launch
            }
            
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val selectedAddonIds = formState.selectedAddonIds.toList()
            
            if (_uiState.value.isEditMode) {
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
            onSuccess = { addonGroup ->
                val isOffline = !addonGroup.isSynced || !addonGroup.isFromServer
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isOfflineSuccess = isOffline,
                        successMessage = if (isOffline) {
                            "เพิ่มกลุ่ม Addon สำเร็จ (บันทึกในเครื่อง)"
                        } else {
                            "เพิ่มกลุ่ม Addon สำเร็จ"
                        }
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
        
        val result = addonGroupRepository.updateAddonGroup(
            addonGroupId = editingGroupId,
            name = formState.groupName,
            isRequired = formState.isRequired,
            isSingleSelection = maxSelection == 1,
            maxSelection = maxSelection,
            minSelection = 0,
            sortOrder = 1,
            isActive = null,
            selectedAddonIds = selectedAddonIds
        )
        
        result.fold(
            onSuccess = { addonGroup ->
                val isOffline = !addonGroup.isSynced || !addonGroup.isFromServer
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        isOfflineSuccess = isOffline,
                        successMessage = if (isOffline) {
                            "แก้ไขกลุ่ม Addon สำเร็จ (บันทึกในเครื่อง)"
                        } else {
                            "แก้ไขกลุ่ม Addon สำเร็จ"
                        }
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
        val errorMessage = error.message ?: "เกิดข้อผิดพลาดในการสร้างกลุ่ม Addon"
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
        val errorMessage = error.message ?: "เกิดข้อผิดพลาดในการแก้ไขกลุ่ม Addon"
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = addonRepository.createAddon(name, price)
            result.fold(
                onSuccess = {
                    loadAvailableAddons()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "เพิ่ม Addon สำเร็จ"
                        )
                    }
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: "เกิดข้อผิดพลาดในการสร้าง Addon"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage
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


