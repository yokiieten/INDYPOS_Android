package com.indybrain.indypos_Android.domain.repository

import com.indybrain.indypos_Android.data.local.entity.AddonEntity
import com.indybrain.indypos_Android.data.local.entity.AddonGroupEntity
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
    
    /**
     * Delete a category
     * If network is available, calls API and deletes from Room
     * If network is not available, marks as deleted locally (isDeletedLocally = true)
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    
    /**
     * Delete multiple categories
     * Returns DeleteCategoriesResult with deleted count, failed count, and error messages
     * (for partial success when some categories could not be deleted)
     */
    suspend fun deleteMultipleCategories(categoryIds: List<String>): Result<DeleteCategoriesResult>
    
    /**
     * Get all products for management (including inactive, excluding deleted)
     */
    fun getAllProductsForManagement(): Flow<List<ProductEntity>>
    
    /**
     * Search products by name and category
     */
    fun searchProducts(query: String, categoryId: String?): Flow<List<ProductEntity>>
    
    /**
     * Get product by ID
     */
    suspend fun getProductById(id: String): ProductEntity?

    /**
     * Fetch product detail from API (product + addon groups + addons).
     * Returns null/error when offline or API fails.
     */
    suspend fun getProductDetailFromApi(productId: String): Result<ProductDetailData>

    /**
     * Fetch product list from API (categories + products for Main Product Screen).
     * Returns error when offline or API fails.
     */
    suspend fun getProductListFromApi(categoryId: String? = null): Result<ProductListData>

    /**
     * Ensure product (and its category if needed) exists in Room before adding to cart.
     * Inserts only if not already present. Call before addToCart when product may come from API.
     */
    suspend fun ensureProductExists(product: ProductEntity, category: CategoryEntity? = null)
    
    /**
     * Get product by barcode (productCode or skuCode)
     */
    suspend fun getProductByCode(code: String): ProductEntity?
    
    /**
     * Delete a product
     * If network is available, calls API and permanently deletes from Room
     * If network is not available and product is not synced, permanently deletes
     * Otherwise, marks as deleted locally (isDeletedLocally = true)
     */
    suspend fun deleteProduct(productId: String): Result<Unit>
    
    /**
     * Delete multiple products
     * Returns DeleteProductsResult with deleted count, failed count, and error messages
     * (for partial success when some products could not be deleted)
     */
    suspend fun deleteMultipleProducts(productIds: List<String>): Result<DeleteProductsResult>
    
    /**
     * Toggle product status (activate/deactivate)
     * If network is available, calls API and updates Room
     * If network is not available, updates in Room only (for sync later)
     */
    suspend fun toggleProductStatus(productId: String, newStatus: Boolean): Result<ProductEntity>
    
    /**
     * Upload product image
     * @param imageUri URI of the image file to upload
     * @return Result containing the image URL from server
     */
    suspend fun uploadProductImage(imageUri: android.net.Uri): Result<String>
    
    /**
     * Create a new product
     * If network is available, calls API and saves to Room
     * If network is not available, saves to Room only (isFromServer = false, isSynced = false)
     */
    suspend fun createProduct(
        name: String,
        productCode: String?,
        price: Double,
        costPrice: Double? = null,
        unit: String? = null,
        imageUrl: String? = null,
        selectedColorHex: String? = null,
        categoryId: String? = null,
        skuCode: String? = null,
        stockQuantity: Int? = null,
        isSkuEnabled: Boolean? = null,
        isStockEnabled: Boolean? = null,
        hasAdditionalOptions: Boolean? = null,
        addonGroupIds: List<String>? = null
    ): Result<ProductEntity>
    
    /**
     * Update an existing product
     * If network is available, calls API and saves to Room
     * If network is not available, updates in Room only (isSynced = false)
     */
    suspend fun updateProduct(
        productId: String,
        name: String,
        productCode: String?,
        price: Double,
        costPrice: Double? = null,
        unit: String? = null,
        imageUrl: String? = null,
        selectedColorHex: String? = null,
        categoryId: String? = null,
        skuCode: String? = null,
        stockQuantity: Int? = null,
        isSkuEnabled: Boolean? = null,
        isStockEnabled: Boolean? = null,
        hasAdditionalOptions: Boolean? = null,
        addonGroupIds: List<String>? = null
    ): Result<ProductEntity>
    
    /**
     * Get sync statistics
     */
    suspend fun getSyncStatistics(): ProductSyncStatistics
    
    /**
     * Sync categories to server
     * Syncs unsynced and deleted categories from local database to server
     */
    suspend fun syncCategories(): Result<Unit>
    
    /**
     * Sync products to server
     * Syncs unsynced and deleted products from local database to server
     */
    suspend fun syncProducts(): Result<Unit>
    
    /**
     * Clear cart items for a product (when product is deactivated)
     */
    suspend fun clearCartItemsByProduct(productId: String)
    
    /**
     * Update product stock quantity (delta update)
     * If network is available, calls API and updates Room
     * If network is not available, updates in Room only (isSynced = false)
     */
    suspend fun updateProductStock(productId: String, delta: Int): Result<ProductEntity>
    
    /**
     * Clear all products, categories, addon groups, addons and their junctions from Room database
     * This is used before fetching fresh data from API
     */
    suspend fun clearAllProductsAndCategories()
}

/**
 * Result of batch delete products operation
 * Supports partial success (some deleted, some failed)
 */
data class DeleteProductsResult(
    val deletedCount: Int,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList()
)

/**
 * Result of batch delete categories operation
 */
data class DeleteCategoriesResult(
    val deletedCount: Int,
    val failedCount: Int = 0,
    val errors: List<String> = emptyList()
)

/**
 * Product list data from API (categories + products for Main Product Screen)
 */
data class ProductListData(
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>
)

/**
 * Product detail data from API (product + addon groups + addons by group)
 */
data class ProductDetailData(
    val product: ProductEntity,
    val addonGroups: List<AddonGroupEntity>,
    val addonsByGroup: Map<String, List<AddonEntity>>,
    val category: CategoryEntity? = null
)

/**
 * Product sync statistics
 */
data class ProductSyncStatistics(
    val totalProducts: Int,
    val syncedCount: Int,
    val pendingSyncCount: Int,
    val deletedCount: Int
)

