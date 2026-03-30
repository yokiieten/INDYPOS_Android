package com.indybrain.indypos_Android.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.CartRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    val cartItemCount = cartRepository.getCartItemCount()
    val cartItems = cartRepository.getCartItems()

    private val _uiState = MutableStateFlow(SearchProductUiState())
    val uiState: StateFlow<SearchProductUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, errorMessage = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            fetchPage(query = query.trim(), page = 1, append = false)
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isLoadingMore || !s.hasNext) return
        val q = s.searchQuery.trim()
        viewModelScope.launch {
            fetchPage(query = q, page = s.currentPage + 1, append = true)
        }
    }

    private suspend fun fetchPage(query: String, page: Int, append: Boolean) {
        if (append) {
            _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        } else {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        val result = productRepository.searchProductsPaginated(
            query = query,
            categoryId = null,
            page = page,
            limit = PAGE_LIMIT
        )
        result.fold(
            onSuccess = { res ->
                _uiState.update { current ->
                    val latestQ = current.searchQuery.trim()
                    if (latestQ != query) {
                        current.copy(isLoading = false, isLoadingMore = false)
                    } else {
                        val merged = if (append) current.products + res.products else res.products
                        current.copy(
                            products = merged,
                            currentPage = res.currentPage,
                            hasNext = res.hasNext,
                            isLoading = false,
                            isLoadingMore = false,
                            errorMessage = null
                        )
                    }
                }
            },
            onFailure = { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = e.message,
                        products = if (append) it.products else emptyList()
                    )
                }
            }
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        const val PAGE_LIMIT = 20
    }
}
