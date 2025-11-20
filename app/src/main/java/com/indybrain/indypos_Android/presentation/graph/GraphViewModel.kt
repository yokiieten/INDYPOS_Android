package com.indybrain.indypos_Android.presentation.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()
    
    init {
        loadMockData()
    }
    
    fun selectPeriod(period: TimePeriod) {
        _uiState.update { current ->
            current.copy(selectedPeriod = period)
        }
        loadMockData(period)
    }
    
    private fun loadMockData(period: TimePeriod = TimePeriod.Today) {
        viewModelScope.launch {
            // Mock data based on period
            val mockSummary = when (period) {
                TimePeriod.Today -> GraphSummary(
                    todaySales = 1821.00,
                    costOfExpenses = 180.00,
                    ordersToday = 1,
                    cancelledOrders = 0,
                    totalSales = 1821.00
                )
                TimePeriod.Week -> GraphSummary(
                    todaySales = 12500.00,
                    costOfExpenses = 1200.00,
                    ordersToday = 8,
                    cancelledOrders = 1,
                    totalSales = 12500.00
                )
                TimePeriod.Month -> GraphSummary(
                    todaySales = 45000.00,
                    costOfExpenses = 4500.00,
                    ordersToday = 35,
                    cancelledOrders = 3,
                    totalSales = 45000.00
                )
                TimePeriod.Custom -> GraphSummary(
                    todaySales = 0.0,
                    costOfExpenses = 0.0,
                    ordersToday = 0,
                    cancelledOrders = 0,
                    totalSales = 0.0
                )
            }
            
            // Mock chart data - hourly sales for today
            val mockChartData = when (period) {
                TimePeriod.Today -> listOf(
                    ChartDataPoint("14:00", 0.0),
                    ChartDataPoint("15:00", 720.0),
                    ChartDataPoint("16:00", 1821.0),
                    ChartDataPoint("17:00", 1821.0),
                    ChartDataPoint("18:00", 1821.0)
                )
                TimePeriod.Week -> listOf(
                    ChartDataPoint("จันทร์", 1500.0),
                    ChartDataPoint("อังคาร", 1800.0),
                    ChartDataPoint("พุธ", 2100.0),
                    ChartDataPoint("พฤหัส", 1900.0),
                    ChartDataPoint("ศุกร์", 2200.0),
                    ChartDataPoint("เสาร์", 1800.0),
                    ChartDataPoint("อาทิตย์", 1200.0)
                )
                TimePeriod.Month -> listOf(
                    ChartDataPoint("สัปดาห์ 1", 10000.0),
                    ChartDataPoint("สัปดาห์ 2", 12000.0),
                    ChartDataPoint("สัปดาห์ 3", 11000.0),
                    ChartDataPoint("สัปดาห์ 4", 12000.0)
                )
                TimePeriod.Custom -> emptyList()
            }
            
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = mockSummary,
                    chartData = mockChartData
                )
            }
        }
    }
}

