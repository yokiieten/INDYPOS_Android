package com.indybrain.indypos_Android.presentation.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.entity.OrderEntity
import com.indybrain.indypos_Android.domain.model.Order
import com.indybrain.indypos_Android.domain.model.OrderListQuery
import com.indybrain.indypos_Android.domain.model.OrderStatus
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    /** คืนตำแหน่งเลื่อนเมื่อ compose หน้ารายการใหม่ — ไม่ใส่ใน StateFlow กัน recompose ถี่ขณะเลื่อน */
    private var completedListScrollIndex: Int = 0
    private var completedListScrollOffset: Int = 0
    private var cancelledListScrollIndex: Int = 0
    private var cancelledListScrollOffset: Int = 0

    fun initialCompletedListScroll(): Pair<Int, Int> =
        completedListScrollIndex to completedListScrollOffset

    fun initialCancelledListScroll(): Pair<Int, Int> =
        cancelledListScrollIndex to cancelledListScrollOffset

    fun saveCompletedListScroll(index: Int, offset: Int) {
        if (completedListScrollIndex == index && completedListScrollOffset == offset) return
        completedListScrollIndex = index
        completedListScrollOffset = offset
    }

    fun saveCancelledListScroll(index: Int, offset: Int) {
        if (cancelledListScrollIndex == index && cancelledListScrollOffset == offset) return
        cancelledListScrollIndex = index
        cancelledListScrollOffset = offset
    }

    init {
        observeOrders()
        refreshOrders()
    }

    /**
     * In-memory list from repository matches the current tab query (server-filtered).
     */
    private fun observeOrders() {
        viewModelScope.launch {
            orderRepository.getOrderHistoryCache().collect { result ->
                result.onSuccess { cache ->
                    fun mapBucket(entities: List<OrderEntity>) =
                        entities.mapNotNull { entity ->
                            try {
                                val status = OrderStatus.fromCode(entity.statusRaw)
                                Order(
                                    id = entity.id,
                                    orderId = entity.orderNumber ?: "",
                                    createdAt = entity.createdAt ?: entity.orderDate ?: Date(),
                                    cancelledAt = if (status == OrderStatus.CANCELLED) entity.updatedAt else null,
                                    status = status,
                                    totalAmount = entity.total ?: 0.0
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                    val completed = mapBucket(cache.completed.orders)
                    val cancelled = mapBucket(cache.cancelled.orders)
                    _uiState.update { current ->
                        current.copy(
                            completedOrders = completed,
                            cancelledOrders = cancelled
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            errorMessage = error.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูล"
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tab: OrderTab) {
        val s = _uiState.value
        if (s.selectedTab == tab) return
        val needsInitialLoad = when (tab) {
            OrderTab.COMPLETED -> s.completedOrders.isEmpty()
            OrderTab.CANCELLED -> s.cancelledOrders.isEmpty()
        }
        _uiState.update { it.copy(selectedTab = tab) }
        if (needsInitialLoad) {
            refreshOrders()
        }
    }

    fun selectFilter(filter: OrderFilter) {
        _uiState.update { it.copy(filterOption = filter) }
        orderRepository.clearOrderHistoryCaches()
        refreshOrders()
    }

    fun selectSort(sort: OrderSort) {
        _uiState.update { it.copy(sortOption = sort) }
        orderRepository.clearOrderHistoryCaches()
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            val tabForThisRequest = _uiState.value.selectedTab
            _uiState.update { s ->
                when (tabForThisRequest) {
                    OrderTab.COMPLETED -> s.copy(
                        isLoading = true,
                        isLoadingMore = false,
                        completedPage = 1,
                        completedHasMore = true,
                        errorMessage = null
                    )
                    OrderTab.CANCELLED -> s.copy(
                        isLoading = true,
                        isLoadingMore = false,
                        cancelledPage = 1,
                        cancelledHasMore = true,
                        errorMessage = null
                    )
                }
            }
            val query = buildQuery(page = 1, forTab = tabForThisRequest)
            orderRepository.refreshOrders(query).fold(
                onSuccess = { info ->
                    _uiState.update { s ->
                        when (tabForThisRequest) {
                            OrderTab.COMPLETED -> s.copy(
                                isLoading = false,
                                completedPage = info.currentPage,
                                completedHasMore = info.hasNext
                            )
                            OrderTab.CANCELLED -> s.copy(
                                isLoading = false,
                                cancelledPage = info.currentPage,
                                cancelledHasMore = info.hasNext
                            )
                        }
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException) {
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "เกิดข้อผิดพลาดในการรีเฟรช"
                            )
                        }
                    }
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val (page, hasMore) = when (state.selectedTab) {
            OrderTab.COMPLETED -> state.completedPage to state.completedHasMore
            OrderTab.CANCELLED -> state.cancelledPage to state.cancelledHasMore
        }
        if (state.isLoadingMore || !hasMore || state.isLoading) return

        viewModelScope.launch {
            val tabForThisRequest = state.selectedTab
            try {
                val nextPage = page + 1
                _uiState.update { it.copy(isLoadingMore = true) }
                val query = buildQuery(page = nextPage, forTab = tabForThisRequest)
                orderRepository.loadMoreOrders(query).fold(
                    onSuccess = { info ->
                        _uiState.update { s ->
                            when (tabForThisRequest) {
                                OrderTab.COMPLETED -> s.copy(
                                    isLoadingMore = false,
                                    completedPage = info.currentPage,
                                    completedHasMore = info.hasNext
                                )
                                OrderTab.CANCELLED -> s.copy(
                                    isLoadingMore = false,
                                    cancelledPage = info.currentPage,
                                    cancelledHasMore = info.hasNext
                                )
                            }
                        }
                    },
                    onFailure = { e ->
                        if (e is CancellationException) {
                            _uiState.update { it.copy(isLoadingMore = false) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoadingMore = false,
                                    errorMessage = e.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูลเพิ่ม"
                                )
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = e.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูลเพิ่ม"
                    )
                }
            }
        }
    }

    fun setCustomRange(startMillis: Long, endMillis: Long) {
        val normalizedStart = minOf(startMillis, endMillis)
        val normalizedEnd = maxOf(startMillis, endMillis)
        _uiState.update {
            it.copy(
                filterOption = OrderFilter.SELECT_DATE,
                customStartDateMillis = normalizedStart,
                customEndDateMillis = normalizedEnd
            )
        }
        orderRepository.clearOrderHistoryCaches()
        refreshOrders()
    }

    private fun buildQuery(page: Int, forTab: OrderTab = _uiState.value.selectedTab): OrderListQuery {
        val s = _uiState.value
        val (startDate, endDate) = dateRangeStrings(
            filter = s.filterOption,
            customStartMillis = s.customStartDateMillis,
            customEndMillis = s.customEndDateMillis
        )
        return OrderListQuery(
            tab = when (forTab) {
                OrderTab.COMPLETED -> "completed"
                OrderTab.CANCELLED -> "cancelled"
            },
            sortBy = when (s.sortOption) {
                OrderSort.LATEST -> "latest"
                OrderSort.OLDEST -> "oldest"
                OrderSort.HIGHEST_AMOUNT -> "highestAmount"
            },
            page = page,
            limit = PAGE_SIZE,
            startDate = startDate,
            endDate = endDate
        )
    }

    private fun dateRangeStrings(
        filter: OrderFilter,
        customStartMillis: Long?,
        customEndMillis: Long?
    ): Pair<String?, String?> {
        val tz = TimeZone.getTimeZone("Asia/Bangkok")
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
        val cal = Calendar.getInstance(tz)

        fun formatDay(millis: Long): String {
            cal.timeInMillis = millis
            return fmt.format(cal.time)
        }

        return when (filter) {
            OrderFilter.ALL -> null to null
            OrderFilter.SELECT_DATE -> {
                if (customStartMillis == null || customEndMillis == null) {
                    null to null
                } else {
                    val startMillis = minOf(customStartMillis, customEndMillis)
                    val endMillis = maxOf(customStartMillis, customEndMillis)
                    formatDay(startMillis) to formatDay(endMillis)
                }
            }
            OrderFilter.TODAY -> {
                cal.timeInMillis = System.currentTimeMillis()
                val day = formatDay(cal.timeInMillis)
                day to day
            }
            OrderFilter.THIS_WEEK -> {
                cal.timeInMillis = System.currentTimeMillis()
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                setToStartOfDay(cal)
                val start = fmt.format(cal.time)
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.add(Calendar.MILLISECOND, -1)
                val end = fmt.format(cal.time)
                start to end
            }
            OrderFilter.THIS_MONTH -> {
                cal.timeInMillis = System.currentTimeMillis()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                setToStartOfDay(cal)
                val start = fmt.format(cal.time)
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.MILLISECOND, -1)
                val end = fmt.format(cal.time)
                start to end
            }
        }
    }

    private fun setToStartOfDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
