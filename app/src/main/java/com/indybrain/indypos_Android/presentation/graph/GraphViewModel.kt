package com.indybrain.indypos_Android.presentation.graph

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import com.indybrain.indypos_Android.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val productDao: ProductDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GraphUiState(isLoading = true))
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize with Today period, but don't fetch yet
        // Fetch will be triggered when screen opens via refreshOrdersFromApi()
        _uiState.update { it.copy(selectedPeriod = TimePeriod.Today) }
    }
    
    fun selectPeriod(period: TimePeriod) {
        _uiState.update { current ->
            current.copy(selectedPeriod = period, isLoading = true)
        }
        loadData(period)
    }

    /**
     * ตั้งค่าช่วงวันที่แบบกำหนดเอง และโหลดข้อมูลสำหรับช่วงนั้น
     */
    fun setCustomRange(startMillis: Long, endMillis: Long) {
        val normalizedStart = minOf(startMillis, endMillis)
        val normalizedEnd = maxOf(startMillis, endMillis)
        
        _uiState.update { current ->
            current.copy(
                selectedPeriod = TimePeriod.Custom,
                isLoading = true,
                customStartDateMillis = normalizedStart,
                customEndDateMillis = normalizedEnd
            )
        }
        
        viewModelScope.launch {
            try {
                loadCustomRangeData(normalizedStart, normalizedEnd)
            } catch (e: Exception) {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = e.message ?: context.getString(R.string.graph_error_loading)
                        )
                    }
            }
        }
    }
    
    /**
     * Load graph data based on selected period.
     *
     * For now:
     * - TimePeriod.Today: ใช้ข้อมูลจริงจาก Room (orders + order_items)
     * - ช่วงอื่น ๆ (Week, Month, Custom): ยังใช้ค่าเริ่มต้นว่าง ๆ เพื่อไม่ให้ mock ตัวเลขผิด ๆ
     */
    private fun loadData(period: TimePeriod) {
        viewModelScope.launch {
            try {
                when (period) {
                    TimePeriod.Today -> loadTodayDataFromRoom()
                    TimePeriod.Week -> loadWeekDataFromRoom()
                    TimePeriod.Month -> loadMonthDataFromRoom()
                    TimePeriod.Custom -> {
                        val start = _uiState.value.customStartDateMillis
                        val end = _uiState.value.customEndDateMillis
                        if (start != null && end != null) {
                            loadCustomRangeData(start, end)
                        } else {
                            _uiState.update { current ->
                                current.copy(
                                    isLoading = false,
                                    summary = GraphSummary(),
                                    chartData = emptyList(),
                                    revenueComparison = RevenueComparison(),
                                    productStats = emptyList(),
                                    totalProductSalesInPeriod = 0.0,
                                    bestSellers = emptyList()
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = e.message ?: context.getString(R.string.graph_error_loading)
                        )
                    }
            }
        }
    }

    /**
     * Fetch orders and products from API and save to Room, then refresh current period data
     * Called when GraphScreen opens
     *
     * Products must be loaded so that Best Seller Top 3 can display product images.
     * Without this, images only show after visiting MainProductScreen (เริ่มออเดอร์) first.
     */
    fun refreshOrdersFromApi() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                // Fetch both products and orders in parallel so Best Seller images are available
                coroutineScope {
                    launch {
                        productRepository.fetchAndSaveProducts()
                    }
                    launch {
                        orderRepository.refreshOrdersList()
                    }
                }
                // After fetching, refresh current period data (best sellers need products in Room)
                refreshCurrentPeriod()
            } catch (e: Exception) {
                // If API call fails, still try to load from Room
                refreshCurrentPeriod()
            }
        }
    }
    
    /**
     * รีโหลดข้อมูลตามช่วงเวลาที่เลือกปัจจุบัน
     * ใช้เมื่อเข้าหน้ากราฟใหม่เพื่อให้ข้อมูลอัปเดตจาก Room ล่าสุด
     */
    fun refreshCurrentPeriod() {
        val currentPeriod = _uiState.value.selectedPeriod
        _uiState.update { it.copy(isLoading = true) }
        loadData(currentPeriod)
    }
    
    /**
     * ดึงข้อมูลจริงจาก Room สำหรับช่วง "วันนี้"
     * - todaySales: SUM(total) ของออเดอร์วันนี้ (ไม่รวม cancelled)
     * - costOfExpenses: SUM(unitCost * quantity) ของ items วันนี้
     * - ordersToday: จำนวนออเดอร์วันนี้ (ไม่รวม cancelled)
     * - cancelledOrders: จำนวนออเดอร์ที่ยกเลิกวันนี้
     */
    private suspend fun loadTodayDataFromRoom() {
        val todaySales = orderRepository.getTodaySales()
        val ordersToday = orderRepository.getTodayOrderCount()
        val cancelledOrders = orderRepository.getTodayCancelledOrderCount()
        val costOfExpenses = orderRepository.getTodayCostOfExpenses()
        
        val summary = GraphSummary(
            todaySales = todaySales,
            costOfExpenses = costOfExpenses,
            ordersToday = ordersToday,
            cancelledOrders = cancelledOrders,
            totalSales = todaySales
        )
        
        // ใช้ mock data สำหรับส่วนกราฟอื่น ๆ แทนไปก่อน เพื่อไม่ให้หน้าจอว่างเปล่า
        // (เฉพาะ Today; ตัวเลข summary ด้านบนใช้ Room จริงแล้ว)
        // แสดงครบทุกชั่วโมง 00:00 - 23:00
        val chartData = (0..23).map { hour ->
            val label = String.format("%02d:00", hour)
            val value = when (hour) {
                15 -> todaySales * 0.4
                16 -> todaySales
                17 -> todaySales * 0.2
                else -> 0.0
            }
            ChartDataPoint(label, value)
        }
        
        // ดึงออเดอร์ทั้งหมดและกรองเฉพาะวันนี้
        val ordersResult = orderRepository.getOrders().first()
        val orders = ordersResult.getOrElse { emptyList() }
        
        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val tomorrow = calendar.apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.time
        
        val todayOrders = orders.filter { order ->
            order.orderDate >= today && order.orderDate < tomorrow
        }
        
        // ยอดตามช่องทาง (โอน / เงินสด) วันนี้
        val activeTodayOrders = todayOrders.filter { it.statusRaw != 5 }
        val transferAmount = activeTodayOrders
            .filter { it.paymentTypeRaw == 1 } // 1 = transfer
            .sumOf { it.total }
        val cashAmount = activeTodayOrders
            .filter { it.paymentTypeRaw == 0 } // 0 = cash
            .sumOf { it.total }
        val revenueComparison = RevenueComparison(
            transferAmount = transferAmount,
            cashAmount = cashAmount
        )
        
        // สินค้า Top ของวันนี้ (จาก order items)
        data class ProductAggToday(
            var name: String,
            var amount: Double,
            var cost: Double,
            var quantity: Int,
            var productId: String?
        )
        
        val productMap = mutableMapOf<String, ProductAggToday>()
        for (order in activeTodayOrders) {
            val items = orderRepository.getOrderItems(order.id)
            items.forEach { item ->
                val key = item.productId ?: item.productName
                val agg = productMap.getOrPut(key) {
                    ProductAggToday(
                        name = item.productName,
                        amount = 0.0,
                        cost = 0.0,
                        quantity = 0,
                        productId = item.productId
                    )
                }
                agg.amount += item.totalPrice
                agg.cost += (item.unitCost ?: 0.0) * item.quantity
                agg.quantity += item.quantity
            }
        }
        
        // สินค้าที่ทำรายได้สูงสุด: เรียงตามยอดขายเต็ม (ไม่หักต้นทุน)
        val topByRevenue = productMap.entries
            .map { (k, v) -> k to v.amount }
            .sortedByDescending { it.second }
            .take(3)
        val revenueList = topByRevenue.map { (key, _) -> key to productMap[key]!! }
        val maxRevenue = revenueList.maxOfOrNull { it.second.amount } ?: 1.0
        val productStats = revenueList.map { (_, agg) ->
            ProductStatsData(
                name = agg.name,
                amount = agg.amount,
                quantity = agg.quantity,
                progress = (agg.amount / maxRevenue).coerceIn(0.0, 1.0)
            )
        }
        // Best seller: เรียงตามจำนวนชิ้นที่ขาย
        val topProducts = productMap.entries.sortedByDescending { it.value.quantity }.take(3)
        val bestSellers = topProducts.mapIndexed { index, (_, agg) ->
            val product = agg.productId?.let { productDao.getProductById(it) }
            BestSellerData(
                productName = agg.name,
                totalSales = agg.amount,
                salesCount = agg.quantity,
                imageUrl = product?.imageUrl,
                colorHex = product?.selectedColorHex,
                rank = index + 1
            )
        }
        val totalProductSalesInPeriod = productMap.values.sumOf { it.amount }
        
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                summary = summary,
                chartData = chartData,
                revenueComparison = revenueComparison,
                productStats = productStats,
                totalProductSalesInPeriod = totalProductSalesInPeriod,
                bestSellers = bestSellers
            )
        }
    }

    /**
     * ดึงข้อมูลจริงจาก Room สำหรับช่วง "1 สัปดาห์"
     * - 1 สัปดาห์ = ตั้งแต่วันอาทิตย์ 00:00:00 จนถึงวันปัจจุบัน 23:59:59 ของสัปดาห์นั้น
     * - กราฟผลรวมยอดขาย: แสดงตามวันในสัปดาห์ (จ. อ. พ. พฤ. ศ. ส. อา.)
     * - กราฟอื่น ๆ (ช่องทาง, สินค้า, Best seller) ใช้ข้อมูลจริงของสัปดาห์เดียวกัน
     */
    private suspend fun loadWeekDataFromRoom() {
        // ดึงออเดอร์ทั้งหมดจาก Room แล้ว filter เองตามวันที่
        val ordersResult = orderRepository.getOrders().first()
        val orders = ordersResult.getOrElse { emptyList() }
        
        if (orders.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = GraphSummary(),
                    chartData = emptyList(),
                    revenueComparison = RevenueComparison(),
                    productStats = emptyList(),
                    totalProductSalesInPeriod = 0.0,
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        val calendar = Calendar.getInstance(Locale.getDefault())
        // สิ้นสุด: วันนี้ 23:59:59
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time
        
        // เริ่มต้น: วันอาทิตย์ของสัปดาห์นั้น 00:00:00
        // Calendar.DAY_OF_WEEK: 1=Sunday, 2=Monday, ..., 7=Saturday
        calendar.time = endDate
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromSunday = dayOfWeek - Calendar.SUNDAY // 0 = อา, 1 = จ, ...
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromSunday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        val weekOrders = orders.filter { order ->
            order.orderDate >= startDate && order.orderDate <= endDate
        }
        
        if (weekOrders.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = GraphSummary(),
                    chartData = emptyList(),
                    revenueComparison = RevenueComparison(),
                    productStats = emptyList(),
                    totalProductSalesInPeriod = 0.0,
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        // Filter out cancelled orders for sales calculation
        val activeWeekOrders = weekOrders.filter { it.statusRaw != 5 }
        val cancelledCount = weekOrders.count { it.statusRaw == 5 }
        
        // Summary (exclude cancelled orders from sales)
        val totalSales = activeWeekOrders.sumOf { it.total }
        val orderCount = activeWeekOrders.size
        
        // คำนวณต้นทุนจาก order items (unitCost * quantity) เฉพาะออเดอร์ที่ไม่ถูกยกเลิก
        var totalCost = 0.0
        for (order in activeWeekOrders) {
            val items = orderRepository.getOrderItems(order.id)
            totalCost += items.sumOf { (it.unitCost ?: 0.0) * it.quantity }
        }
        
        val summary = GraphSummary(
            todaySales = totalSales, // ใช้ field นี้เป็นยอดรวมของช่วงที่เลือก
            costOfExpenses = totalCost,
            ordersToday = orderCount,
            cancelledOrders = cancelledCount,
            totalSales = totalSales
        )
        
        // กราฟผลรวมยอดขายรายวันในสัปดาห์ (exclude cancelled)
        val dayKeyFormatter = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val salesByDayKey = activeWeekOrders.groupBy { order ->
            dayKeyFormatter.format(order.orderDate)
        }.mapValues { (_, dayOrders) ->
            dayOrders.sumOf { it.total }
        }
        
        // แกน X: แสดงตามวันจริงในสัปดาห์ (7 วัน)
        val dayLabels = listOf(
            context.getString(R.string.graph_day_monday),
            context.getString(R.string.graph_day_tuesday),
            context.getString(R.string.graph_day_wednesday),
            context.getString(R.string.graph_day_thursday),
            context.getString(R.string.graph_day_friday),
            context.getString(R.string.graph_day_saturday),
            context.getString(R.string.graph_day_sunday)
        )
        val chartData = mutableListOf<ChartDataPoint>()
        calendar.time = startDate
        for (i in 0..6) {
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            // แปลง Calendar.DAY_OF_WEEK (1=Sunday, 2=Monday, ..., 7=Saturday) เป็น index (0=Monday, 6=Sunday)
            val dayIndex = when (dayOfWeek) {
                Calendar.SUNDAY -> 6
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> i
            }
            val label = dayLabels[dayIndex]
            val key = dayKeyFormatter.format(calendar.time)
            val value = salesByDayKey[key] ?: 0.0
            chartData.add(ChartDataPoint(label, value))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // ยอดตามช่องทาง (โอน / เงินสด) ในช่วง 1 สัปดาห์ (exclude cancelled)
        val transferAmount = activeWeekOrders
            .filter { it.paymentTypeRaw == 1 } // 1 = transfer
            .sumOf { it.total }
        val cashAmount = activeWeekOrders
            .filter { it.paymentTypeRaw == 0 } // 0 = cash
            .sumOf { it.total }
        val revenueComparison = RevenueComparison(
            transferAmount = transferAmount,
            cashAmount = cashAmount
        )
        
        // สินค้า Top ของสัปดาห์ (จาก order items)
        data class ProductAgg(
            var name: String,
            var amount: Double,
            var cost: Double,
            var quantity: Int,
            var productId: String?
        )
        
        val productMap = mutableMapOf<String, ProductAgg>()
        for (order in activeWeekOrders) {
            val items = orderRepository.getOrderItems(order.id)
            items.forEach { item ->
                val key = item.productId ?: item.productName
                val agg = productMap.getOrPut(key) {
                    ProductAgg(
                        name = item.productName,
                        amount = 0.0,
                        cost = 0.0,
                        quantity = 0,
                        productId = item.productId
                    )
                }
                agg.amount += item.totalPrice
                agg.cost += (item.unitCost ?: 0.0) * item.quantity
                agg.quantity += item.quantity
            }
        }
        
        // สินค้าที่ทำรายได้สูงสุด: เรียงตามยอดขายเต็ม (ไม่หักต้นทุน)
        val topByRevenue = productMap.entries
            .map { (k, v) -> k to v.amount }
            .sortedByDescending { it.second }
            .take(3)
        val revenueList = topByRevenue.map { (key, _) -> key to productMap[key]!! }
        val maxRevenue = revenueList.maxOfOrNull { it.second.amount } ?: 1.0
        val productStats = revenueList.map { (_, agg) ->
            ProductStatsData(
                name = agg.name,
                amount = agg.amount,
                quantity = agg.quantity,
                progress = (agg.amount / maxRevenue).coerceIn(0.0, 1.0)
            )
        }
        val topProducts = productMap.entries.sortedByDescending { it.value.quantity }.take(3)
        val bestSellers = topProducts.mapIndexed { index, (_, agg) ->
            val product = agg.productId?.let { productDao.getProductById(it) }
            BestSellerData(
                productName = agg.name,
                totalSales = agg.amount,
                salesCount = agg.quantity,
                imageUrl = product?.imageUrl,
                colorHex = product?.selectedColorHex,
                rank = index + 1
            )
        }
        val totalProductSalesInPeriod = productMap.values.sumOf { it.amount }
        
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                summary = summary,
                chartData = chartData,
                revenueComparison = revenueComparison,
                productStats = productStats,
                totalProductSalesInPeriod = totalProductSalesInPeriod,
                bestSellers = bestSellers
            )
        }
    }

    /**
     * ดึงข้อมูลจริงจาก Room สำหรับช่วง "1 เดือน"
     * - 1 เดือน = ตั้งแต่วันที่ 1 ของเดือน 00:00:00 จนถึงวันปัจจุบัน 23:59:59 ของเดือนนั้น
     * - กราฟผลรวมยอดขาย: แบ่งเป็น 4 ช่วง (W1 - W4) ตามวันที่ในเดือน
     * - กราฟอื่น ๆ (ช่องทาง, สินค้า, Best seller) ใช้ข้อมูลจริงของเดือนเดียวกัน
     */
    private suspend fun loadMonthDataFromRoom() {
        val ordersResult = orderRepository.getOrders().first()
        val orders = ordersResult.getOrElse { emptyList() }
        
        if (orders.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = GraphSummary(),
                    chartData = emptyList(),
                    revenueComparison = RevenueComparison(),
                    productStats = emptyList(),
                    totalProductSalesInPeriod = 0.0,
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        val calendar = Calendar.getInstance(Locale.getDefault())
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // วันที่ 1 ของเดือน 00:00:00
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.time
        
        // วันนี้ 23:59:59 (ไม่ใช่วันสุดท้ายของเดือน)
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val monthEnd = calendar.time
        
        val monthOrders = orders.filter { order ->
            order.orderDate >= monthStart && order.orderDate <= monthEnd
        }
        
        if (monthOrders.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = GraphSummary(),
                    chartData = emptyList(),
                    revenueComparison = RevenueComparison(),
                    productStats = emptyList(),
                    totalProductSalesInPeriod = 0.0,
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        // Filter out cancelled orders for sales calculation
        val activeMonthOrders = monthOrders.filter { it.statusRaw != 5 }
        val cancelledCount = monthOrders.count { it.statusRaw == 5 }
        
        // Summary (exclude cancelled orders from sales)
        val totalSales = activeMonthOrders.sumOf { it.total }
        val orderCount = activeMonthOrders.size
        
        var totalCost = 0.0
        for (order in activeMonthOrders) {
            val items = orderRepository.getOrderItems(order.id)
            totalCost += items.sumOf { (it.unitCost ?: 0.0) * it.quantity }
        }
        
        val summary = GraphSummary(
            todaySales = totalSales, // ใช้ field นี้เป็นยอดรวมของช่วงที่เลือก
            costOfExpenses = totalCost,
            ordersToday = orderCount,
            cancelledOrders = cancelledCount,
            totalSales = totalSales
        )
        
        // กราฟ W1-W4 แบ่งตามสัปดาห์ภายในเดือน (exclude cancelled)
        // แบ่งเดือนเป็น 4 ช่วงตามจำนวนวันในเดือน
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysPerWeek = daysInMonth / 4.0
        
        val salesByWeekIndex = DoubleArray(4) { 0.0 }
        activeMonthOrders.forEach { order ->
            calendar.time = order.orderDate
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            var weekIndex = ((dayOfMonth - 1) / daysPerWeek).toInt()
            if (weekIndex > 3) weekIndex = 3
            salesByWeekIndex[weekIndex] += order.total
        }
        
        val chartData = listOf(
            ChartDataPoint(context.getString(R.string.graph_week_1), salesByWeekIndex[0]),
            ChartDataPoint(context.getString(R.string.graph_week_2), salesByWeekIndex[1]),
            ChartDataPoint(context.getString(R.string.graph_week_3), salesByWeekIndex[2]),
            ChartDataPoint(context.getString(R.string.graph_week_4), salesByWeekIndex[3])
        )
        
        // ยอดตามช่องทางของทั้งเดือน (exclude cancelled)
        val transferAmount = activeMonthOrders
            .filter { it.paymentTypeRaw == 1 }
            .sumOf { it.total }
        val cashAmount = activeMonthOrders
            .filter { it.paymentTypeRaw == 0 }
            .sumOf { it.total }
        val revenueComparison = RevenueComparison(
            transferAmount = transferAmount,
            cashAmount = cashAmount
        )
        
        // สินค้า Top ของเดือน
        data class ProductAggMonth(
            var name: String,
            var amount: Double,
            var cost: Double,
            var quantity: Int,
            var productId: String?
        )
        
        val productMap = mutableMapOf<String, ProductAggMonth>()
        for (order in activeMonthOrders) {
            val items = orderRepository.getOrderItems(order.id)
            items.forEach { item ->
                val key = item.productId ?: item.productName
                val agg = productMap.getOrPut(key) {
                    ProductAggMonth(
                        name = item.productName,
                        amount = 0.0,
                        cost = 0.0,
                        quantity = 0,
                        productId = item.productId
                    )
                }
                agg.amount += item.totalPrice
                agg.cost += (item.unitCost ?: 0.0) * item.quantity
                agg.quantity += item.quantity
            }
        }
        
        // สินค้าที่ทำรายได้สูงสุด: เรียงตามยอดขายเต็ม (ไม่หักต้นทุน)
        val topByRevenue = productMap.entries
            .map { (k, v) -> k to v.amount }
            .sortedByDescending { it.second }
            .take(3)
        val revenueList = topByRevenue.map { (key, _) -> key to productMap[key]!! }
        val maxRevenue = revenueList.maxOfOrNull { it.second.amount } ?: 1.0
        val productStats = revenueList.map { (_, agg) ->
            ProductStatsData(
                name = agg.name,
                amount = agg.amount,
                quantity = agg.quantity,
                progress = (agg.amount / maxRevenue).coerceIn(0.0, 1.0)
            )
        }
        val topProducts = productMap.entries.sortedByDescending { it.value.quantity }.take(3)
        val bestSellers = topProducts.mapIndexed { index, (_, agg) ->
            val product = agg.productId?.let { productDao.getProductById(it) }
            BestSellerData(
                productName = agg.name,
                totalSales = agg.amount,
                salesCount = agg.quantity,
                imageUrl = product?.imageUrl,
                colorHex = product?.selectedColorHex,
                rank = index + 1
            )
        }
        val totalProductSalesInPeriod = productMap.values.sumOf { it.amount }
        
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                summary = summary,
                chartData = chartData,
                revenueComparison = revenueComparison,
                productStats = productStats,
                totalProductSalesInPeriod = totalProductSalesInPeriod,
                bestSellers = bestSellers
            )
        }
    }

    /**
     * โหลดข้อมูลสำหรับช่วงวันที่กำหนดเอง (Custom)
     * - ใช้ startMillis / endMillis เป็นกรอบเวลา
     * - กราฟผลรวมยอดขาย แบ่งเป็นบัคเก็ตสัปดาห์ละ 7 วันจากวันเริ่มต้น
     */
    private suspend fun loadCustomRangeData(startMillis: Long, endMillis: Long) {
        val ordersResult = orderRepository.getOrders().first()
        val orders = ordersResult.getOrElse { emptyList() }
        if (orders.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = GraphSummary(),
                    chartData = emptyList(),
                    revenueComparison = RevenueComparison(),
                    productStats = emptyList(),
                    totalProductSalesInPeriod = 0.0,
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        // เริ่มต้น = ต้นวัน 00:00:00, สิ้นสุด = ปลายวัน 23:59:59 (รวมทั้งวันของวันสิ้นสุด)
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = startMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startDate = cal.time
        cal.timeInMillis = endMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endDate = cal.time
        
        val rangeOrders = orders.filter { order ->
            order.orderDate >= startDate && order.orderDate <= endDate
        }
        
        if (rangeOrders.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = GraphSummary(),
                    chartData = emptyList(),
                    revenueComparison = RevenueComparison(),
                    productStats = emptyList(),
                    totalProductSalesInPeriod = 0.0,
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        // Filter out cancelled orders for sales calculation
        val activeRangeOrders = rangeOrders.filter { it.statusRaw != 5 }
        val cancelledCount = rangeOrders.count { it.statusRaw == 5 }
        
        // Summary (exclude cancelled orders from sales)
        val totalSales = activeRangeOrders.sumOf { it.total }
        val orderCount = activeRangeOrders.size
        
        var totalCost = 0.0
        for (order in activeRangeOrders) {
            val items = orderRepository.getOrderItems(order.id)
            totalCost += items.sumOf { (it.unitCost ?: 0.0) * it.quantity }
        }
        
        val summary = GraphSummary(
            todaySales = totalSales,
            costOfExpenses = totalCost,
            ordersToday = orderCount,
            cancelledOrders = cancelledCount,
            totalSales = totalSales
        )
        
        // กราฟเป็นช่วงสัปดาห์ (W-like) โดยใช้วันที่เริ่มต้นเป็นจุดอ้างอิง (exclude cancelled)
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val bucketStart = calendar.time
        
        val buckets = mutableListOf<Pair<java.util.Date, java.util.Date>>()
        var currentStart = bucketStart
        while (currentStart <= endDate) {
            val cal = Calendar.getInstance().apply { time = currentStart }
            cal.add(Calendar.DAY_OF_YEAR, 6)
            var currentEnd = cal.time
            if (currentEnd > endDate) currentEnd = endDate
            buckets.add(currentStart to currentEnd)
            
            cal.time = currentEnd
            cal.add(Calendar.DAY_OF_YEAR, 1)
            currentStart = cal.time
        }
        
        val chartData = buckets.map { (bStart, bEnd) ->
            val bucketSales = activeRangeOrders.filter { order ->
                order.orderDate >= bStart && order.orderDate <= bEnd
            }.sumOf { it.total }
            
            val labelFormat = java.text.SimpleDateFormat("dd/MM", Locale.getDefault())
            ChartDataPoint(labelFormat.format(bStart), bucketSales)
        }
        
        // ช่องทางชำระเงินทั้งช่วง (exclude cancelled)
        val transferAmount = activeRangeOrders
            .filter { it.paymentTypeRaw == 1 }
            .sumOf { it.total }
        val cashAmount = activeRangeOrders
            .filter { it.paymentTypeRaw == 0 }
            .sumOf { it.total }
        val revenueComparison = RevenueComparison(
            transferAmount = transferAmount,
            cashAmount = cashAmount
        )
        
        // Top products ในช่วง custom (เหมือน month)
        data class ProductAggCustom(
            var name: String,
            var amount: Double,
            var cost: Double,
            var quantity: Int,
            var productId: String?
        )
        
        val productMap = mutableMapOf<String, ProductAggCustom>()
        for (order in activeRangeOrders) {
            val items = orderRepository.getOrderItems(order.id)
            items.forEach { item ->
                val key = item.productId ?: item.productName
                val agg = productMap.getOrPut(key) {
                    ProductAggCustom(
                        name = item.productName,
                        amount = 0.0,
                        cost = 0.0,
                        quantity = 0,
                        productId = item.productId
                    )
                }
                agg.amount += item.totalPrice
                agg.cost += (item.unitCost ?: 0.0) * item.quantity
                agg.quantity += item.quantity
            }
        }
        
        // สินค้าที่ทำรายได้สูงสุด: เรียงตามยอดขายเต็ม (ไม่หักต้นทุน)
        val topByRevenue = productMap.entries
            .map { (k, v) -> k to v.amount }
            .sortedByDescending { it.second }
            .take(3)
        val revenueList = topByRevenue.map { (key, _) -> key to productMap[key]!! }
        val maxRevenue = revenueList.maxOfOrNull { it.second.amount } ?: 1.0
        val productStats = revenueList.map { (_, agg) ->
            ProductStatsData(
                name = agg.name,
                amount = agg.amount,
                quantity = agg.quantity,
                progress = (agg.amount / maxRevenue).coerceIn(0.0, 1.0)
            )
        }
        val topProducts = productMap.entries.sortedByDescending { it.value.quantity }.take(3)
        val bestSellers = topProducts.mapIndexed { index, (_, agg) ->
            val product = agg.productId?.let { productDao.getProductById(it) }
            BestSellerData(
                productName = agg.name,
                totalSales = agg.amount,
                salesCount = agg.quantity,
                imageUrl = product?.imageUrl,
                colorHex = product?.selectedColorHex,
                rank = index + 1
            )
        }
        val totalProductSalesInPeriod = productMap.values.sumOf { it.amount }
        
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                summary = summary,
                chartData = chartData,
                revenueComparison = revenueComparison,
                productStats = productStats,
                totalProductSalesInPeriod = totalProductSalesInPeriod,
                bestSellers = bestSellers
            )
        }
    }
}

