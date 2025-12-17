package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {
    
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: String): Flow<List<OrderItemEntity>>
    
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsSync(orderId: String): List<OrderItemEntity>
    
    /**
     * Sum of today's cost of goods sold (COGS) based on unitCost * quantity
     * for all order items whose parent order is created "today" (local time).
     */
    @Query(
        """
        SELECT 
            SUM(IFNULL(oi.unitCost, 0) * oi.quantity) 
        FROM order_items AS oi
        INNER JOIN orders AS o ON o.id = oi.orderId
        WHERE DATE(o.orderDate/1000, 'unixepoch') = DATE(datetime('now', 'localtime'))
        """
    )
    suspend fun getTodayCostOfExpenses(): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)
    
    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItems(orderId: String)
    
    @Query("DELETE FROM order_items")
    suspend fun deleteAllOrderItems()
}

