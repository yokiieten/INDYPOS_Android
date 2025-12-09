package com.indybrain.indypos_Android.presentation.productedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    private val getGroupedCartItemsUseCase: GetGroupedCartItemsByProductUseCase,
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
            getGroupedCartItemsUseCase(productId)
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
                        },
                        onFailure = { e ->
                            _events.emit(
                                ProductEditEvent.ShowError(
                                    if (e is InsufficientStockException) 
                                        "Insufficient stock" 
                                    else 
                                        e.message ?: "Failed to update quantity"
                                )
                            )
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
}

