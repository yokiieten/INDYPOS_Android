package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for product-related operations
 */
interface ProductRepository {
    /**
     * Sync all product data from API and save to local database
     * @return Result indicating success or failure with error message
     */
    suspend fun syncAllProductData(): Result<Unit>
    
    /**
     * Get all active products from local database
     */
    fun getAllActiveProducts(): Flow<List<ProductEntity>>
    
    /**
     * Get products by category from local database
     */
    fun getProductsByCategory(categoryId: String?): Flow<List<ProductEntity>>
    
    /**
     * Get all active categories from local database
     */
    fun getAllActiveCategories(): Flow<List<CategoryEntity>>
    
    /**
     * Fetch products from API and save to local database
     */
    suspend fun fetchAndSaveProducts(): Result<Unit>
}

