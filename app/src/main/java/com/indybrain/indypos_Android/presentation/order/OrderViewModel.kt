package com.indybrain.indypos_Android.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.model.Order
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
    
    init {
        observeOrders()
        refreshOrders()
    }
    
    /**
     * Observe all orders from repository and keep an in-memory list.
     * Filtering / sorting is done in-memory based on [OrderUiState.filterOption] and [OrderUiState.sortOption].
     */
    private fun observeOrders() {
        viewModelScope.launch {
            orderRepository.getOrders().collect { result ->
                result.onSuccess { entities ->
                    val orders = entities.map { entity ->
                        val status = OrderStatus.fromCode(entity.statusRaw) // Changed from orderStatus to statusRaw
                        Order(
                            id = entity.id,
                            orderId = entity.orderNumber,
                            createdAt = entity.createdAt ?: entity.orderDate, // Use createdAt if available, fallback to orderDate
                            cancelledAt = if (status == OrderStatus.CANCELLED) entity.updatedAt else null, // Use updatedAt for cancelled orders
                            status = status,
                            totalAmount = entity.total
                        )
                    }
                    
                    _uiState.update { current ->
                        val (completed, cancelled) = filterAndSortOrders(
                            orders = orders,
                            filter = current.filterOption,
                            sort = current.sortOption,
                            customStartMillis = current.customStartDateMillis,
                            customEndMillis = current.customEndDateMillis
                        )
                        current.copy(
                            isLoading = false,
                            allOrders = orders,
                            completedOrders = completed,
                            cancelledOrders = cancelled
                        )
                    }
                }.onFailure {
                    _uiState.update { current ->
                        current.copy(isLoading = false)
                    }
                }
            }
        }
    }
    
    fun selectTab(tab: OrderTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
    
    fun selectFilter(filter: OrderFilter) {
        _uiState.update { current ->
            val (completed, cancelled) = filterAndSortOrders(
                orders = current.allOrders,
                filter = filter,
                sort = current.sortOption,
                customStartMillis = current.customStartDateMillis,
                customEndMillis = current.customEndDateMillis
            )
            current.copy(
                filterOption = filter,
                completedOrders = completed,
                cancelledOrders = cancelled
            )
        }
    }
    
    fun selectSort(sort: OrderSort) {
        _uiState.update { current ->
            val (completed, cancelled) = filterAndSortOrders(
                orders = current.allOrders,
                filter = current.filterOption,
                sort = sort,
                customStartMillis = current.customStartDateMillis,
                customEndMillis = current.customEndDateMillis
            )
            current.copy(
                sortOption = sort,
                completedOrders = completed,
                cancelledOrders = cancelled
            )
        }
    }
    
    fun refreshOrders() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    currentPage = 1,
                    hasMore = true
                )
            }
            orderRepository.refreshOrders()
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) {
            return
        }

        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            _uiState.update { it.copy(isLoadingMore = true) }
            val hasMore = orderRepository.loadMoreOrders(page = nextPage, pageSize = 10)
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    currentPage = if (hasMore) nextPage else nextPage,
                    hasMore = hasMore
                )
            }
        }
    }

    /**
     * ตั้งค่าช่วงวันที่แบบกำหนดเอง (ใช้กับ filter SELECT_DATE)
     */
    fun setCustomRange(startMillis: Long, endMillis: Long) {
        val normalizedStart = minOf(startMillis, endMillis)
        val normalizedEnd = maxOf(startMillis, endMillis)

        _uiState.update { current ->
            val (completed, cancelled) = filterAndSortOrders(
                orders = current.allOrders,
                filter = OrderFilter.SELECT_DATE,
                sort = current.sortOption,
                customStartMillis = normalizedStart,
                customEndMillis = normalizedEnd
            )
            current.copy(
                filterOption = OrderFilter.SELECT_DATE,
                customStartDateMillis = normalizedStart,
                customEndDateMillis = normalizedEnd,
                completedOrders = completed,
                cancelledOrders = cancelled
            )
        }
    }

    /**
     * Apply date filter + sorting on the given order list.
     * Date ranges are calculated in Asia/Bangkok timezone to match UI expectations.
     */
    private fun filterAndSortOrders(
        orders: List<Order>,
        filter: OrderFilter,
        sort: OrderSort,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null
    ): Pair<List<Order>, List<Order>> {
        if (orders.isEmpty()) {
            return emptyList<Order>() to emptyList()
        }

        val timeZone = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val calendar = Calendar.getInstance(timeZone)

        val filtered = when (filter) {
            OrderFilter.ALL -> orders

            OrderFilter.SELECT_DATE -> {
                if (customStartMillis == null || customEndMillis == null) {
                    orders
                } else {
                    calendar.timeInMillis = customStartMillis
                    setToStartOfDay(calendar)
                    val start = calendar.timeInMillis

                    calendar.timeInMillis = customEndMillis
                    setToEndOfDay(calendar)
                    val end = calendar.timeInMillis

                    orders.filter { it.createdAt.time in start..end }
                }
            }

            OrderFilter.TODAY -> {
                calendar.timeInMillis = System.currentTimeMillis()
                setToStartOfDay(calendar)
                val start = calendar.timeInMillis
                setToEndOfDay(calendar)
                val end = calendar.timeInMillis
                orders.filter { it.createdAt.time in start..end }
            }

            OrderFilter.THIS_WEEK -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // start of week
                setToStartOfDay(calendar)
                val start = calendar.timeInMillis

                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val end = calendar.timeInMillis

                orders.filter { it.createdAt.time in start..end }
            }

            OrderFilter.THIS_MONTH -> {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                setToStartOfDay(calendar)
                val start = calendar.timeInMillis

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val end = calendar.timeInMillis

                orders.filter { it.createdAt.time in start..end }
            }
        }

        val sorted = when (sort) {
            OrderSort.LATEST -> filtered.sortedByDescending { it.createdAt.time }
            OrderSort.OLDEST -> filtered.sortedBy { it.createdAt.time }
            OrderSort.HIGHEST_AMOUNT -> filtered.sortedByDescending { it.totalAmount }
        }

        // แท็บ "เสร็จสิ้น" แสดงทุกสถานะที่ไม่ใช่ยกเลิก
        val completedOrders = sorted.filter { it.status != OrderStatus.CANCELLED }
        // แท็บ "ยกเลิก" แสดงเฉพาะสถานะยกเลิก (code = 5)
        val cancelledOrders = sorted.filter { it.status == OrderStatus.CANCELLED }

        return completedOrders to cancelledOrders
    }

    private fun setToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun setToEndOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    private fun createDate(year: Int, month: Int, day: Int, hour: Int, minute: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, hour, minute, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}

