package com.indybrain.indypos_Android.presentation.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.config.AppConfig
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: GraphViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showCustomRangeSheet by rememberSaveable { mutableStateOf(false) }
    var customStartDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var customEndDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    // Fetch orders from API when screen first opens, then save to Room and display
    LaunchedEffect(Unit) {
        viewModel.refreshOrdersFromApi()
    }
    
    // รีโหลดข้อมูลทุกครั้งที่เปิดหน้ากราฟ (ตามช่วงเวลาที่เลือกปัจจุบัน)
    LaunchedEffect(uiState.selectedPeriod, uiState.customStartDateMillis, uiState.customEndDateMillis) {
        viewModel.refreshCurrentPeriod()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
            // Date selector
            TimePeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                customRangeLabel = if (
                    uiState.selectedPeriod == TimePeriod.Custom &&
                    uiState.customStartDateMillis != null &&
                    uiState.customEndDateMillis != null
                ) {
                    formatCustomRange(
                        startMillis = uiState.customStartDateMillis,
                        endMillis = uiState.customEndDateMillis
                    )
                } else null,
                onPeriodSelected = { period ->
                    if (period == TimePeriod.Custom) {
                        // preload current range from uiState หากมี
                        customStartDateMillis = uiState.customStartDateMillis
                        customEndDateMillis = uiState.customEndDateMillis
                        showCustomRangeSheet = true
                    } else {
                        viewModel.selectPeriod(period)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary section
            Text(
                text = stringResource(id = R.string.graph_summary_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            SummaryCardsSection(
                summary = uiState.summary,
                selectedPeriod = uiState.selectedPeriod
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Graph section
            Text(
                text = stringResource(id = R.string.graph_chart_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ChartCard(
                totalSales = uiState.summary.totalSales,
                chartData = uiState.chartData
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Revenue Comparison section
            RevenueComparisonCard(
                revenueComparison = uiState.revenueComparison
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Product Stats section
            ProductStatsCard(
                productStats = uiState.productStats
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Best Seller section
            BestSellerCard(
                bestSellers = uiState.bestSellers
            )
    }
    
    if (showCustomRangeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        
        // Initialize default dates (current month) when sheet opens and dates are null
        LaunchedEffect(showCustomRangeSheet) {
            if (customStartDateMillis == null || customEndDateMillis == null) {
                val calendar = Calendar.getInstance()
                // Set start date to first day of current month
                val startCalendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // Set end date to last day of current month
                val endCalendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                if (customStartDateMillis == null) {
                    customStartDateMillis = startCalendar.timeInMillis
                }
                if (customEndDateMillis == null) {
                    customEndDateMillis = endCalendar.timeInMillis
                }
            }
        }
        
        fun openDatePicker(isStart: Boolean) {
            val calendar = Calendar.getInstance()
            val currentMillis = if (isStart) customStartDateMillis else customEndDateMillis
            if (currentMillis != null) {
                calendar.timeInMillis = currentMillis
            }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    val cal = Calendar.getInstance().apply {
                        set(y, m, d, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (isStart) {
                        customStartDateMillis = cal.timeInMillis
                    } else {
                        customEndDateMillis = cal.timeInMillis
                    }
                },
                year,
                month,
                day
            ).show()
        }
        
        ModalBottomSheet(
            onDismissRequest = { showCustomRangeSheet = false },
            sheetState = sheetState
        ) {
            val context = LocalContext.current
            val locale = LocaleHelper.getCurrentLocale(context)
            val buddhistEraLabel = stringResource(id = R.string.graph_date_buddhist_era)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.graph_custom_range_cancel),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton,
                        modifier = Modifier.clickable {
                            showCustomRangeSheet = false
                        }
                    )
                    Text(
                        text = stringResource(id = R.string.graph_custom_range_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                    Text(
                        text = stringResource(id = R.string.graph_custom_range_done),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryButton,
                        modifier = Modifier.clickable {
                            if (customStartDateMillis != null && customEndDateMillis != null) {
                                showCustomRangeSheet = false
                                viewModel.setCustomRange(
                                    customStartDateMillis!!,
                                    customEndDateMillis!!
                                )
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(id = R.string.graph_custom_range_warning),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Small
                    ),
                    color = Color(0xFFFF9500),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.graph_custom_range_start_date),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp))
                            .clickable { openDatePicker(isStart = true) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = formatCustomDate(customStartDateMillis, locale, buddhistEraLabel),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                    }
                    
                    Text(
                        text = stringResource(id = R.string.graph_custom_range_end_date),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFFF2F2F7), RoundedCornerShape(10.dp))
                            .clickable { openDatePicker(isStart = false) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = formatCustomDate(customEndDateMillis, locale, buddhistEraLabel),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = PrimaryText
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    customRangeLabel: String? = null,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Surface(
            modifier = Modifier
                .clickable { expanded = true }
                .width(120.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFE8F4FD)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selectedPeriod == TimePeriod.Custom && !customRangeLabel.isNullOrBlank()) {
                        customRangeLabel
                    } else {
                        stringResource(id = selectedPeriod.stringResId)
                    },
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Small
                    ),
                    color = PrimaryText
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            Text(
                text = stringResource(id = R.string.graph_select_period),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Small
                ),
                color = PrimaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            TimePeriod.entries.forEach { period ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = period.stringResId),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                    },
                    onClick = {
                        onPeriodSelected(period)
                        expanded = false
                    },
                    trailingIcon = if (period == selectedPeriod) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = PrimaryButton,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

private fun formatCustomDate(millis: Long?, locale: Locale, buddhistEraLabel: String): String {
    if (millis == null) return ""
    val calendar = Calendar.getInstance().apply {
        timeInMillis = millis
    }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val monthFormat = SimpleDateFormat("MMM", locale)
    val monthStr = monthFormat.format(calendar.time)
    val yearBE = calendar.get(Calendar.YEAR) + 543
    return String.format("%02d %s %s %d", day, monthStr, buddhistEraLabel, yearBE)
}

@Composable
private fun formatCustomRange(startMillis: Long?, endMillis: Long?): String {
    if (startMillis == null || endMillis == null) return stringResource(id = TimePeriod.Custom.stringResId)
    val context = LocalContext.current
    val locale = LocaleHelper.getCurrentLocale(context)
    val sdf = SimpleDateFormat("dd/MM/yyyy", locale)
    val start = java.util.Date(minOf(startMillis, endMillis))
    val end = java.util.Date(maxOf(startMillis, endMillis))
    return "${sdf.format(start)} - ${sdf.format(end)}"
}

@Composable
private fun SummaryCardsSection(
    summary: GraphSummary,
    selectedPeriod: TimePeriod
) {
    // Get dynamic labels based on selected period
    val salesLabel = when (selectedPeriod) {
        TimePeriod.Today -> stringResource(id = R.string.graph_today_sales)
        TimePeriod.Week -> stringResource(id = R.string.graph_week_sales)
        TimePeriod.Month -> stringResource(id = R.string.graph_month_sales)
        TimePeriod.Custom -> stringResource(id = R.string.graph_period_sales)
    }
    
    val ordersLabel = when (selectedPeriod) {
        TimePeriod.Today -> stringResource(id = R.string.graph_orders_today)
        TimePeriod.Week -> stringResource(id = R.string.graph_orders_week)
        TimePeriod.Month -> stringResource(id = R.string.graph_orders_month)
        TimePeriod.Custom -> stringResource(id = R.string.graph_orders_period)
    }
    
    val profitLabel = when (selectedPeriod) {
        TimePeriod.Today -> stringResource(id = R.string.graph_profit_today)
        TimePeriod.Week -> stringResource(id = R.string.graph_profit_week)
        TimePeriod.Month -> stringResource(id = R.string.graph_profit_month)
        TimePeriod.Custom -> stringResource(id = R.string.graph_profit_period)
    }
    
    val profitValue = summary.todaySales - summary.costOfExpenses
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Sales and Cost
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = salesLabel,
                value = formatCurrency(summary.todaySales),
                valueColor = GreenComplete,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = stringResource(id = R.string.graph_cost_expenses),
                value = formatCurrency(summary.costOfExpenses),
                valueColor = RedFailure,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Profit row: กำไร (ยอดขายหักต้นทุน)
        SummaryCard(
            title = profitLabel,
            value = formatCurrency(profitValue),
            valueColor = if (profitValue >= 0) GreenComplete else RedFailure,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Middle row: ออเดอร์ตามช่วงเวลา (เหมือนเดิม) + ทั้งหมด
        SummaryCard(
            title = "$ordersLabel ${stringResource(id = R.string.order_filter_all)}",
            value = "${summary.ordersToday + summary.cancelledOrders} ${stringResource(id = R.string.home_orders_unit)}",
            valueColor = GreenComplete,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Bottom row: ออเดอร์สำเร็จ (กรองแค่สำเร็จตามช่วง) และ ออเดอร์ยกเลิก
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = stringResource(id = R.string.graph_orders_success),
                value = "${summary.ordersToday} ${stringResource(id = R.string.home_orders_unit)}",
                valueColor = GreenComplete,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = stringResource(id = R.string.graph_cancelled_orders),
                value = "${summary.cancelledOrders} ${stringResource(id = R.string.home_orders_unit)}",
                valueColor = RedFailure,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F4FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = SecondaryText
            )
            Text(
                text = value,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = valueColor
            )
        }
    }
}

@Composable
private fun ChartCard(
    totalSales: Double,
    chartData: List<ChartDataPoint>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.graph_total_sales),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${formatCurrency(totalSales)} ${stringResource(id = R.string.home_currency_suffix)}",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (chartData.isNotEmpty()) {
                val scrollState = rememberScrollState()
                val configuration = LocalConfiguration.current
                val screenWidth = configuration.screenWidthDp.dp
                val contentWidth = maxOf(40.dp * 24, screenWidth)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .horizontalScroll(scrollState)
                ) {
                    LineChart(
                        data = chartData,
                        modifier = Modifier
                            .height(200.dp)
                            .width(contentWidth)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.graph_no_data_available),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = PlaceholderText
                    )
                }
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val context = LocalContext.current
    val currencySymbol = stringResource(id = R.string.graph_currency_symbol)
    
    val maxValue = data.maxOfOrNull { it.value } ?: 1.0
    val minValue = data.minOfOrNull { it.value } ?: 0.0
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)
    
    val padding = 40.dp
    val chartColor = PrimaryButton
    val gridColor = Color(0xFFE5E5E5)
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    Canvas(
        modifier = modifier
            .pointerInput(data) {
                if (data.isEmpty()) return@pointerInput
                detectTapGestures { tapOffset ->
                    // คำนวณจุดในกราฟเพื่อหา point ที่ใกล้กับตำแหน่งที่แตะที่สุด
                    val width = size.width
                    val height = size.height
                    val paddingPx = padding.toPx()
                    val chartWidth = width - paddingPx * 2
                    val chartHeight = height - paddingPx * 2
                    val startX = paddingPx
                    val startY = paddingPx
                    val endY = startY + chartHeight
                    
                    val points = data.mapIndexed { index, point ->
                        val x = startX + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
                        val normalizedValue = ((point.value - minValue) / valueRange).coerceIn(0.0, 1.0)
                        val y = endY - (chartHeight * normalizedValue.toFloat())
                        Offset(x, y)
                    }
                    
                    // หา point ที่ใกล้ตำแหน่งที่แตะที่สุด
                    val nearestIndex = points.indices.minByOrNull { i ->
                        val dx = points[i].x - tapOffset.x
                        val dy = points[i].y - tapOffset.y
                        dx * dx + dy * dy
                    }
                    selectedIndex = nearestIndex
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val chartWidth = width - padding.toPx() * 2
        val chartHeight = height - padding.toPx() * 2
        val startX = padding.toPx()
        val startY = padding.toPx()
        val endX = startX + chartWidth
        val endY = startY + chartHeight
        
        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = startY + (chartHeight / gridLines) * i
            drawLine(
                color = gridColor,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw Y-axis labels
        for (i in 0..gridLines) {
            val value = maxValue - (valueRange / gridLines) * i
            val y = startY + (chartHeight / gridLines) * i
            val label = if (value >= 1000) {
                "${(value / 1000).toInt()}k"
            } else {
                formatCurrency(value)
            }
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#999999")
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawText(label, startX - 8.dp.toPx(), y + 4.dp.toPx(), paint)
            }
        }
        
        // Calculate points
        val points = data.mapIndexed { index, point ->
            val x = startX + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
            val normalizedValue = ((point.value - minValue) / valueRange).coerceIn(0.0, 1.0)
            val y = endY - (chartHeight * normalizedValue.toFloat())
            Offset(x, y)
        }
        
        // Draw area under line
        if (points.size > 1) {
            val areaPath = Path().apply {
                moveTo(points[0].x, endY)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, endY)
                close()
            }
            
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5EA6ED).copy(alpha = 0.3f),
                        Color(0xFF5EA6ED).copy(alpha = 0.1f)
                    ),
                    startY = endY,
                    endY = points.minOfOrNull { it.y } ?: endY
                )
            )
        }
        
        // Draw line
        if (points.size > 1) {
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = chartColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        
        // Draw points
        points.forEachIndexed { index, point ->
            val isSelected = selectedIndex == index
            val radiusOuter = if (isSelected) 8.dp.toPx() else 6.dp.toPx()
            val radiusInner = if (isSelected) 4.dp.toPx() else 3.dp.toPx()
            
            drawCircle(
                color = chartColor,
                radius = radiusOuter,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = radiusInner,
                center = point
            )
        }
        
        // Draw tooltip for selected point
        selectedIndex?.let { index ->
            if (index in points.indices) {
                val point = points[index]
                val value = data[index].value
                val label = "$currencySymbol${formatCurrency(value)}"
                
                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 12.dp.toPx()
                        isAntiAlias = true
                    }
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        isAntiAlias = true
                    }
                    
                    val textWidth = textPaint.measureText(label)
                    val textHeight = textPaint.fontMetrics.run { bottom - top }
                    val paddingPx = 8.dp.toPx()
                    
                    val rectLeft = point.x - textWidth / 2f - paddingPx
                    val rectTop = point.y - 32.dp.toPx() - textHeight - paddingPx * 2
                    val rectRight = rectLeft + textWidth + paddingPx * 2
                    val rectBottom = rectTop + textHeight + paddingPx * 2
                    
                    val rect = android.graphics.RectF(
                        rectLeft,
                        rectTop,
                        rectRight,
                        rectBottom
                    )
                    
                    drawRoundRect(rect, 16f, 16f, bgPaint)
                    drawText(
                        label,
                        rect.left + paddingPx,
                        rect.bottom - paddingPx,
                        textPaint
                    )
                }
            }
        }
        
        // Draw X-axis labels
        data.forEachIndexed { index, point ->
            val x = startX + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#999999")
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(point.time, x, endY + 20.dp.toPx(), paint)
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(value)
}

@Composable
private fun RevenueComparisonCard(
    revenueComparison: RevenueComparison
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.graph_revenue_comparison_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val total = revenueComparison.transferAmount + revenueComparison.cashAmount
            Text(
                text = "${formatCurrency(total)} ${stringResource(id = R.string.graph_currency_baht)}",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Donut chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                if (total > 0) {
                    DonutChart(
                        transferAmount = revenueComparison.transferAmount,
                        cashAmount = revenueComparison.cashAmount,
                        modifier = Modifier.size(120.dp)
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.graph_no_data_available),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = PlaceholderText
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(
                    color = PrimaryButton,
                    title = stringResource(id = R.string.graph_transfer_payment),
                    amount = revenueComparison.transferAmount
                )
                LegendItem(
                    color = GreenComplete,
                    title = stringResource(id = R.string.graph_cash_payment),
                    amount = revenueComparison.cashAmount
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    transferAmount: Double,
    cashAmount: Double,
    modifier: Modifier = Modifier
) {
    val total = transferAmount + cashAmount
    if (total <= 0) return
    
    val transferAngle = ((transferAmount / total) * 360.0).toFloat()
    val cashAngle = ((cashAmount / total) * 360.0).toFloat()
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 20.dp.toPx()
        val strokeWidth = 20.dp.toPx()
        
        // Background circle
        drawCircle(
            color = Color(0xFFE5E5E5),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        
        // Transfer arc (blue)
        if (transferAmount > 0) {
            drawArc(
                color = PrimaryButton,
                startAngle = -90f,
                sweepAngle = transferAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        
        // Cash arc (green)
        if (cashAmount > 0) {
            drawArc(
                color = GreenComplete,
                startAngle = -90f + transferAngle,
                sweepAngle = cashAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    title: String,
    amount: Double
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(60.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = FontUtils.mainFont(
                style = AppFontStyle.Medium,
                size = FontSize.Small
            ),
            color = SecondaryText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${formatCurrency(amount)} ${stringResource(id = R.string.graph_currency_baht)}",
            style = FontUtils.mainFont(
                style = AppFontStyle.Bold,
                size = FontSize.Medium
            ),
            color = PrimaryText
        )
    }
}

@Composable
private fun ProductStatsCard(
    productStats: List<ProductStatsData>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.graph_product_stats_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val totalAmount = productStats.sumOf { it.amount }
            Text(
                text = "${formatCurrency(totalAmount)} ${stringResource(id = R.string.graph_currency_baht)}",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (productStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.graph_no_data_available),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = PlaceholderText
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    productStats.take(3).forEachIndexed { index, product ->
                        ProductStatItem(
                            product = product,
                            rank = index + 1,
                            totalAmount = totalAmount
                        )
                    }
                    repeat(3 - productStats.size.coerceAtMost(3)) {
                        EmptyProductStatItem()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductStatItem(
    product: ProductStatsData,
    rank: Int,
    totalAmount: Double
) {
    val sharePercent = if (totalAmount > 0) (product.amount / totalAmount * 100) else 0.0
    val progressFraction = if (totalAmount > 0) {
        (product.amount / totalAmount).coerceIn(0.05, 1.0)
    } else {
        0.0
    }
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> PrimaryButton
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(rankColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Small
                    ),
                    color = Color.White
                )
            }
            Text(
                text = product.name,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier.weight(1f)
            )
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${formatCurrency(product.amount)} ${stringResource(id = R.string.graph_currency_baht)}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = GreenComplete
                )
                Text(
                    text = "${product.quantity} ${stringResource(id = R.string.stock_unit_piece)}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", sharePercent),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = SecondaryText
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar เต็มแถว ใต้เปอร์เซ็นต์ (ให้ layout ใกล้เคียงตัวอย่าง)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFFE5E5E5), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction.toFloat())
                    .background(PrimaryButton, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun EmptyProductStatItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFE5E5E5), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "-",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Medium
                ),
                color = PlaceholderText
            )
        }
        Text(
            text = "-",
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Medium
            ),
            color = PlaceholderText,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "-",
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Small
            ),
            color = PlaceholderText
        )
    }
}

