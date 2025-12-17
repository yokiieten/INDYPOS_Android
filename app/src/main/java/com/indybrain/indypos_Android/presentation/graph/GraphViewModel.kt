package com.indybrain.indypos_Android.presentation.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.data.local.dao.ProductDao
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val productDao: ProductDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GraphUiState(isLoading = true))
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()
    
    init {
        loadData(TimePeriod.Today)
    }
    
    fun selectPeriod(period: TimePeriod) {
        _uiState.update { current ->
            current.copy(selectedPeriod = period, isLoading = true)
        }
        loadData(period)
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
                        // TODO: รองรับช่วงกำหนดเองจาก Room ในภายหลัง
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                summary = GraphSummary(),
                                chartData = emptyList(),
                                revenueComparison = RevenueComparison(),
                                productStats = emptyList(),
                                bestSellers = emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "เกิดข้อผิดพลาดในการโหลดข้อมูลกราฟ"
                    )
                }
            }
        }
    }
    
    /**
     * ดึงข้อมูลจริงจาก Room สำหรับช่วง "วันนี้"
     * - todaySales: SUM(total) ของออเดอร์วันนี้
     * - costOfExpenses: SUM(unitCost * quantity) ของ items วันนี้
     * - ordersToday: จำนวนออเดอร์วันนี้
     * - cancelledOrders: (ตอนนี้ยังไม่แยก cancelled, ใช้ 0 ไปก่อน)
     */
    private suspend fun loadTodayDataFromRoom() {
        val todaySales = orderRepository.getTodaySales()
        val ordersToday = orderRepository.getTodayOrderCount()
        val costOfExpenses = orderRepository.getTodayCostOfExpenses()
        
        val summary = GraphSummary(
            todaySales = todaySales,
            costOfExpenses = costOfExpenses,
            ordersToday = ordersToday,
            cancelledOrders = 0, // ถ้าต้องการนับ cancelled แยก ค่อยเพิ่ม query เพิ่มเติมที่ OrderDao/Repository
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
        
        val revenueComparison = RevenueComparison(
            transferAmount = todaySales * 0.55,
            cashAmount = todaySales * 0.45
        )
        
        // สร้าง mock product stats / bestseller จากยอดรวมเพื่อให้มีกราฟดูง่าย ๆ
        val productStats = listOf(
            ProductStatsData("สินค้า A", todaySales * 0.4, 1.0),
            ProductStatsData("สินค้า B", todaySales * 0.35, 0.8),
            ProductStatsData("สินค้า C", todaySales * 0.25, 0.6)
        )
        
        val bestSellers = listOf(
            BestSellerData(
                productName = "สินค้า A",
                totalSales = todaySales * 0.4,
                salesCount = 10,
                imageUrl = null,
                colorHex = "#8B4513",
                rank = 1
            ),
            BestSellerData(
                productName = "สินค้า B",
                totalSales = todaySales * 0.35,
                salesCount = 8,
                imageUrl = null,
                colorHex = "#D2691E",
                rank = 2
            ),
            BestSellerData(
                productName = "สินค้า C",
                totalSales = todaySales * 0.25,
                salesCount = 6,
                imageUrl = null,
                colorHex = "#F4A460",
                rank = 3
            )
        )
        
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                summary = summary,
                chartData = chartData,
                revenueComparison = revenueComparison,
                productStats = productStats,
                bestSellers = bestSellers
            )
        }
    }

    /**
     * ดึงข้อมูลจริงจาก Room สำหรับช่วง "1 สัปดาห์"
     * - ใช้สัปดาห์ปัจจุบัน (จันทร์ - เสาร์)
     * - กราฟผลรวมยอดขาย: แสดงตามวันในสัปดาห์ (จ. อ. พ. พฤ. ศ. ส.)
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
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        val calendar = Calendar.getInstance()
        // หา Monday ของสัปดาห์ปัจจุบัน
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        val startDate = calendar.time // จันทร์
        calendar.add(Calendar.DAY_OF_YEAR, 5)
        val endDate = calendar.time   // เสาร์
        
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
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        // Summary
        val totalSales = weekOrders.sumOf { it.total }
        val orderCount = weekOrders.size
        val cancelledCount = weekOrders.count { it.statusRaw == 5 }
        
        // คำนวณต้นทุนจาก order items (unitCost * quantity)
        var totalCost = 0.0
        for (order in weekOrders) {
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
        
        // กราฟผลรวมยอดขายรายวันในสัปดาห์
        val dayKeyFormatter = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val salesByDayKey = weekOrders.groupBy { order ->
            dayKeyFormatter.format(order.orderDate)
        }.mapValues { (_, dayOrders) ->
            dayOrders.sumOf { it.total }
        }
        
        // แกน X: จ. อ. พ. พฤ. ศ. ส. (6 วันทำการหลัก)
        val dayLabels = listOf("จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.")
        val chartData = mutableListOf<ChartDataPoint>()
        calendar.time = startDate
        for (i in 0..5) {
            val label = dayLabels[i]
            val key = dayKeyFormatter.format(calendar.time)
            val value = salesByDayKey[key] ?: 0.0
            chartData.add(ChartDataPoint(label, value))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // ยอดตามช่องทาง (โอน / เงินสด) ในช่วง 1 สัปดาห์
        val transferAmount = weekOrders
            .filter { it.paymentTypeRaw == 1 } // 1 = transfer
            .sumOf { it.total }
        val cashAmount = weekOrders
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
            var quantity: Int,
            var productId: String?
        )
        
        val productMap = mutableMapOf<String, ProductAgg>()
        for (order in weekOrders) {
            val items = orderRepository.getOrderItems(order.id)
            items.forEach { item ->
                val key = item.productId ?: item.productName
                val agg = productMap.getOrPut(key) {
                    ProductAgg(
                        name = item.productName,
                        amount = 0.0,
                        quantity = 0,
                        productId = item.productId
                    )
                }
                agg.amount += item.totalPrice
                agg.quantity += item.quantity
            }
        }
        
        val topProducts = productMap
            .entries
            .sortedByDescending { it.value.amount }
            .take(3)
        
        val maxAmount = topProducts.maxOfOrNull { it.value.amount } ?: 1.0
        val productStats = topProducts.map { (_, agg) ->
            ProductStatsData(
                name = agg.name,
                amount = agg.amount,
                progress = (agg.amount / maxAmount).coerceIn(0.0, 1.0)
            )
        }
        
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
        
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                summary = summary,
                chartData = chartData,
                revenueComparison = revenueComparison,
                productStats = productStats,
                bestSellers = bestSellers
            )
        }
    }

    /**
     * ดึงข้อมูลจริงจาก Room สำหรับช่วง "1 เดือน"
     * - ใช้เดือนปัจจุบัน
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
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val monthOrders = orders.filter { order ->
            calendar.time = order.orderDate
            calendar.get(Calendar.MONTH) == currentMonth &&
                calendar.get(Calendar.YEAR) == currentYear
        }
        
        if (monthOrders.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = GraphSummary(),
                    chartData = emptyList(),
                    revenueComparison = RevenueComparison(),
                    productStats = emptyList(),
                    bestSellers = emptyList()
                )
            }
            return
        }
        
        // Summary
        val totalSales = monthOrders.sumOf { it.total }
        val orderCount = monthOrders.size
        val cancelledCount = monthOrders.count { it.statusRaw == 5 }
        
        var totalCost = 0.0
        for (order in monthOrders) {
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
        
        // กราฟ W1-W4 แบ่งตามวันที่ภายในเดือน
        val salesByWeekIndex = DoubleArray(4) { 0.0 }
        calendar.time = monthOrders.first().orderDate
        monthOrders.forEach { order ->
            calendar.time = order.orderDate
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            var weekIndex = (dayOfMonth - 1) / 7 // 0-based
            if (weekIndex > 3) weekIndex = 3
            salesByWeekIndex[weekIndex] += order.total
        }
        
        val chartData = listOf(
            ChartDataPoint("W1", salesByWeekIndex[0]),
            ChartDataPoint("W2", salesByWeekIndex[1]),
            ChartDataPoint("W3", salesByWeekIndex[2]),
            ChartDataPoint("W4", salesByWeekIndex[3])
        )
        
        // ยอดตามช่องทางของทั้งเดือน
        val transferAmount = monthOrders
            .filter { it.paymentTypeRaw == 1 }
            .sumOf { it.total }
        val cashAmount = monthOrders
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
            var quantity: Int,
            var productId: String?
        )
        
        val productMap = mutableMapOf<String, ProductAggMonth>()
        for (order in monthOrders) {
            val items = orderRepository.getOrderItems(order.id)
            items.forEach { item ->
                val key = item.productId ?: item.productName
                val agg = productMap.getOrPut(key) {
                    ProductAggMonth(
                        name = item.productName,
                        amount = 0.0,
                        quantity = 0,
                        productId = item.productId
                    )
                }
                agg.amount += item.totalPrice
                agg.quantity += item.quantity
            }
        }
        
        val topProducts = productMap
            .entries
            .sortedByDescending { it.value.amount }
            .take(3)
        
        val maxAmount = topProducts.maxOfOrNull { it.value.amount } ?: 1.0
        val productStats = topProducts.map { (_, agg) ->
            ProductStatsData(
                name = agg.name,
                amount = agg.amount,
                progress = (agg.amount / maxAmount).coerceIn(0.0, 1.0)
            )
        }
        
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
        
        _uiState.update { current ->
            current.copy(
                isLoading = false,
                summary = summary,
                chartData = chartData,
                revenueComparison = revenueComparison,
                productStats = productStats,
                bestSellers = bestSellers
            )
        }
    }
}

