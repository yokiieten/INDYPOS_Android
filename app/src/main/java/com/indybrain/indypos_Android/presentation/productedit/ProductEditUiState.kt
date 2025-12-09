package com.indybrain.indypos_Android.presentation.productedit

import com.indybrain.indypos_Android.domain.model.GroupedCartItem

data class ProductEditUiState(
    val groupedItems: List<GroupedCartItem> = emptyList(),
    val isLoading: Boolean = false
)

sealed class ProductEditEvent {
    data class ShowError(val message: String) : ProductEditEvent()
    object ItemDeleted : ProductEditEvent()
    object CartUpdated : ProductEditEvent()
}

