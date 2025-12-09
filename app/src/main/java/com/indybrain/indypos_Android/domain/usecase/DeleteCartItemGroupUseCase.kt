package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DeleteCartItemGroupUseCase @Inject constructor(
    private val repository: CartRepository
) {
    operator fun invoke(cartItemIds: List<String>): Flow<Result<Unit>> = flow {
        val result = repository.deleteCartItems(cartItemIds)
        emit(result)
    }
}

