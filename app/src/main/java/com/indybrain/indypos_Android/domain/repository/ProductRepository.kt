package com.indybrain.indypos_Android.domain.repository

/**
 * Repository interface for product-related operations
 */
interface ProductRepository {
    /**
     * Sync all product data from API and save to local database
     * @return Result indicating success or failure with error message
     */
    suspend fun syncAllProductData(): Result<Unit>
}

