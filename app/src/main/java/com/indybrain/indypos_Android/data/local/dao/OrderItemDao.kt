package com.indybrain.indypos_Android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Result of today's top-selling product (by quantity).
 */
data class TodayTopProductResult(
    val productName: String,
    val totalQuantity: Long,
    val totalAmount: Double
)

@Dao
interface OrderItemDao {
    
    /**
     * Top-selling product for today (orders from today, excluding cancelled).
     * Groups by productName, orders by total quantity descending, returns first row.
     */
    @Query(
        """
        SELECT oi.productName AS productName,
               SUM(oi.quantity) AS totalQuantity,
               SUM(oi.totalPrice) AS totalAmount
        FROM order_items AS oi
        INNER JOIN orders AS o ON o.id = oi.orderId
        WHERE DATE(datetime(o.orderDate/1000, 'unixepoch', 'localtime')) = DATE(datetime('now', 'localtime'))
          AND o.statusRaw != 5
          AND TRIM(oi.productName) != ''
        GROUP BY oi.productName
        ORDER BY totalQuantity DESC
        LIMIT 1
        """
    )
    suspend fun getTodayTopProduct(): TodayTopProductResult?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: String): Flow<List<OrderItemEntity>>
    
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsSync(orderId: String): List<OrderItemEntity>
    
    /**
     * Sum of today's cost of goods sold (COGS) based on unitCost * quantity
     * for all order items whose parent order is created "today" (local time),
     * excluding cancelled orders (statusRaw == 5).
     */
    @Query(
        """
        SELECT 
            SUM(IFNULL(oi.unitCost, 0) * oi.quantity) 
        FROM order_items AS oi
        INNER JOIN orders AS o ON o.id = oi.orderId
        WHERE DATE(datetime(o.orderDate/1000, 'unixepoch', 'localtime')) = DATE(datetime('now', 'localtime'))
          AND o.statusRaw != 5
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

