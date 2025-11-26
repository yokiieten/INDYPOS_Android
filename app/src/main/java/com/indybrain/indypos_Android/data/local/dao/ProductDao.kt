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
    
    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

