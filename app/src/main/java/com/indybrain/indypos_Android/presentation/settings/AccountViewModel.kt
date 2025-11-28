package com.indybrain.indypos_Android.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.update { current ->
                    val name = buildString {
                        val first = user?.firstName.orEmpty()
                        val last = user?.lastName.orEmpty()
                        when {
                            first.isNotBlank() && last.isNotBlank() -> append("$first $last")
                            first.isNotBlank() -> append(first)
                            last.isNotBlank() -> append(last)
                            !user?.username.isNullOrBlank() -> append(user?.username)
                            else -> append("User")
                        }
                    }
                    current.copy(
                        displayName = name,
                        email = user?.email ?: "",
                        packageName = user?.subscriptionPlan ?: "Basic"
                    )
                }
            }
        }
    }
}

data class AccountUiState(
    val displayName: String = "",
    val email: String = "",
    val packageName: String = "Basic"
)


