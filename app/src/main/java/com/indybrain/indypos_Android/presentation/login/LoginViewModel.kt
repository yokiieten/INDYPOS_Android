package com.indybrain.indypos_Android.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.LoginRequest
import com.indybrain.indypos_Android.domain.repository.AddonGroupRepository
import com.indybrain.indypos_Android.domain.repository.AddonRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import com.indybrain.indypos_Android.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Login screen implementing MVI pattern with StateFlow
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val productRepository: ProductRepository,
    private val addonGroupRepository: AddonGroupRepository,
    private val addonRepository: AddonRepository
) : ViewModel() {
    
    // UI State Flow
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    // MVI State Flow
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    /**
     * Handle intents/actions from UI
     */
    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> {
                _uiState.update { it.copy(email = intent.email, errorMessage = null) }
            }
            
            is LoginIntent.UpdatePassword -> {
                _uiState.update { it.copy(password = intent.password, errorMessage = null) }
            }
            
            is LoginIntent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            
            is LoginIntent.Login -> {
                performLogin()
            }
            
            is LoginIntent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
                _state.value = LoginState.Idle
            }

            is LoginIntent.AcknowledgeSuccess -> {
                _uiState.update {
                    it.copy(
                        isLoginSuccess = false,
                        successMessage = null
                    )
                }
                _state.value = LoginState.Idle
            }
            
            is LoginIntent.ShowUnauthorizedError -> {
                // Show unauthorized error message (session expired)
                _uiState.update {
                    it.copy(
                        errorMessage = "เซสชันหมดอายุ กรุณาเข้าสู่ระบบอีกครั้ง"
                    )
                }
                _state.value = LoginState.Error("เซสชันหมดอายุ กรุณาเข้าสู่ระบบอีกครั้ง")
            }
        }
    }
    
    /**
     * Perform login operation
     */
    private fun performLogin() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { 
                it.copy(
                    errorMessage = "Please fill in all fields",
                    isLoading = false
                )
            }
            _state.value = LoginState.Error("Please fill in all fields")
            return
        }
        
        // Update state to loading
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        _state.value = LoginState.Loading
        
        viewModelScope.launch {
            try {
                val request = LoginRequest(email = email, password = password)
                loginUseCase(request)
                    .onSuccess { user ->
                        // Fetch all data after successful login - wait for all 4 APIs to succeed
                        fetchAllDataAfterLogin { errorMessage ->
                            if (errorMessage != null) {
                                // Some data fetching failed - show error and don't navigate
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoginSuccess = false, // Don't navigate if error
                                        successMessage = null,
                                        user = user,
                                        errorMessage = errorMessage
                                    )
                                }
                                _state.value = LoginState.Error(errorMessage)
                            } else {
                                // All 4 APIs fetched successfully - now navigate to home
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoginSuccess = true, // Navigate only when all succeed
                                        successMessage = null,
                                        user = user,
                                        errorMessage = null
                                    )
                                }
                                _state.value = LoginState.Success(user)
                            }
                        }
                    }
                    .onFailure { exception ->
                        val errorMessage = when {
                            exception.message != null -> exception.message!!
                            exception.cause?.message != null -> exception.cause!!.message!!
                            else -> "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง"
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoginSuccess = false,
                                successMessage = null,
                                errorMessage = errorMessage
                            )
                        }
                        _state.value = LoginState.Error(errorMessage)
                    }
            } catch (e: Exception) {
                // Catch any unexpected exceptions to prevent app crash
                val errorMessage = when {
                    e.message != null -> e.message!!
                    e.cause?.message != null -> e.cause!!.message!!
                    else -> "เกิดข้อผิดพลาดที่ไม่คาดคิด กรุณาลองใหม่อีกครั้ง"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoginSuccess = false,
                        successMessage = null,
                        errorMessage = errorMessage
                    )
                }
                _state.value = LoginState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Fetch all data after login (Categories, Products, Addon Groups, Addons)
     * Waits for all 4 APIs to succeed before allowing navigation to home
     */
    private suspend fun fetchAllDataAfterLogin(completion: (errorMessage: String?) -> Unit) {
        println("🔄 Starting to fetch all data after login...")
        
        val errorMessages = mutableListOf<String>()
        
        // Fetch all APIs in parallel using async/awaitAll within coroutineScope
        coroutineScope {
            val categoriesDeferred = async {
                try {
                    val result = productRepository.fetchAndSyncCategories()
                    if (result.isSuccess) {
                        println("✅ Categories fetched successfully")
                        null // no error
                    } else {
                        val error = "ไม่สามารถโหลดหมวดหมู่ได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        println("❌ Failed to fetch categories: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลดหมวดหมู่ได้: ${e.message ?: "Unknown error"}"
                    println("❌ Failed to fetch categories: $error")
                    error
                }
            }
            
            val productsDeferred = async {
                try {
                    val result = productRepository.fetchAndSaveProducts()
                    if (result.isSuccess) {
                        println("✅ Products fetched successfully")
                        null // no error
                    } else {
                        val error = "ไม่สามารถโหลดสินค้าได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        println("❌ Failed to fetch products: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลดสินค้าได้: ${e.message ?: "Unknown error"}"
                    println("❌ Failed to fetch products: $error")
                    error
                }
            }
            
            val addonGroupsDeferred = async {
                try {
                    val result = addonGroupRepository.fetchAndSyncAddonGroups()
                    if (result.isSuccess) {
                        println("✅ Addon Groups fetched successfully")
                        null // no error
                    } else {
                        val error = "ไม่สามารถโหลด AddOn Groups ได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        println("❌ Failed to fetch addon groups: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลด AddOn Groups ได้: ${e.message ?: "Unknown error"}"
                    println("❌ Failed to fetch addon groups: $error")
                    error
                }
            }
            
            val addonsDeferred = async {
                try {
                    val result = addonRepository.fetchAndSyncAddons()
                    if (result.isSuccess) {
                        println("✅ Addons fetched successfully")
                        null // no error
                    } else {
                        val error = "ไม่สามารถโหลด AddOns ได้: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                        println("❌ Failed to fetch addons: $error")
                        error
                    }
                } catch (e: Exception) {
                    val error = "ไม่สามารถโหลด AddOns ได้: ${e.message ?: "Unknown error"}"
                    println("❌ Failed to fetch addons: $error")
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
            
            // Collect all error messages
            results.forEach { error ->
                if (error != null) {
                    errorMessages.add(error)
                }
            }
        }
        
        if (errorMessages.isNotEmpty()) {
            val combinedError = "ไม่สามารถโหลดข้อมูลบางส่วนได้:\n${errorMessages.joinToString("\n")}\n\nกรุณาลองใหม่อีกครั้ง"
            println("❌ Some data fetching failed: $combinedError")
            completion(combinedError)
        } else {
            println("✅ All 4 APIs fetched successfully - ready to navigate to home")
            completion(null) // All succeeded
        }
    }
}

