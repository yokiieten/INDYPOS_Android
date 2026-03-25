package com.indybrain.indypos_Android.data.repository

import android.util.Log
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.data.mapper.OrderMapper
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.api.UpdateOrderStatusRequestDto
import com.indybrain.indypos_Android.data.remote.dto.OrderDto
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Date
import javax.inject.Inject

private data class CachedOrders(
    val orders: List<OrderEntity> = emptyList(),
    val itemsByOrderId: Map<String, List<OrderItemEntity>> = emptyMap()
)

/**
 * Order list + detail lines are kept in memory from API responses only (no Room read/write for UX).
 * Room order tables may still exist for legacy/export paths elsewhere.
 */
class OrderRepositoryImpl @Inject constructor(
    private val ordersApi: OrdersApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : OrderRepository {

    private val ordersCache = MutableStateFlow(CachedOrders())

    override fun getOrders(): Flow<Result<List<OrderEntity>>> {
        return ordersCache.map { Result.success(it.orders) }
    }

    override suspend fun getOrdersSync(): Result<List<OrderEntity>> {
        return Result.success(ordersCache.value.orders)
    }

    override suspend fun refreshOrders() {
        if (!networkConnectivityChecker.isConnected()) return
        try {
            val response = ordersApi.getOrders(
                limit = 10,
                page = 1,
                status = null,
                startDate = null,
                endDate = null
            )
            val ordersDto = response.data?.orders
            if (response.status == 200 && ordersDto != null) {
                val (orders, itemsMap) = buildFromPaginatedDtos(ordersDto)
                ordersCache.value = CachedOrders(orders, itemsMap)
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "refreshOrders: ${e.message}", e)
        }
    }

    override suspend fun loadMoreOrders(page: Int, pageSize: Int): Boolean {
        if (!networkConnectivityChecker.isConnected()) {
            return false
        }
        return try {
            val response = ordersApi.getOrders(
                limit = pageSize,
                page = page,
                status = null,
                startDate = null,
                endDate = null
            )
            val ordersDto = response.data?.orders
            if (response.status == 200 && ordersDto != null && ordersDto.isNotEmpty()) {
                val (newOrders, newItems) = buildFromPaginatedDtos(ordersDto)
                ordersCache.update { prev ->
                    val existingIds = prev.orders.map { it.id }.toSet()
                    val mergedOrders = prev.orders + newOrders.filter { it.id !in existingIds }
                    val mergedItems = prev.itemsByOrderId + newItems
                    CachedOrders(mergedOrders, mergedItems)
                }
                val pagination = response.data?.pagination
                when {
                    pagination != null -> pagination.hasNext
                    else -> ordersDto.size >= pageSize
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "loadMoreOrders: ${e.message}", e)
            false
        }
    }

    override suspend fun refreshOrdersList() {
        if (!networkConnectivityChecker.isConnected()) return
        try {
            Log.d("OrderRepository", "Getting orders from API (list)")
            val response = ordersApi.getOrdersList()
            val ordersList = response.data
            if (response.status == 200 && ordersList != null) {
                Log.d("OrderRepository", "Get orders list success: ${ordersList.size} items")
                val orders = ordersList.mapNotNull { OrderMapper.toEntity(it) }
                val itemsMap = mutableMapOf<String, List<OrderItemEntity>>()
                ordersList.forEach { orderListData ->
                    val oid = orderListData.id ?: return@forEach
                    val items = orderListData.items
                        ?.mapNotNull { OrderMapper.toEntity(it, oid) }
                        ?: emptyList()
                    itemsMap[oid] = items
                }
                ordersCache.value = CachedOrders(orders, itemsMap)
            } else {
                Log.e(
                    "OrderRepository",
                    "Get orders list error: status=${response.status}, message=${response.message}"
                )
            }
        } catch (e: Exception) {
            Log.e("OrderRepository", "Get orders list error: ${e.message}", e)
        }
    }

    /** Kept for interface compatibility; no local order store. Use API-backed home/analytics if needed. */
    override suspend fun getTodaySales(): Double = 0.0

    override suspend fun getTodayOrderCount(): Int = 0

    override suspend fun getTodayCancelledOrderCount(): Int = 0

    override suspend fun getTodayCostOfExpenses(): Double = 0.0

    override suspend fun getOrderById(orderId: String): OrderEntity? {
        return ordersCache.value.orders.find { it.id == orderId }
    }

    override suspend fun getOrderItems(orderId: String): List<OrderItemEntity> {
        return ordersCache.value.itemsByOrderId[orderId].orEmpty()
    }

    override suspend fun getTodayTopProduct(): Triple<String, Int, Double>? = null

    override suspend fun updateOrderStatus(orderId: String, status: Int): Result<OrderEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
            }
            val existing = ordersCache.value.orders.find { it.id == orderId }
                ?: return Result.failure(Exception("ไม่พบออเดอร์"))
            val response = ordersApi.updateOrderStatus(orderId, UpdateOrderStatusRequestDto(status))
            if (response.status == 200 && response.data != null) {
                val updated = OrderMapper.toEntity(response.data)
                ordersCache.update { prev ->
                    CachedOrders(
                        orders = prev.orders.map { if (it.id == orderId) updated else it },
                        itemsByOrderId = prev.itemsByOrderId
                    )
                }
                Result.success(updated)
            } else if (response.status == 200) {
                val patched = existing.copy(statusRaw = status, updatedAt = Date())
                ordersCache.update { prev ->
                    CachedOrders(
                        orders = prev.orders.map { if (it.id == orderId) patched else it },
                        itemsByOrderId = prev.itemsByOrderId
                    )
                }
                Result.success(patched)
            } else {
                val errorMessage = response.message?.takeIf { it.isNotBlank() }
                    ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะออเดอร์"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "เกิดข้อผิดพลาดในการอัปเดตสถานะออเดอร์"))
        }
    }

    private fun buildFromPaginatedDtos(
        ordersDto: List<OrderDto>
    ): Pair<List<OrderEntity>, Map<String, List<OrderItemEntity>>> {
        val orders = ordersDto.map { OrderMapper.toEntity(it) }
        val itemsMap = ordersDto.associate { orderDto ->
            val items = orderDto.items?.map { OrderMapper.toEntity(it, orderDto.id) }.orEmpty()
            orderDto.id to items
        }
        return orders to itemsMap
    }
}
