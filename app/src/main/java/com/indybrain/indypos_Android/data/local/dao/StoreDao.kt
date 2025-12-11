package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(store: StoreEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stores: List<StoreEntity>)
    
    @Query("SELECT * FROM stores WHERE id = :id")
    fun getById(id: String): Flow<StoreEntity?>
    
    @Query("SELECT * FROM stores WHERE id = :id")
    suspend fun getByIdSync(id: String): StoreEntity?
    
    @Query("SELECT * FROM stores WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveStores(): Flow<List<StoreEntity>>
    
    @Query("SELECT * FROM stores WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getAllActiveStoresSync(): List<StoreEntity>
    
    @Query("SELECT * FROM stores ORDER BY name ASC")
    fun getAllStores(): Flow<List<StoreEntity>>
    
    @Query("SELECT * FROM stores ORDER BY name ASC")
    suspend fun getAllStoresSync(): List<StoreEntity>
    
    @Query("DELETE FROM stores WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM stores")
    suspend fun deleteAll()
}


