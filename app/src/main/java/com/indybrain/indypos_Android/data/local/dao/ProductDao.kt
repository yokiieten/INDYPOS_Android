package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)
    
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY popularityRank ASC")
    suspend fun getAllActiveProducts(): List<ProductEntity>
    
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY popularityRank ASC")
    fun getAllActiveProductsFlow(): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1 ORDER BY popularityRank ASC")
    suspend fun getProductsByCategory(categoryId: String?): List<ProductEntity>
    
    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1 ORDER BY popularityRank ASC")
    fun getProductsByCategoryFlow(categoryId: String?): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE categoryId IS NULL AND isActive = 1 ORDER BY popularityRank ASC")
    suspend fun getProductsWithoutCategory(): List<ProductEntity>
    
    @Query("SELECT * FROM products ORDER BY popularityRank ASC")
    suspend fun getAllProducts(): List<ProductEntity>
    
    @Query("SELECT * FROM products ORDER BY popularityRank ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE isDeletedLocally = 0 ORDER BY isSynced ASC, name ASC")
    fun getAllProductsForManagementFlow(): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR :query = '') AND (categoryId = :categoryId OR :categoryId IS NULL) AND isDeletedLocally = 0 ORDER BY isSynced ASC, name ASC")
    fun searchProductsFlow(query: String, categoryId: String?): Flow<List<ProductEntity>>
    
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?
    
    @Query("SELECT * FROM products WHERE (productCode = :code OR skuCode = :code) AND isActive = 1 LIMIT 1")
    suspend fun getProductByCode(code: String): ProductEntity?
    
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)
    
    @Query("UPDATE products SET isDeletedLocally = 1 WHERE id = :id")
    suspend fun markAsDeletedLocally(id: String)
    
    @Query("UPDATE products SET isDeletedLocally = 1, isSynced = 0 WHERE id = :id")
    suspend fun markAsDeletedLocallyAndUnsynced(id: String)
    
    @Query("UPDATE products SET isActive = :isActive, isSynced = :isSynced WHERE id = :id")
    suspend fun updateProductStatus(id: String, isActive: Boolean, isSynced: Boolean)
    
    @Query("SELECT COUNT(*) FROM products WHERE isDeletedLocally = 0")
    suspend fun getTotalProductCount(): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE isSynced = 1 AND isDeletedLocally = 0")
    suspend fun getSyncedProductCount(): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE isSynced = 0 AND isDeletedLocally = 0")
    suspend fun getPendingSyncProductCount(): Int
    
    @Query("SELECT COUNT(*) FROM products WHERE isDeletedLocally = 1")
    suspend fun getDeletedProductCount(): Int
    
    @Query("DELETE FROM products")
    suspend fun deleteAll()
    
    // Sync operations
    @Query("SELECT * FROM products WHERE isSynced = 0 AND isDeletedLocally = 0")
    suspend fun getUnsyncedProducts(): List<ProductEntity>
    
    @Query("SELECT * FROM products WHERE isDeletedLocally = 1 AND isSynced = 0")
    suspend fun getDeletedProducts(): List<ProductEntity>
    
    @Query("UPDATE products SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("DELETE FROM products WHERE id = :id AND isDeletedLocally = 1")
    suspend fun permanentlyDelete(id: String)
}

