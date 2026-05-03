package com.indybrain.indypos_Android.presentation.productedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.usecase.*
import kotlinx.coroutines.flow.catch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    private val getGroupedCartItemsByProductUseCase: GetGroupedCartItemsByProductUseCase,
    private val increaseQuantityUseCase: IncreaseCartItemQuantityUseCase,
    private val decreaseQuantityUseCase: DecreaseCartItemQuantityUseCase,
    private val deleteCartItemGroupUseCase: DeleteCartItemGroupUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductEditUiState())
    val uiState: StateFlow<ProductEditUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<ProductEditEvent>()
    val events: SharedFlow<ProductEditEvent> = _events.asSharedFlow()
    
    fun init(productId: String) {
        viewModelScope.launch {
            getGroupedCartItemsByProductUseCase(productId)
                .catch { e ->
                    _events.emit(ProductEditEvent.ShowError(e.message ?: "Unknown error"))
                }
                .collect { groupedItems ->
                    _uiState.update { it.copy(groupedItems = groupedItems) }
                }
        }
    }
    
    fun increaseQuantity(cartItemId: String) {
        viewModelScope.launch {
            increaseQuantityUseCase(cartItemId)
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            // Success - UI will auto-update via Flow
                            // Clear any previous error
                            _uiState.update { it.copy(errorMessage = null) }
                        },
                        onFailure = { e ->
                            val errorMessage = if (e is InsufficientStockException) {
                                e.message?.takeIf { it.isNotBlank() }
                                    ?: CartInsufficientStockErrors.TOKEN_PLAIN
                            } else {
                                e.message ?: "ไม่สามารถอัปเดตจำนวนได้"
                            }
                            _uiState.update { it.copy(errorMessage = errorMessage) }
                            // Clear error message after 3 seconds
                            launch {
                                kotlinx.coroutines.delay(3000)
                                _uiState.update { it.copy(errorMessage = null) }
                            }
                        }
                    )
                }
        }
    }
    
    fun decreaseQuantity(cartItemId: String) {
        viewModelScope.launch {
            decreaseQuantityUseCase(cartItemId)
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            // Success - UI will auto-update via Flow
                        },
                        onFailure = { e ->
                            _events.emit(
                                ProductEditEvent.ShowError(
                                    e.message ?: "Failed to update quantity"
                                )
                            )
                        }
                    )
                }
        }
    }
    
    fun deleteItemGroup(cartItemIds: List<String>) {
        viewModelScope.launch {
            deleteCartItemGroupUseCase(cartItemIds)
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            _events.emit(ProductEditEvent.ItemDeleted)
                        },
                        onFailure = { e ->
                            _events.emit(
                                ProductEditEvent.ShowError(
                                    e.message ?: "Failed to delete item"
                                )
                            )
                        }
                    )
                }
        }
    }
    
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

