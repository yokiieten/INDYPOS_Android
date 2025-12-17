package com.indybrain.indypos_Android.presentation.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val orderRepository: OrderRepository
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
                    TimePeriod.Week,
                    TimePeriod.Month,
                    TimePeriod.Custom -> {
                        // TODO: ค่อยขยายให้รองรับช่วงสัปดาห์/เดือนจาก Room ในภายหลัง
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
}

