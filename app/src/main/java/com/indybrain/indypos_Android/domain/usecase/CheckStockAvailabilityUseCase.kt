package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CheckStockAvailabilityUseCase @Inject constructor(
    private val repository: CartRepository
) {
    operator fun invoke(productId: String, requestedQuantity: Int): Flow<Boolean> = flow {
        val hasStock = repository.checkStockAvailability(productId, requestedQuantity)
        emit(hasStock)
    }
}

