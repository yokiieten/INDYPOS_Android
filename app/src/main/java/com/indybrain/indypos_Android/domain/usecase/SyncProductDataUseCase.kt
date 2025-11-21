package com.indybrain.indypos_Android.domain.usecase

import com.indybrain.indypos_Android.domain.repository.ProductRepository
import javax.inject.Inject

/**
 * Use case to sync all product data from API to local database
 */
class SyncProductDataUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return productRepository.syncAllProductData()
    }
}

