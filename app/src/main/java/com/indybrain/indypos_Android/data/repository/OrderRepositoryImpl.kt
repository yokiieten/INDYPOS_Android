package com.indybrain.indypos_Android.data.repository

import android.util.Log
import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.data.mapper.OrderMapper
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.data.remote.api.UpdateOrderStatusRequestDto
import com.indybrain.indypos_Android.data.remote.dto.OrderDto
import com.indybrain.indypos_Android.domain.model.OrderHistoryCache
import com.indybrain.indypos_Android.domain.model.OrderListPageInfo
import com.indybrain.indypos_Android.domain.model.OrderListQuery
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.model.OrderTabBucket
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Order list + line items are kept in memory from API only ([OrderEntity] / [OrderItemEntity] are not Room tables).
 * แยก cache ตามแท็บ completed / cancelled เพื่อสลับแท็บแล้วยังเลื่อนตำแหน่งเดิมได้ (ไม่ถูกทับด้วยหน้าแรกของอีกแท็บ).
 */
class OrderRepositoryImpl @Inject constructor(
    private val ordersApi: OrdersApi,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : OrderRepository {

    private val ordersCache = MutableStateFlow(OrderHistoryCache())
    private val completedRefreshGen = AtomicInteger(0)
    private val cancelledRefreshGen = AtomicInteger(0)

    private fun refreshGenFor(tab: String): AtomicInteger =
        if (tab == "completed") completedRefreshGen else cancelledRefreshGen

    override fun getOrderHistoryCache(): Flow<Result<OrderHistoryCache>> {
        return ordersCache.map { Result.success(it) }
    }

    override fun clearOrderHistoryCaches() {
        ordersCache.value = OrderHistoryCache()
    }

    override suspend fun getOrdersSync(): Result<List<OrderEntity>> {
        val c = ordersCache.value
        return Result.success(c.completed.orders + c.cancelled.orders)
    }

    override suspend fun refreshOrders(query: OrderListQuery): Result<OrderListPageInfo> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
        }
        val genCounter = refreshGenFor(query.tab)
        val myGen = genCounter.incrementAndGet()
        return try {
            val limit = query.limit.coerceIn(1, 100)
            val response = ordersApi.getOrders(
                tab = query.tab,
                sortBy = query.sortBy,
                page = query.page,
                limit = limit,
                startDate = query.startDate,
                endDate = query.endDate
            )
            if (myGen != genCounter.get()) {
                return Result.failure(CancellationException())
            }
            if (response.status != 200) {
                val msg = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message?.takeIf { it.isNotBlank() }
                    ?: "เกิดข้อผิดพลาดในการโหลดออเดอร์"
                return Result.failure(Exception(msg))
            }
            val ordersDto = response.data?.orders ?: emptyList()
            val (orders, itemsMap) = buildFromPaginatedDtos(ordersDto)
            val bucket = OrderTabBucket(orders, itemsMap)
            ordersCache.update { prev ->
                when (query.tab) {
                    "completed" -> prev.copy(completed = bucket)
                    "cancelled" -> prev.copy(cancelled = bucket)
                    else -> prev
                }
            }
            val p = response.data?.pagination
            val hasNext = p?.hasNext ?: (ordersDto.size >= limit)
            val currentPage = p?.currentPage ?: query.page
            Result.success(OrderListPageInfo(hasNext = hasNext, currentPage = currentPage))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val msg = parseHttpErrorMessage(e)
            Log.e("OrderRepository", "refreshOrders: ${e.code()} $msg", e)
            Result.failure(Exception(msg ?: e.message() ?: "เกิดข้อผิดพลาด"))
        } catch (e: Exception) {
            Log.e("OrderRepository", "refreshOrders: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun loadMoreOrders(query: OrderListQuery): Result<OrderListPageInfo> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
        }
        val genCounter = refreshGenFor(query.tab)
        val myGen = genCounter.get()
        return try {
            val limit = query.limit.coerceIn(1, 100)
            val response = ordersApi.getOrders(
                tab = query.tab,
                sortBy = query.sortBy,
                page = query.page,
                limit = limit,
                startDate = query.startDate,
                endDate = query.endDate
            )
            if (myGen != genCounter.get()) {
                return Result.failure(CancellationException())
            }
            if (response.status != 200) {
                val msg = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message?.takeIf { it.isNotBlank() }
                    ?: "เกิดข้อผิดพลาดในการโหลดออเดอร์"
                return Result.failure(Exception(msg))
            }
            val ordersDto = response.data?.orders ?: emptyList()
            val p = response.data?.pagination
            if (ordersDto.isEmpty()) {
                return Result.success(
                    OrderListPageInfo(
                        hasNext = p?.hasNext ?: false,
                        currentPage = p?.currentPage ?: query.page
                    )
                )
            }
            val (newOrders, newItems) = buildFromPaginatedDtos(ordersDto)
            if (myGen != genCounter.get()) {
                return Result.failure(CancellationException())
            }
            ordersCache.update { prev ->
                when (query.tab) {
                    "completed" -> {
                        val b = prev.completed
                        val existingIds = b.orders.map { it.id }.toSet()
                        val mergedOrders = b.orders + newOrders.filter { it.id !in existingIds }
                        val mergedItems = b.itemsByOrderId + newItems
                        prev.copy(completed = OrderTabBucket(mergedOrders, mergedItems))
                    }
                    "cancelled" -> {
                        val b = prev.cancelled
                        val existingIds = b.orders.map { it.id }.toSet()
                        val mergedOrders = b.orders + newOrders.filter { it.id !in existingIds }
                        val mergedItems = b.itemsByOrderId + newItems
                        prev.copy(cancelled = OrderTabBucket(mergedOrders, mergedItems))
                    }
                    else -> prev
                }
            }
            val hasNext = p?.hasNext ?: (ordersDto.size >= limit)
            Result.success(
                OrderListPageInfo(
                    hasNext = hasNext,
                    currentPage = p?.currentPage ?: query.page
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val msg = parseHttpErrorMessage(e)
            Log.e("OrderRepository", "loadMoreOrders: ${e.code()} $msg", e)
            Result.failure(Exception(msg ?: e.message() ?: "เกิดข้อผิดพลาด"))
        } catch (e: Exception) {
            Log.e("OrderRepository", "loadMoreOrders: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchOrderDetail(orderId: String): Result<Unit> {
        if (!networkConnectivityChecker.isConnected()) {
            return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
        }
        return try {
            val response = ordersApi.getOrderById(orderId)
            if (response.status == 200 && response.data != null) {
                val dto = response.data
                val order = OrderMapper.toEntity(dto)
                val items = dto.items?.map { OrderMapper.toEntity(it, dto.id) }.orEmpty()
                ordersCache.update { prev ->
                    fun mergeInto(bucket: OrderTabBucket): OrderTabBucket {
                        val newOrders = if (bucket.orders.any { it.id == orderId }) {
                            bucket.orders.map { existing ->
                                if (existing.id == orderId) order else existing
                            }
                        } else {
                            bucket.orders + order
                        }
                        return bucket.copy(
                            orders = newOrders,
                            itemsByOrderId = bucket.itemsByOrderId + (orderId to items)
                        )
                    }
                    when {
                        prev.completed.orders.any { it.id == orderId } ->
                            prev.copy(completed = mergeInto(prev.completed))
                        prev.cancelled.orders.any { it.id == orderId } ->
                            prev.copy(cancelled = mergeInto(prev.cancelled))
                        order.statusRaw == OrderStatus.CANCELLED.code ->
                            prev.copy(cancelled = mergeInto(prev.cancelled))
                        else ->
                            prev.copy(completed = mergeInto(prev.completed))
                    }
                }
                Result.success(Unit)
            } else {
                val msg = response.error?.takeIf { it.isNotBlank() }
                    ?: response.message?.takeIf { it.isNotBlank() }
                    ?: when (response.status) {
                        404 -> "ไม่พบออเดอร์"
                        else -> "เกิดข้อผิดพลาดในการโหลดออเดอร์"
                    }
                Result.failure(Exception(msg))
            }
        } catch (e: HttpException) {
            val msg = parseHttpErrorMessage(e)
            Log.e("OrderRepository", "fetchOrderDetail: ${e.code()} $msg", e)
            Result.failure(Exception(msg ?: e.message()))
        } catch (e: Exception) {
            Log.e("OrderRepository", "fetchOrderDetail: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseHttpErrorMessage(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string() ?: return null
            val obj = com.google.gson.JsonParser.parseString(body).asJsonObject
            obj.get("error")?.asString?.takeIf { it.isNotBlank() }
                ?: obj.get("message")?.asString?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
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
                val completedOrders =
                    orders.filter { it.statusRaw != OrderStatus.CANCELLED.code }
                val cancelledOrders =
                    orders.filter { it.statusRaw == OrderStatus.CANCELLED.code }
                val compItems = completedOrders.associate { it.id to (itemsMap[it.id].orEmpty()) }
                val cancItems = cancelledOrders.associate { it.id to (itemsMap[it.id].orEmpty()) }
                ordersCache.value = OrderHistoryCache(
                    completed = OrderTabBucket(completedOrders, compItems),
                    cancelled = OrderTabBucket(cancelledOrders, cancItems)
                )
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

    override suspend fun getTodaySales(): Double = 0.0

    override suspend fun getTodayOrderCount(): Int = 0

    override suspend fun getTodayCancelledOrderCount(): Int = 0

    override suspend fun getTodayCostOfExpenses(): Double = 0.0

    override suspend fun getOrderById(orderId: String): OrderEntity? {
        val c = ordersCache.value
        return c.completed.orders.find { it.id == orderId }
            ?: c.cancelled.orders.find { it.id == orderId }
    }

    override suspend fun getOrderItems(orderId: String): List<OrderItemEntity> {
        val c = ordersCache.value
        return c.completed.itemsByOrderId[orderId]
            ?: c.cancelled.itemsByOrderId[orderId].orEmpty()
    }

    override suspend fun getTodayTopProduct(): Triple<String, Int, Double>? = null

    override suspend fun updateOrderStatus(orderId: String, status: Int): Result<OrderEntity> {
        return try {
            if (!networkConnectivityChecker.isConnected()) {
                return Result.failure(Exception("กรุณาเชื่อมต่ออินเทอร์เน็ต"))
            }
            val existing = getOrderById(orderId)
                ?: return Result.failure(Exception("ไม่พบออเดอร์"))
            val response = ordersApi.updateOrderStatus(orderId, UpdateOrderStatusRequestDto(status))
            if (response.status == 200 && response.data != null) {
                val updated = OrderMapper.toEntity(response.data)
                ordersCache.update { prev ->
                    when {
                        prev.completed.orders.any { it.id == orderId } ->
                            prev.copy(
                                completed = prev.completed.copy(
                                    orders = prev.completed.orders.map {
                                        if (it.id == orderId) updated else it
                                    }
                                )
                            )
                        prev.cancelled.orders.any { it.id == orderId } ->
                            prev.copy(
                                cancelled = prev.cancelled.copy(
                                    orders = prev.cancelled.orders.map {
                                        if (it.id == orderId) updated else it
                                    }
                                )
                            )
                        else -> prev
                    }
                }
                Result.success(updated)
            } else if (response.status == 200) {
                val patched = existing.copy(statusRaw = status, updatedAt = Date())
                ordersCache.update { prev ->
                    when {
                        prev.completed.orders.any { it.id == orderId } ->
                            prev.copy(
                                completed = prev.completed.copy(
                                    orders = prev.completed.orders.map {
                                        if (it.id == orderId) patched else it
                                    }
                                )
                            )
                        prev.cancelled.orders.any { it.id == orderId } ->
                            prev.copy(
                                cancelled = prev.cancelled.copy(
                                    orders = prev.cancelled.orders.map {
                                        if (it.id == orderId) patched else it
                                    }
                                )
                            )
                        else -> prev
                    }
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
