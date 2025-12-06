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
    
    /**
     * Fetch categories from API and save to local database
     * Compares with existing Room data and only adds new categories
     */
    suspend fun fetchAndSyncCategories(): Result<Unit>
    
    /**
     * Get all categories from local database (including inactive)
     */
    suspend fun getAllCategories(): List<CategoryEntity>
    
    /**
     * Get all categories from local database as Flow (including inactive)
     */
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>
    
    /**
     * Get category by ID
     */
    suspend fun getCategoryById(id: String): CategoryEntity?
    
    /**
     * Add a new category to local database
     */
    suspend fun addCategory(category: CategoryEntity): Result<Unit>
    
    /**
     * Update an existing category in local database
     */
    suspend fun updateCategory(category: CategoryEntity): Result<Unit>
    
    /**
     * Get current user ID
     */
    suspend fun getCurrentUserId(): Int?
    
    /**
     * Create a new category
     * If network is available, calls API and saves to Room
     * If network is not available, saves to Room only (for sync later)
     */
    suspend fun createCategory(
        name: String,
        sortOrder: Int,
        isActive: Boolean = true
    ): Result<CategoryEntity>
    
    /**
     * Update an existing category
     * If network is available, calls API and saves to Room
     * If network is not available, saves to Room only (for sync later)
     */
    suspend fun updateCategory(
        categoryId: String,
        name: String,
        sortOrder: Int,
        isActive: Boolean
    ): Result<CategoryEntity>
    
    /**
     * Toggle category status (activate/deactivate)
     * If network is available, calls API and saves to Room
     * If network is not available, updates in Room only (for sync later)
     */
    suspend fun toggleCategoryStatus(
        categoryId: String,
        newStatus: Boolean
    ): Result<CategoryEntity>
}

