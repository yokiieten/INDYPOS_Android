package com.indybrain.indypos_Android.presentation.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.BuildConfig
import com.indybrain.indypos_Android.domain.model.RegisterRequest
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for Register screen implementing MVI pattern with StateFlow
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val productRepository: ProductRepository,
    private val addonGroupRepository: AddonGroupRepository,
    private val addonRepository: AddonRepository
) : ViewModel() {
    
    // UI State Flow
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    // MVI State Flow
    private val _state = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val state: StateFlow<RegisterState> = _state.asStateFlow()
    
    /**
     * Handle intents/actions from UI
     */
    fun handleIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateUsername -> {
                _uiState.update { it.copy(username = intent.username, errorMessage = null) }
            }
            is RegisterIntent.UpdatePassword -> {
                _uiState.update { it.copy(password = intent.password, errorMessage = null) }
            }
            is RegisterIntent.UpdateConfirmPassword -> {
                _uiState.update { it.copy(confirmPassword = intent.confirmPassword, errorMessage = null) }
            }
            is RegisterIntent.UpdateFirstName -> {
                _uiState.update { it.copy(firstName = intent.firstName, errorMessage = null) }
            }
            is RegisterIntent.UpdateLastName -> {
                _uiState.update { it.copy(lastName = intent.lastName, errorMessage = null) }
            }
            is RegisterIntent.UpdateEmail -> {
                _uiState.update { it.copy(email = intent.email, errorMessage = null) }
            }
            is RegisterIntent.UpdatePhone -> {
                _uiState.update { it.copy(phone = intent.phone, errorMessage = null) }
            }
            is RegisterIntent.UpdateGender -> {
                _uiState.update { it.copy(gender = intent.gender, errorMessage = null) }
            }
            is RegisterIntent.UpdateBirthDate -> {
                _uiState.update { it.copy(birthDate = intent.birthDate, errorMessage = null) }
            }
            is RegisterIntent.UpdateShopName -> {
                _uiState.update { it.copy(shopName = intent.shopName, errorMessage = null) }
            }
            is RegisterIntent.UpdateShopDescription -> {
                _uiState.update { it.copy(shopDescription = intent.shopDescription, errorMessage = null) }
            }
            is RegisterIntent.UpdateTermsAccepted -> {
                _uiState.update { it.copy(termsAccepted = intent.accepted, errorMessage = null) }
            }
            is RegisterIntent.UpdatePrivacyAccepted -> {
                _uiState.update { it.copy(privacyAccepted = intent.accepted, errorMessage = null) }
            }
            is RegisterIntent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is RegisterIntent.ToggleConfirmPasswordVisibility -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }
            is RegisterIntent.Register -> {
                performRegister()
            }
            is RegisterIntent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
                _state.value = RegisterState.Idle
            }
        }
    }
    
    /**
     * Perform registration operation
     */
    private fun performRegister() {
        val validation = validateInput()
        if (!validation.isValid) {
            _uiState.update { 
                it.copy(
                    errorMessage = validation.message,
                    isLoading = false
                )
            }
            _state.value = RegisterState.Error(validation.message ?: "Invalid data")
            return
        }
        
        // Update state to loading
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        _state.value = RegisterState.Loading
        
        viewModelScope.launch {
            try {
                val request = RegisterRequest(
                    username = _uiState.value.username.trim(),
                    firstName = _uiState.value.firstName.trim(),
                    lastName = _uiState.value.lastName.trim(),
                    email = _uiState.value.email.trim(),
                    phone = _uiState.value.phone.trim(),
                    password = _uiState.value.password,
                    shopName = _uiState.value.shopName.trim(),
                    shopDescription = _uiState.value.shopDescription.takeIf { it.isNotBlank() },
                    shopImageUrl = null,
                    gender = _uiState.value.gender.takeIf { it.isNotBlank() },
                    birthDate = _uiState.value.birthDate.takeIf { it.isNotBlank() },
                    termOfUse = _uiState.value.termsAccepted,
                    privacyPolicy = _uiState.value.privacyAccepted
                )
                
                registerUseCase(request)
                    .onSuccess { user ->
                        // Fetch all data after successful registration - wait for all 4 APIs to succeed
                        fetchAllDataAfterRegistration { errorMessage ->
                            if (errorMessage != null) {
                                // Some data fetching failed - show error and don't navigate
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isRegistrationSuccess = false,
                                        successMessage = null,
                                        user = user,
                                        errorMessage = errorMessage
                                    )
                                }
                                _state.value = RegisterState.Error(errorMessage)
                            } else {
                                // All 4 APIs fetched successfully - now navigate to home
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isRegistrationSuccess = true,
                                        successMessage = null,
                                        user = user,
                                        errorMessage = null
                                    )
                                }
                                _state.value = RegisterState.Success(user)
                            }
                        }
                    }
                    .onFailure { exception ->
                        // Repository already provides localized error messages
                        // Use exception.message which contains the localized error from repository
                        val errorMessage = exception.message 
                            ?: exception.cause?.message 
                            ?: "เกิดข้อผิดพลาดในการสมัครสมาชิก"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRegistrationSuccess = false,
                                successMessage = null,
                                errorMessage = errorMessage
                            )
                        }
                        _state.value = RegisterState.Error(errorMessage)
                    }
            } catch (e: Exception) {
                // Handle unexpected exceptions (shouldn't happen if repository handles properly)
                val errorMessage = e.message 
                    ?: e.cause?.message 
                    ?: "เกิดข้อผิดพลาดในการสมัครสมาชิก"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRegistrationSuccess = false,
                        successMessage = null,
                        errorMessage = errorMessage
                    )
                }
                _state.value = RegisterState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Fetch all data after registration (Categories, Products, Addon Groups, Addons)
     * Waits for all 4 APIs to succeed before allowing navigation to home
     */
    private suspend fun fetchAllDataAfterRegistration(completion: (errorMessage: String?) -> Unit) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Starting to fetch all data after registration...")

        val errorMessages = mutableListOf<String>()

        coroutineScope {
            val categoriesDeferred = async {
                try {
                    val result = productRepository.fetchAndSyncCategories()
                    if (result.isSuccess) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Categories fetched successfully")
                        null
                    } else {
                        val error = "ไม่สามารถโหลดหมวดหมู่ได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch categories: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลดหมวดหมู่ได้: ${e.message ?: "Unknown error"}"
                    if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch categories: $error")
                    error
                }
            }

            val productsDeferred = async {
                try {
                    val result = productRepository.fetchAndSaveProducts()
                    if (result.isSuccess) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Products fetched successfully")
                        null
                    } else {
                        val error = "ไม่สามารถโหลดสินค้าได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch products: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลดสินค้าได้: ${e.message ?: "Unknown error"}"
                    if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch products: $error")
                    error
                }
            }

            val addonGroupsDeferred = async {
                try {
                    val result = addonGroupRepository.fetchAndSyncAddonGroups()
                    if (result.isSuccess) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Addon Groups fetched successfully")
                        null
                    } else {
                        val error = "ไม่สามารถโหลด AddOn Groups ได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch addon groups: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลด AddOn Groups ได้: ${e.message ?: "Unknown error"}"
                    if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch addon groups: $error")
                    error
                }
            }

            val addonsDeferred = async {
                try {
                    val result = addonRepository.fetchAndSyncAddons()
                    if (result.isSuccess) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Addons fetched successfully")
                        null
                    } else {
                        val error = "ไม่สามารถโหลด AddOns ได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch addons: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลด AddOns ได้: ${e.message ?: "Unknown error"}"
                    if (BuildConfig.DEBUG) Log.d(TAG, "Failed to fetch addons: $error")
                    error
                }
            }

            // Wait for all requests to complete
            val results = awaitAll(
                categoriesDeferred,
                productsDeferred,
                addonGroupsDeferred,
                addonsDeferred
            )

            results.forEach { error ->
                if (error != null) errorMessages.add(error)
            }
        }

        if (errorMessages.isNotEmpty()) {
            val combinedError = "ไม่สามารถโหลดข้อมูลบางส่วนได้:\n${errorMessages.joinToString("\n")}\n\nกรุณาลองใหม่อีกครั้ง"
            if (BuildConfig.DEBUG) Log.d(TAG, "Some data fetching failed: $combinedError")
            completion(combinedError)
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "All 4 APIs fetched successfully - ready to navigate to home")
            completion(null)
        }
    }
    
    /**
     * Validate input fields
     */
    fun validateInput(): ValidationResult {
        val state = _uiState.value
        
        // Validate username
        val trimmedUsername = state.username.trim()
        if (trimmedUsername.isEmpty()) {
            return ValidationResult(false, "register_validation_username_required")
        }
        if (trimmedUsername.length < 3) {
            return ValidationResult(false, "register_validation_username_too_short")
        }
        if (trimmedUsername.length > 50) {
            return ValidationResult(false, "register_validation_username_too_long")
        }
        
        // Validate password
        val trimmedPassword = state.password.trim()
        if (trimmedPassword.isEmpty()) {
            return ValidationResult(false, "register_validation_password_required")
        }
        if (trimmedPassword.length < 6) {
            return ValidationResult(false, "register_validation_password_too_short")
        }
        if (trimmedPassword.length > 50) {
            return ValidationResult(false, "register_validation_password_too_long")
        }
        
        // Validate confirm password
        val trimmedConfirmPassword = state.confirmPassword.trim()
        if (trimmedConfirmPassword.isEmpty()) {
            return ValidationResult(false, "register_validation_confirm_password_required")
        }
        if (trimmedConfirmPassword != trimmedPassword) {
            return ValidationResult(false, "register_validation_passwords_not_match")
        }
        
        // Validate first name
        val trimmedFirstName = state.firstName.trim()
        if (trimmedFirstName.isEmpty()) {
            return ValidationResult(false, "register_validation_first_name_required")
        }
        if (trimmedFirstName.length < 2) {
            return ValidationResult(false, "register_validation_first_name_too_short")
        }
        
        // Validate last name
        val trimmedLastName = state.lastName.trim()
        if (trimmedLastName.isEmpty()) {
            return ValidationResult(false, "register_validation_last_name_required")
        }
        
        // Validate email
        val trimmedEmail = state.email.trim()
        if (trimmedEmail.isEmpty()) {
            return ValidationResult(false, "register_validation_email_required")
        }
        if (!isValidEmail(trimmedEmail)) {
            return ValidationResult(false, "register_validation_email_invalid")
        }
        
        // Validate phone
        val trimmedPhone = state.phone.trim()
        if (trimmedPhone.isEmpty()) {
            return ValidationResult(false, "register_validation_phone_required")
        }
        if (!isValidPhone(trimmedPhone)) {
            return ValidationResult(false, "register_validation_phone_invalid")
        }
        
        // Validate birth date
        val trimmedBirthDate = state.birthDate.trim()
        if (trimmedBirthDate.isEmpty()) {
            return ValidationResult(false, "register_validation_birth_date_required")
        }
        if (!isAtLeastSixteenYearsOld(trimmedBirthDate)) {
            return ValidationResult(false, "register_validation_birth_date_under_16")
        }
        
        // Validate gender
        if (state.gender.isEmpty()) {
            return ValidationResult(false, "register_validation_gender_required")
        }
        
        // Validate shop name
        val trimmedShopName = state.shopName.trim()
        if (trimmedShopName.isEmpty()) {
            return ValidationResult(false, "register_validation_shop_name_required")
        }
        if (trimmedShopName.length < 2) {
            return ValidationResult(false, "register_validation_shop_name_too_short")
        }
        if (trimmedShopName.length > 100) {
            return ValidationResult(false, "register_validation_shop_name_too_long")
        }
        
        // Validate shop description (if provided)
        val trimmedDescription = state.shopDescription.trim()
        if (trimmedDescription.isNotEmpty()) {
            if (trimmedDescription.length < 5) {
                return ValidationResult(false, "register_validation_description_too_short")
            }
            if (trimmedDescription.length > 500) {
                return ValidationResult(false, "register_validation_description_too_long")
            }
        }
        
        // Validate terms acceptance
        if (!state.termsAccepted) {
            return ValidationResult(false, "register_validation_terms_required")
        }
        
        // Validate privacy acceptance
        if (!state.privacyAccepted) {
            return ValidationResult(false, "register_validation_privacy_required")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validation helper functions
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return emailRegex.toRegex().matches(email)
    }
    
    private fun isValidPhone(phone: String): Boolean {
        // Remove dashes for validation
        val digitsOnly = phone.replace("-", "")
        // Check if all digits and exactly 10 digits
        return digitsOnly.all { it.isDigit() } && digitsOnly.length == 10
    }
    
    private fun isAtLeastSixteenYearsOld(isoDate: String): Boolean {
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            formatter.isLenient = false
            val birthDate = formatter.parse(isoDate) ?: return false
            val calendar = Calendar.getInstance()
            val today = calendar.time
            
            calendar.time = birthDate
            calendar.add(Calendar.YEAR, 16)
            val sixteenYearsAgo = calendar.time
            
            today >= sixteenYearsAgo
        } catch (e: Exception) {
            false
        }
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String? // String resource key
    )

    companion object {
        private const val TAG = "RegisterViewModel"
    }
}

