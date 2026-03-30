package com.indybrain.indypos_Android.presentation.settings.receipt

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.data.local.LanguageLocalDataSource
import com.indybrain.indypos_Android.domain.model.PromptPayType
import com.indybrain.indypos_Android.domain.repository.ReceiptSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptSettingsViewModel @Inject constructor(
    private val receiptSettingsRepository: ReceiptSettingsRepository,
    @ApplicationContext private val context: Context,
    private val languageLocalDataSource: LanguageLocalDataSource
) : ViewModel() {

    private fun getLocalizedString(resId: Int): String {
        val localeCode = languageLocalDataSource.getLanguageLocale()
        val localizedContext = LocaleHelper.setLocale(context, localeCode)
        return localizedContext.getString(resId)
    }
    
    private val _uiState = MutableStateFlow(ReceiptSettingsUiState())
    val uiState: StateFlow<ReceiptSettingsUiState> = _uiState.asStateFlow()
    
    // Temporary state for unsaved changes
    private var tempState = ReceiptSettingsUiState()
    
    // Pending shop logo changes (not yet saved to database)
    private var pendingShopLogoUri: Uri? = null
    private var removeShopLogo: Boolean = false
    
    // Flag to prevent Flow from overriding data after save
    private var isSavingInProgress = false
    
    /** Last loaded/saved settings values — not the live UI mirror; avoids hasChanges flipping false when IME re-sends the same text. */
    private var persistedSettings = PersistedReceiptSettings()
    
    init {
        initializeSettings()
    }
    
    private fun initializeSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Initialize default settings if not exists
            receiptSettingsRepository.initializeDefaultSettings()
            
            // Load current settings
            receiptSettingsRepository.getReceiptSettings()
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "ไม่สามารถโหลดการตั้งค่าได้: ${e.message}"
                        )
                    }
                }
                .collect { settings ->
                    // Skip update if we're currently saving to prevent data loss
                    if (isSavingInProgress) {
                        return@collect
                    }
                    
                    if (settings != null) {
                        val shopLogoBitmap = receiptSettingsRepository.getShopLogoBitmap()
                        
                        tempState = ReceiptSettingsUiState(
                            printShopLogo = settings.printShopLogo,
                            printAfterFinish = settings.printAfterFinish,
                            showQRCode = settings.showQRCode,
                            openCashDrawer = settings.openCashDrawer,
                            taxIdentificationNumber = settings.taxIdentificationNumber,
                            paperSize = settings.paperSize ?: "58",
                            footer = settings.footer ?: "",
                            shopLogoBitmap = shopLogoBitmap,
                            promptPayType = PromptPayType.fromString(settings.promptPayType),
                            promptPayIdentifier = settings.promptPayIdentifier ?: "",
                            tinNumber = sanitizeTinInput(settings.tinNumber ?: "")
                        )
                        persistedSettings = PersistedReceiptSettings.from(tempState)
                        
                        // Reset pending logo operations
                        pendingShopLogoUri = null
                        removeShopLogo = false
                        
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                printShopLogo = tempState.printShopLogo,
                                printAfterFinish = tempState.printAfterFinish,
                                showQRCode = tempState.showQRCode,
                                openCashDrawer = tempState.openCashDrawer,
                                taxIdentificationNumber = tempState.taxIdentificationNumber,
                                paperSize = tempState.paperSize,
                                footer = tempState.footer,
                                shopLogoBitmap = tempState.shopLogoBitmap,
                                promptPayType = tempState.promptPayType,
                                promptPayIdentifier = tempState.promptPayIdentifier,
                                tinNumber = tempState.tinNumber
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }
    
    fun updatePrintShopLogo(enabled: Boolean) {
        tempState = tempState.copy(printShopLogo = enabled)
        checkForChanges()
    }
    
    fun updatePrintAfterFinish(enabled: Boolean) {
        tempState = tempState.copy(printAfterFinish = enabled)
        checkForChanges()
    }
    
    fun updateShowQRCode(enabled: Boolean) {
        tempState = tempState.copy(showQRCode = enabled)
        if (!enabled) {
            // Clear validation error when disabled
            tempState = tempState.copy(promptPayIdentifierError = null)
        }
        checkForChanges()
    }
    
    fun updateOpenCashDrawer(enabled: Boolean) {
        tempState = tempState.copy(openCashDrawer = enabled)
        checkForChanges()
    }
    
    fun updateTaxIdentificationNumber(enabled: Boolean) {
        tempState = tempState.copy(taxIdentificationNumber = enabled)
        if (!enabled) {
            tempState = tempState.copy(tinNumberError = null)
        } else {
            validateTIN()
        }
        checkForChanges()
    }
    
    fun updatePaperSize(size: String) {
        tempState = tempState.copy(paperSize = size)
        checkForChanges()
    }
    
    fun updateFooter(footer: String) {
        tempState = tempState.copy(footer = footer)
        checkForChanges()
    }
    
    fun updatePromptPayType(type: PromptPayType) {
        tempState = tempState.copy(
            promptPayType = type,
            promptPayIdentifierError = null // Clear error when type changes
        )
        checkForChanges()
    }
    
    fun updatePromptPayIdentifier(identifier: String) {
        tempState = tempState.copy(
            promptPayIdentifier = identifier,
            promptPayIdentifierError = null
        )
        validatePromptPay()
        checkForChanges()
    }
    
    fun updateTINNumber(tinNumber: String) {
        tempState = tempState.copy(
            tinNumber = sanitizeTinInput(tinNumber),
            tinNumberError = null
        )
        validateTIN()
        checkForChanges()
    }
    
    fun updateShopLogoImage(imageUri: Uri?) {
        viewModelScope.launch {
            if (imageUri == null) {
                // User chose to remove current logo (but don't persist yet)
                pendingShopLogoUri = null
                removeShopLogo = true
                tempState = tempState.copy(shopLogoBitmap = null)
                checkForChanges()
            } else {
                // User selected a new logo; generate preview only
                removeShopLogo = false
                pendingShopLogoUri = imageUri
                
                val result = receiptSettingsRepository.generateShopLogoPreview(imageUri)
                result.onSuccess { bitmap ->
                    tempState = tempState.copy(shopLogoBitmap = bitmap)
                    checkForChanges()
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(errorMessage = "ไม่สามารถประมวลผลรูปภาพได้: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun validatePromptPay() {
        if (!tempState.showQRCode) {
            tempState = tempState.copy(promptPayIdentifierError = null)
            return
        }
        
        val identifier = tempState.promptPayIdentifier.trim()
        if (identifier.isEmpty()) {
            tempState = tempState.copy(
                promptPayIdentifierError = "กรุณากรอกข้อมูล PromptPay"
            )
            return
        }
        
        val isValid = when (tempState.promptPayType) {
            PromptPayType.PHONE_NUMBER -> {
                identifier.matches(Regex("^0[0-9]{9}$"))
            }
            PromptPayType.NATIONAL_ID -> {
                identifier.matches(Regex("^[0-9]{13}$"))
            }
            PromptPayType.E_WALLET -> {
                identifier.isNotBlank()
            }
        }
        
        if (!isValid) {
            val errorMessage = when (tempState.promptPayType) {
                PromptPayType.PHONE_NUMBER -> "รูปแบบหมายเลขโทรศัพท์ไม่ถูกต้อง (ต้องขึ้นต้นด้วย 0 และมี 10 หลัก)"
                PromptPayType.NATIONAL_ID -> "รูปแบบเลขบัตรประชาชนไม่ถูกต้อง (ต้องมี 13 หลัก)"
                PromptPayType.E_WALLET -> "กรุณากรอก e-Wallet ID"
            }
            tempState = tempState.copy(promptPayIdentifierError = errorMessage)
        } else {
            tempState = tempState.copy(promptPayIdentifierError = null)
        }
    }
    
    private fun validateTIN() {
        if (!tempState.taxIdentificationNumber) {
            tempState = tempState.copy(tinNumberError = null)
            return
        }
        
        val tin = tempState.tinNumber
        if (tin.isEmpty()) {
            tempState = tempState.copy(
                tinNumberError = getLocalizedString(R.string.settings_tin_required)
            )
            return
        }
        
        if (tin.length != 13) {
            tempState = tempState.copy(
                tinNumberError = getLocalizedString(R.string.settings_tin_error_must_be_13_digits)
            )
        } else {
            tempState = tempState.copy(tinNumberError = null)
        }
    }
    
    private fun checkForChanges() {
        val hasChanges = hasDraftChangesComparedToPersisted()
        
        _uiState.update { 
            it.copy(
                printShopLogo = tempState.printShopLogo,
                printAfterFinish = tempState.printAfterFinish,
                showQRCode = tempState.showQRCode,
                openCashDrawer = tempState.openCashDrawer,
                taxIdentificationNumber = tempState.taxIdentificationNumber,
                paperSize = tempState.paperSize,
                footer = tempState.footer,
                shopLogoBitmap = tempState.shopLogoBitmap,
                promptPayType = tempState.promptPayType,
                promptPayIdentifier = tempState.promptPayIdentifier,
                promptPayIdentifierError = tempState.promptPayIdentifierError,
                tinNumber = tempState.tinNumber,
                tinNumberError = tempState.tinNumberError,
                hasChanges = hasChanges
            )
        }
    }
    
    private fun hasDraftChangesComparedToPersisted(): Boolean {
        if (pendingShopLogoUri != null || removeShopLogo) return true
        val p = persistedSettings
        val t = tempState
        return t.printShopLogo != p.printShopLogo ||
            t.printAfterFinish != p.printAfterFinish ||
            t.showQRCode != p.showQRCode ||
            t.openCashDrawer != p.openCashDrawer ||
            t.taxIdentificationNumber != p.taxIdentificationNumber ||
            t.paperSize != p.paperSize ||
            t.footer != p.footer ||
            t.promptPayType != p.promptPayType ||
            t.promptPayIdentifier != p.promptPayIdentifier ||
            t.tinNumber != p.tinNumber ||
            t.shopLogoBitmap !== p.shopLogoBitmap
    }
    
    fun saveSettings() {
        if (!isValidForSaving()) {
            _uiState.update {
                it.copy(
                    errorMessage = validationBlockReason()
                        ?: "กรุณาแก้ข้อมูลให้ครบถ้วนก่อนบันทึก"
                )
            }
            return
        }
        
        viewModelScope.launch {
            isSavingInProgress = true
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            
            try {
                // Save all settings
                receiptSettingsRepository.updatePrintShopLogo(tempState.printShopLogo)
                receiptSettingsRepository.updatePrintAfterFinish(tempState.printAfterFinish)
                receiptSettingsRepository.updateShowQRCode(tempState.showQRCode)
                receiptSettingsRepository.updateOpenCashDrawer(tempState.openCashDrawer)
                receiptSettingsRepository.updateTaxIdentificationNumber(tempState.taxIdentificationNumber)
                receiptSettingsRepository.updatePaperSize(tempState.paperSize)
                receiptSettingsRepository.updateFooter(tempState.footer)
                receiptSettingsRepository.updatePromptPayType(tempState.promptPayType)
                receiptSettingsRepository.updatePromptPayIdentifier(tempState.promptPayIdentifier)
                receiptSettingsRepository.updateTINNumber(tempState.tinNumber)
                
                // Save shop logo (add / change / remove) only when user confirms saving
                if (removeShopLogo) {
                    receiptSettingsRepository.updateShopLogoImage(null)
                } else if (pendingShopLogoUri != null) {
                    receiptSettingsRepository.updateShopLogoImage(pendingShopLogoUri)
                }
                
                // Reload settings to sync state
                val settings = receiptSettingsRepository.getReceiptSettingsSync()
                if (settings != null) {
                    val shopLogoBitmap = receiptSettingsRepository.getShopLogoBitmap()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            successMessage = "บันทึกการตั้งค่าสำเร็จ",
                            printShopLogo = settings.printShopLogo,
                            printAfterFinish = settings.printAfterFinish,
                            showQRCode = settings.showQRCode,
                            openCashDrawer = settings.openCashDrawer,
                            taxIdentificationNumber = settings.taxIdentificationNumber,
                            paperSize = settings.paperSize ?: "58",
                            footer = settings.footer ?: "",
                            shopLogoBitmap = shopLogoBitmap,
                            promptPayType = PromptPayType.fromString(settings.promptPayType),
                            promptPayIdentifier = settings.promptPayIdentifier ?: "",
                            tinNumber = sanitizeTinInput(settings.tinNumber ?: ""),
                            hasChanges = false
                        )
                    }
                    
                    // Update temp state to match saved state (important for reloading)
                    tempState = ReceiptSettingsUiState(
                        printShopLogo = settings.printShopLogo,
                        printAfterFinish = settings.printAfterFinish,
                        showQRCode = settings.showQRCode,
                        openCashDrawer = settings.openCashDrawer,
                        taxIdentificationNumber = settings.taxIdentificationNumber,
                        paperSize = settings.paperSize ?: "58",
                        footer = settings.footer ?: "",
                        shopLogoBitmap = shopLogoBitmap,
                        promptPayType = PromptPayType.fromString(settings.promptPayType),
                        promptPayIdentifier = settings.promptPayIdentifier ?: "",
                        tinNumber = sanitizeTinInput(settings.tinNumber ?: "")
                    )
                    persistedSettings = PersistedReceiptSettings.from(tempState)
                    
                    // Clear pending logo operations after successful save
                    pendingShopLogoUri = null
                    removeShopLogo = false
                    
                    // Reset saving flag after a short delay to allow Flow to catch up
                    kotlinx.coroutines.delay(200)
                    isSavingInProgress = false
                } else {
                    isSavingInProgress = false
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "ไม่สามารถบันทึกการตั้งค่าได้"
                        )
                    }
                }
            } catch (e: Exception) {
                isSavingInProgress = false
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "เกิดข้อผิดพลาด: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun isValidForSaving(): Boolean {
        // Validate PromptPay if enabled
        if (tempState.showQRCode) {
            if (tempState.promptPayIdentifierError != null || tempState.promptPayIdentifier.trim().isEmpty()) {
                return false
            }
        }
        
        // Validate TIN if enabled
        if (tempState.taxIdentificationNumber) {
            if (tempState.tinNumberError != null || tempState.tinNumber.length != 13) {
                return false
            }
        }
        
        return true
    }
    
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
    
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Reload settings from database
     * Useful when returning to screen to ensure latest data is displayed
     */
    fun reloadSettings() {
        viewModelScope.launch {
            val settings = receiptSettingsRepository.getReceiptSettingsSync()
            if (settings != null) {
                val shopLogoBitmap = receiptSettingsRepository.getShopLogoBitmap()
                
                tempState = ReceiptSettingsUiState(
                    printShopLogo = settings.printShopLogo,
                    printAfterFinish = settings.printAfterFinish,
                    showQRCode = settings.showQRCode,
                    openCashDrawer = settings.openCashDrawer,
                    taxIdentificationNumber = settings.taxIdentificationNumber,
                    paperSize = settings.paperSize ?: "58",
                    footer = settings.footer ?: "",
                    shopLogoBitmap = shopLogoBitmap,
                    promptPayType = PromptPayType.fromString(settings.promptPayType),
                    promptPayIdentifier = settings.promptPayIdentifier ?: "",
                    tinNumber = sanitizeTinInput(settings.tinNumber ?: "")
                )
                persistedSettings = PersistedReceiptSettings.from(tempState)
                
                // Reset pending logo operations
                pendingShopLogoUri = null
                removeShopLogo = false
                
                _uiState.update {
                    it.copy(
                        printShopLogo = tempState.printShopLogo,
                        printAfterFinish = tempState.printAfterFinish,
                        showQRCode = tempState.showQRCode,
                        openCashDrawer = tempState.openCashDrawer,
                        taxIdentificationNumber = tempState.taxIdentificationNumber,
                        paperSize = tempState.paperSize,
                        footer = tempState.footer,
                        shopLogoBitmap = tempState.shopLogoBitmap,
                        promptPayType = tempState.promptPayType,
                        promptPayIdentifier = tempState.promptPayIdentifier,
                        tinNumber = tempState.tinNumber,
                        hasChanges = false
                    )
                }
            }
        }
    }

    /**
     * First validation message that blocks save (for user feedback).
     */
    private fun validationBlockReason(): String? {
        if (tempState.showQRCode) {
            val err = tempState.promptPayIdentifierError
            if (err != null) return err
            if (tempState.promptPayIdentifier.trim().isEmpty()) {
                return "กรุณากรอกข้อมูล PromptPay"
            }
        }
        if (tempState.taxIdentificationNumber) {
            tempState.tinNumberError?.let { return it }
            if (tempState.tinNumber.isEmpty()) {
                return getLocalizedString(R.string.settings_tin_required)
            }
            if (tempState.tinNumber.length != 13) {
                return getLocalizedString(R.string.settings_tin_error_must_be_13_digits)
            }
        }
        return null
    }

    private fun sanitizeTinInput(raw: String): String = raw.filter { it.isDigit() }.take(13)
}

/**
 * Values last matched to DB / disk; excludes transient UI-only fields.
 */
private data class PersistedReceiptSettings(
    val printShopLogo: Boolean = false,
    val printAfterFinish: Boolean = false,
    val showQRCode: Boolean = false,
    val openCashDrawer: Boolean = false,
    val taxIdentificationNumber: Boolean = false,
    val paperSize: String = "58",
    val footer: String = "",
    val promptPayType: PromptPayType = PromptPayType.PHONE_NUMBER,
    val promptPayIdentifier: String = "",
    val tinNumber: String = "",
    val shopLogoBitmap: Bitmap? = null
) {
    companion object {
        fun from(state: ReceiptSettingsUiState) = PersistedReceiptSettings(
            printShopLogo = state.printShopLogo,
            printAfterFinish = state.printAfterFinish,
            showQRCode = state.showQRCode,
            openCashDrawer = state.openCashDrawer,
            taxIdentificationNumber = state.taxIdentificationNumber,
            paperSize = state.paperSize,
            footer = state.footer,
            promptPayType = state.promptPayType,
            promptPayIdentifier = state.promptPayIdentifier,
            tinNumber = state.tinNumber,
            shopLogoBitmap = state.shopLogoBitmap
        )
    }
}

