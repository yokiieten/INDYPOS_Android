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
    
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC")
    suspend fun getAllActiveCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC")
    fun getAllActiveCategoriesFlow(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    suspend fun getAllCategories(): List<CategoryEntity>
    
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

