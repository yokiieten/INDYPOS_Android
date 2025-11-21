package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    
    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)
    
    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
    
    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int
    
    @Query("SELECT SUM(total) FROM orders WHERE DATE(orderDate/1000, 'unixepoch') = DATE(datetime('now', 'localtime'))")
    suspend fun getTodaySales(): Double?
}

