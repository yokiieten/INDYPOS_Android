package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?
    
    @Query("SELECT * FROM categories WHERE isActive = 1 AND isDeletedLocally = 0 ORDER BY sortOrder ASC")
    suspend fun getAllActiveCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE isActive = 1 AND isDeletedLocally = 0 ORDER BY sortOrder ASC")
    fun getAllActiveCategoriesFlow(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE isDeletedLocally = 0 ORDER BY sortOrder ASC")
    suspend fun getAllCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE isDeletedLocally = 0 ORDER BY sortOrder ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>
    
    @Query("SELECT MAX(sortOrder) FROM categories")
    suspend fun getMaxSortOrder(): Int?
    
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)
    
    @Query("UPDATE categories SET isDeletedLocally = 1 WHERE id = :id")
    suspend fun markAsDeletedLocally(id: String)
}

