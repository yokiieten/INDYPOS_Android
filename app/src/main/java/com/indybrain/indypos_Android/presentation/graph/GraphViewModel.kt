package com.indybrain.indypos_Android.presentation.graph

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.domain.repository.GraphRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val graphRepository: GraphRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState(isLoading = true))
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
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
            loadDataFromApi(TimePeriod.Custom, normalizedStart, normalizedEnd)
        }
    }

    /**
     * Load graph data from Graph Dashboard API
     */
    private fun loadData(period: TimePeriod) {
        viewModelScope.launch {
            when (period) {
                TimePeriod.Today, TimePeriod.Week, TimePeriod.Month -> {
                    loadDataFromApi(period, null, null)
                }
                TimePeriod.Custom -> {
                    val start = _uiState.value.customStartDateMillis
                    val end = _uiState.value.customEndDateMillis
                    if (start != null && end != null) {
                        loadDataFromApi(period, start, end)
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
        }
    }

    private suspend fun loadDataFromApi(
        period: TimePeriod,
        startDateMillis: Long?,
        endDateMillis: Long?
    ) {
        graphRepository.getDashboard(
            filterType = period,
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis
        ).fold(
            onSuccess = { result ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        summary = result.summary,
                        chartData = result.chartData,
                        revenueComparison = result.revenueComparison,
                        productStats = result.productStats,
                        totalProductSalesInPeriod = result.totalProductSalesInPeriod,
                        bestSellers = result.bestSellers,
                        errorMessage = null
                    )
                }
            },
            onFailure = { e ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = e.message ?: context.getString(R.string.graph_error_loading)
                    )
                }
            }
        )
    }

    /**
     * รีโหลดข้อมูลตามช่วงเวลาที่เลือกปัจจุบัน จาก API
     */
    fun refreshCurrentPeriod() {
        val currentPeriod = _uiState.value.selectedPeriod
        _uiState.update { it.copy(isLoading = true) }
        loadData(currentPeriod)
    }
}