@Composable
private fun BestSellerCard(
    bestSellers: List<BestSellerData>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.graph_bestseller_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (bestSellers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.graph_no_bestseller),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = PlaceholderText
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Show top 3 best sellers
                    bestSellers.take(3).forEach { seller ->
                        BestSellerItem(seller = seller)
                    }
                    
                    // Fill remaining slots if less than 3
                    repeat(3 - bestSellers.size.coerceAtMost(3)) {
                        EmptyBestSellerItem()
                    }
                }
            }
        }
    }
}

@Composable
private fun BestSellerItem(
    seller: BestSellerData
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Product image
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            val backgroundColor = seller.colorHex?.let {
                try {
                    Color(android.graphics.Color.parseColor(it))
                } catch (e: Exception) {
                    Color(0xFFE0E0E0)
                }
            } ?: Color(0xFFE0E0E0)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor, RoundedCornerShape(8.dp))
            ) {
                val imageUrl = seller.imageUrl?.takeIf { it.isNotBlank() }
                
                if (!imageUrl.isNullOrBlank()) {
                    val fullImageUrl = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                        imageUrl
                    } else {
                        AppConfig.buildImageUrl(imageUrl)
                    }
                    
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fullImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = seller.productName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.logo_appstore),
                        placeholder = painterResource(id = R.drawable.logo_appstore)
                    )
                } else if (seller.colorHex != null) {
                    // Just show background color
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo_appstore),
                        contentDescription = seller.productName,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            
            // Rank badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(30.dp)
                    .background(
                        when (seller.rank) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> PrimaryButton
                        },
                        RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${seller.rank}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Small
                    ),
                    color = Color.White
                )
            }
            
            // Crown icon for rank 1 (iOS style - top right)
            if (seller.rank == 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(24.dp)
                        .background(
                            Color(0xFFFFD700), // Gold color
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Crown",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Product info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = seller.productName,
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${formatCurrency(seller.totalSales)} ${stringResource(id = R.string.graph_currency_baht)}",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Small
                ),
                color = GreenComplete
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = "${seller.salesCount} ${stringResource(id = R.string.home_orders_unit)}",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun EmptyBestSellerItem() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFFE5E5E5), RoundedCornerShape(8.dp))
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "-",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PlaceholderText
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "-",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Small
                ),
                color = PlaceholderText
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = "-",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Small
                ),
                color = PlaceholderText
            )
        }
    }
}

