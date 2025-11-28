package com.indybrain.indypos_Android.data.repository

import com.indybrain.indypos_Android.core.network.NetworkConnectivityChecker
import com.indybrain.indypos_Android.data.local.dao.OrderAddonDao
import com.indybrain.indypos_Android.data.local.dao.OrderDao
import com.indybrain.indypos_Android.data.local.dao.OrderItemDao
import com.indybrain.indypos_Android.data.local.entity.OrderAddonEntity
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.data.local.entity.OrderItemEntity
import com.indybrain.indypos_Android.data.mapper.OrderMapper
import com.indybrain.indypos_Android.data.remote.api.OrdersApi
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val ordersApi: OrdersApi,
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val orderAddonDao: OrderAddonDao,
    private val networkConnectivityChecker: NetworkConnectivityChecker
) : OrderRepository {
    
    override fun getOrders(): Flow<Result<List<OrderEntity>>> {
        return orderDao.getAllOrders().map { Result.success(it) }
    }
    
    override suspend fun refreshOrders() {
        if (networkConnectivityChecker.isConnected()) {
            try {
                // Default params: first page, reasonable limit, no status/date filtering
                val response = ordersApi.getOrders(
                    limit = 100,
                    page = 1,
                    status = null,
                    startDate = null,
                    endDate = null
                )
                
                val ordersDto = response.data?.orders
                if (response.status == 200 && ordersDto != null) {
                    // Convert DTOs to entities
                    val orders = ordersDto.map { OrderMapper.toEntity(it) }
                    val orderItems = mutableListOf<OrderItemEntity>()
                    val orderAddons = mutableListOf<OrderAddonEntity>()
                    
                    // Process items and addons
                    ordersDto.forEach { orderDto ->
                        orderDto.items?.forEach { itemDto ->
                            orderItems.add(OrderMapper.toEntity(itemDto, orderDto.id))
                            itemDto.addons?.forEach { addonDto ->
                                orderAddons.add(OrderMapper.toEntity(addonDto, itemDto.id))
                            }
                        }
                    }
                    
                    // Save to database
                    orderDao.deleteAllOrders()
                    orderItemDao.deleteAllOrderItems()
                    orderAddonDao.deleteAllOrderAddons()
                    
                    orderDao.insertOrders(orders)
                    orderItemDao.insertOrderItems(orderItems)
                    orderAddonDao.insertOrderAddons(orderAddons)
                }
            } catch (e: Exception) {
                // Silently fail - local database will be used
            }
        }
    }
    
    override suspend fun getTodaySales(): Double {
        return orderDao.getTodaySales() ?: 0.0
    }
    
    override suspend fun getTodayOrderCount(): Int {
        return orderDao.getOrderCount()
    }
}

