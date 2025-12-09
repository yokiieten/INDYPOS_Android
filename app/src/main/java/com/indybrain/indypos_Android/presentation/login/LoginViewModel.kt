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
                        // Fetch all data after successful login
                        fetchAllDataAfterLogin { hasError ->
                            if (hasError) {
                                // Some data fetching failed, but continue with login
                                val errorMessage = "เกิดข้อผิดพลาดในการโหลดข้อมูลบางส่วน แต่สามารถเข้าสู่ระบบได้"
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoginSuccess = true,
                                        successMessage = null,
                                        user = user,
                                        errorMessage = errorMessage
                                    )
                                }
                                _state.value = LoginState.Success(user)
                            } else {
                                // All data fetched successfully
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoginSuccess = true,
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
     * Similar to DispatchGroup pattern in iOS - waits for all requests to complete
     */
    private suspend fun fetchAllDataAfterLogin(completion: (hasError: Boolean) -> Unit) {
        println("🔄 Starting to fetch all data after login...")
        
        var hasError = false
        
        // Fetch all APIs in parallel using async/awaitAll within coroutineScope
        coroutineScope {
            val categoriesDeferred = async {
                try {
                    val result = productRepository.fetchAndSyncCategories()
                    if (result.isSuccess) {
                        println("✅ Categories fetched successfully")
                        false // no error
                    } else {
                        println("❌ Failed to fetch categories: ${result.exceptionOrNull()?.message}")
                        true // has error
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch categories: ${e.message}")
                    true // has error
                }
            }
            
            val productsDeferred = async {
                try {
                    val result = productRepository.fetchAndSaveProducts()
                    if (result.isSuccess) {
                        println("✅ Products fetched successfully")
                        false // no error
                    } else {
                        println("❌ Failed to fetch products: ${result.exceptionOrNull()?.message}")
                        true // has error
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch products: ${e.message}")
                    true // has error
                }
            }
            
            val addonGroupsDeferred = async {
                try {
                    val result = addonGroupRepository.fetchAndSyncAddonGroups()
                    if (result.isSuccess) {
                        println("✅ Addon Groups fetched successfully")
                        false // no error
                    } else {
                        println("❌ Failed to fetch addon groups: ${result.exceptionOrNull()?.message}")
                        true // has error
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch addon groups: ${e.message}")
                    true // has error
                }
            }
            
            val addonsDeferred = async {
                try {
                    val result = addonRepository.fetchAndSyncAddons()
                    if (result.isSuccess) {
                        println("✅ Addons fetched successfully")
                        false // no error
                    } else {
                        println("❌ Failed to fetch addons: ${result.exceptionOrNull()?.message}")
                        true // has error
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch addons: ${e.message}")
                    true // has error
                }
            }
            
            // Wait for all requests to complete
            val results = awaitAll(
                categoriesDeferred,
                productsDeferred,
                addonGroupsDeferred,
                addonsDeferred
            )
            
            // Check if any request failed
            hasError = results.any { it }
        }
        
        if (hasError) {
            println("⚠️ Some data fetching failed, but continuing with login...")
        } else {
            println("✅ All data fetched successfully")
        }
        
        completion(hasError)
    }
}

