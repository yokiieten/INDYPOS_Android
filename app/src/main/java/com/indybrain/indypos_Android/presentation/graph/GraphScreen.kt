package com.indybrain.indypos_Android.presentation.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
import com.indybrain.indypos_Android.ui.theme.SecondaryButton
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GraphScreen(
    viewModel: GraphViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomRangeSheet by rememberSaveable { mutableStateOf(false) }
    var customStartDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var customEndDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    // โหลดข้อมูลจาก API เมื่อเปิดหน้าหรือเปลี่ยนช่วงเวลา (ยิงรอบเดียว)
    LaunchedEffect(uiState.selectedPeriod, uiState.customStartDateMillis, uiState.customEndDateMillis) {
        viewModel.refreshCurrentPeriod()
    }
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
    ) {
        stickyHeader {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BaseBackground)
            ) {
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
                            customStartDateMillis = uiState.customStartDateMillis
                            customEndDateMillis = uiState.customEndDateMillis
                            showCustomRangeSheet = true
                        } else {
                            viewModel.selectPeriod(period)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
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
        }

        item {
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
        }

        item {
            RevenueComparisonCard(
                revenueComparison = uiState.revenueComparison
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            ProductStatsCard(
                productStats = uiState.productStats,
                totalProductSalesInPeriod = uiState.totalProductSalesInPeriod
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            BestSellerCard(
                bestSellers = uiState.bestSellers
            )
        }
    }
    
    if (showCustomRangeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        
        // Default: start/end เป็นวันเดียวกัน (ต้นวันปัจจุบัน) เมื่อยังไม่มีค่า
        LaunchedEffect(showCustomRangeSheet) {
            if (customStartDateMillis == null || customEndDateMillis == null) {
                val todayMillis = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                if (customStartDateMillis == null) {
                    customStartDateMillis = todayMillis
                }
                if (customEndDateMillis == null) {
                    customEndDateMillis = todayMillis
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                            text = formatCustomDate(customStartDateMillis, locale),
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
                            text = formatCustomDate(customEndDateMillis, locale),
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
    
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .wrapContentWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(28.dp),
            color = PrimaryButton,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
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
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
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

private fun formatCustomDate(millis: Long?, locale: Locale): String {
    if (millis == null) return ""
    val calendar = Calendar.getInstance().apply {
        timeInMillis = millis
    }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val monthFormat = SimpleDateFormat("MMM", locale)
    val monthStr = monthFormat.format(calendar.time)
    val isThai = locale.language == "th"
    val year = if (isThai) calendar.get(Calendar.YEAR) + 543 else calendar.get(Calendar.YEAR)
    return String.format("%02d %s %d", day, monthStr, year)
}

@Composable
private fun formatCustomRange(startMillis: Long?, endMillis: Long?): String {
    if (startMillis == null || endMillis == null) return stringResource(id = TimePeriod.Custom.stringResId)
    val context = LocalContext.current
    val locale = LocaleHelper.getCurrentLocale(context)
    val isThai = locale.language == "th"
    val (startCal, endCal) = Pair(
        Calendar.getInstance().apply { timeInMillis = minOf(startMillis, endMillis) },
        Calendar.getInstance().apply { timeInMillis = maxOf(startMillis, endMillis) }
    )
    val dayMonthFormat = SimpleDateFormat("dd/MM", locale)
    val startYear = if (isThai) startCal.get(Calendar.YEAR) + 543 else startCal.get(Calendar.YEAR)
    val endYear = if (isThai) endCal.get(Calendar.YEAR) + 543 else endCal.get(Calendar.YEAR)
    val startStr = "${dayMonthFormat.format(startCal.time)}/$startYear"
    val endStr = "${dayMonthFormat.format(endCal.time)}/$endYear"
    return "$startStr - $endStr"
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
                value = "${formatCurrency(summary.todaySales)} ${stringResource(id = R.string.graph_currency_baht)}",
                valueColor = GreenComplete,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = stringResource(id = R.string.graph_cost_expenses),
                value = "${formatCurrency(summary.costOfExpenses)} ${stringResource(id = R.string.graph_currency_baht)}",
                valueColor = RedFailure,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Profit row: กำไร (ยอดขายหักต้นทุน)
        SummaryCard(
            title = profitLabel,
            value = "${formatCurrency(profitValue)} ${stringResource(id = R.string.graph_currency_baht)}",
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
                    style = AppFontStyle.Medium,
                    size = FontSize.Small
                ),
                color = SecondaryText
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
                val density = LocalDensity.current
                val context = LocalContext.current
                val screenWidth = configuration.screenWidthDp.dp
                val totalContentWidth = maxOf(40.dp * 24, screenWidth)
                val chartSidePadding = 40.dp
                val yLabelEndGap = 8.dp
                val maxValue = chartData.maxOf { it.value }
                val minValue = chartData.minOf { it.value }
                val axisTypeface = ResourcesCompat.getFont(
                    context,
                    AppFontStyle.Regular.fontResource
                ) ?: Typeface.DEFAULT
                val maxYLabelWidthPx = remember(maxValue, minValue, axisTypeface, density.fontScale) {
                    with(density) {
                        measureMaxYAxisLabelWidthPx(
                            maxValue,
                            minValue,
                            ChartGridLines,
                            FontSize.Smaller.value.toPx(),
                            axisTypeface
                        )
                    }
                }
                val leftPadPx = remember(maxYLabelWidthPx, density.fontScale) {
                    with(density) {
                        maxOf(chartSidePadding.toPx(), maxYLabelWidthPx + yLabelEndGap.toPx())
                    }
                }
                val leftAxisWidthDp = with(density) { leftPadPx.toDp() }
                val minPointSpan =
                    40.dp * (chartData.size - 1).coerceAtLeast(1) + ChartPlotLeadingInset + 40.dp

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val rowInnerWidth = maxWidth
                    val basePlot = totalContentWidth - leftAxisWidthDp
                    val fillViewportPlot = (rowInnerWidth - leftAxisWidthDp).coerceAtLeast(1.dp)
                    val plotWidth = maxOf(
                        basePlot,
                        minPointSpan,
                        fillViewportPlot
                    ).coerceAtLeast(48.dp)

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            LineChartYAxisColumn(
                                maxValue = maxValue,
                                minValue = minValue,
                                axisTypeface = axisTypeface,
                                modifier = Modifier
                                    .width(leftAxisWidthDp)
                                    .fillMaxHeight()
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = true)
                                    .fillMaxHeight()
                                    .horizontalScroll(scrollState)
                            ) {
                                LineChart(
                                    data = chartData,
                                    showYAxisLabels = false,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .width(plotWidth)
                                )
                            }
                        }
                    }
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

/** Catmull-Rom style smooth curve through [points] (converted to cubic Bézier segments). */
private fun appendSmoothCurve(path: Path, points: List<Offset>) {
    if (points.size < 2) return
    for (i in 0 until points.size - 1) {
        val p0 = if (i == 0) points[0] else points[i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]
        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f
        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
}

private fun buildSmoothLinePath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    if (points.size == 1) {
        moveTo(points[0].x, points[0].y)
        return@apply
    }
    moveTo(points[0].x, points[0].y)
    appendSmoothCurve(this, points)
}

private fun buildSmoothAreaPath(points: List<Offset>, baselineY: Float): Path = Path().apply {
    if (points.isEmpty()) return@apply
    if (points.size == 1) {
        moveTo(points[0].x, baselineY)
        lineTo(points[0].x, points[0].y)
        lineTo(points[0].x, baselineY)
        close()
        return@apply
    }
    moveTo(points[0].x, baselineY)
    lineTo(points[0].x, points[0].y)
    appendSmoothCurve(this, points)
    lineTo(points.last().x, baselineY)
    close()
}

private const val ChartGridLines = 5

/** Left inset for the scrollable plot so spline + markers are not clipped at the canvas edge. */
private val ChartPlotLeadingInset = 24.dp

/** Widest Y-axis tick label (e.g. "12.2M") so we can reserve enough left gutter. */
private fun measureMaxYAxisLabelWidthPx(
    maxValue: Double,
    minValue: Double,
    gridLines: Int,
    textSizePx: Float,
    typeface: Typeface?
): Float {
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)
    val paint = android.graphics.Paint().apply {
        textSize = textSizePx
        this.typeface = typeface
        isAntiAlias = true
    }
    var maxW = 0f
    for (i in 0..gridLines) {
        val value = maxValue - (valueRange / gridLines) * i
        maxW = maxOf(maxW, paint.measureText(formatYAxisValue(value)))
    }
    return maxW
}

/** Fixed column: Y-axis ticks stay visible while the plot scrolls horizontally. */
@Composable
private fun LineChartYAxisColumn(
    maxValue: Double,
    minValue: Double,
    axisTypeface: Typeface,
    modifier: Modifier = Modifier
) {
    val axisLabelColor = SecondaryText
    val padding = 40.dp
    val yLabelEndGap = 8.dp
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)
    Canvas(modifier = modifier) {
        val axisLabelTextPx = FontSize.Smaller.value.toPx()
        val topPadPx = padding.toPx()
        val chartHeight = size.height - topPadPx * 2
        val startY = topPadPx
        val gridLines = ChartGridLines
        val yLabelGapPx = yLabelEndGap.toPx()
        for (i in 0..gridLines) {
            val value = maxValue - (valueRange / gridLines) * i
            val y = startY + (chartHeight / gridLines) * i
            val label = formatYAxisValue(value)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = axisLabelColor.toArgb()
                    textSize = axisLabelTextPx
                    textAlign = android.graphics.Paint.Align.RIGHT
                    typeface = axisTypeface
                    isAntiAlias = true
                }
                drawText(label, size.width - yLabelGapPx, y + 4.dp.toPx(), paint)
            }
        }
    }
}

@Composable
private fun LineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    showYAxisLabels: Boolean = true
) {
    if (data.isEmpty()) return
    
    val context = LocalContext.current
    val density = LocalDensity.current
    val currencySymbol = stringResource(id = R.string.graph_currency_symbol)
    
    val maxValue = data.maxOfOrNull { it.value } ?: 1.0
    val minValue = data.minOfOrNull { it.value } ?: 0.0
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)
    
    val padding = 40.dp
    val yLabelEndGap = 8.dp
    // Theme: AppColors — line/fill from PrimaryButton; axis from SecondaryText; grid from SecondaryButton
    val chartColor = PrimaryButton
    val axisLabelColor = SecondaryText
    val gridColor = SecondaryButton.copy(alpha = 0.72f)
    val areaGradientTop = PrimaryButton.copy(alpha = 0.26f)
    val areaGradientBottom = Color.Transparent
    val dashIntervals = floatArrayOf(6f, 8f)
    val axisTypeface = ResourcesCompat.getFont(context, AppFontStyle.Regular.fontResource) ?: Typeface.DEFAULT
    
    val maxYLabelWidthPx = remember(maxValue, minValue, axisTypeface, density.fontScale, showYAxisLabels) {
        if (!showYAxisLabels) 0f
        else with(density) {
            val textPx = FontSize.Smaller.value.toPx()
            measureMaxYAxisLabelWidthPx(maxValue, minValue, ChartGridLines, textPx, axisTypeface)
        }
    }
    val leftPadPx = remember(maxYLabelWidthPx, density.fontScale, showYAxisLabels) {
        if (!showYAxisLabels) with(density) { ChartPlotLeadingInset.toPx() }
        else with(density) { maxOf(padding.toPx(), maxYLabelWidthPx + yLabelEndGap.toPx()) }
    }
    val rightPadPx = remember(density.fontScale) { with(density) { padding.toPx() } }
    val topPadPx = remember(density.fontScale) { with(density) { padding.toPx() } }
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    Canvas(
        modifier = modifier
            .pointerInput(data, maxValue, minValue, valueRange, leftPadPx, rightPadPx, topPadPx, showYAxisLabels) {
                if (data.isEmpty()) return@pointerInput
                detectTapGestures { tapOffset ->
                    // คำนวณจุดในกราฟเพื่อหา point ที่ใกล้กับตำแหน่งที่แตะที่สุด
                    val width = size.width
                    val height = size.height
                    val chartWidth = width - leftPadPx - rightPadPx
                    val chartHeight = height - topPadPx - topPadPx
                    val startX = leftPadPx
                    val startY = topPadPx
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
            .graphicsLayer { clip = false }
    ) {
        val axisLabelTextPx = FontSize.Smaller.value.toPx()
        val width = size.width
        val height = size.height
        val chartWidth = width - leftPadPx - rightPadPx
        val chartHeight = height - topPadPx - topPadPx
        val startX = leftPadPx
        val startY = topPadPx
        val endX = startX + chartWidth
        val endY = startY + chartHeight
        
        // Draw horizontal grid (dotted)
        val gridLines = ChartGridLines
        val gridStroke = 1.dp.toPx()
        val gridPathEffect = PathEffect.dashPathEffect(dashIntervals, 0f)
        for (i in 0..gridLines) {
            val y = startY + (chartHeight / gridLines) * i
            drawLine(
                color = gridColor,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = gridStroke,
                pathEffect = gridPathEffect
            )
        }
        
        if (showYAxisLabels) {
            val yLabelGapPx = yLabelEndGap.toPx()
            for (i in 0..gridLines) {
                val value = maxValue - (valueRange / gridLines) * i
                val y = startY + (chartHeight / gridLines) * i
                val label = formatYAxisValue(value)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = axisLabelColor.toArgb()
                        textSize = axisLabelTextPx
                        textAlign = android.graphics.Paint.Align.RIGHT
                        typeface = axisTypeface
                        isAntiAlias = true
                    }
                    drawText(label, startX - yLabelGapPx, y + 4.dp.toPx(), paint)
                }
            }
        }
        
        // Calculate points
        val points = data.mapIndexed { index, point ->
            val x = startX + (chartWidth / (data.size - 1).coerceAtLeast(1)) * index
            val normalizedValue = ((point.value - minValue) / valueRange).coerceIn(0.0, 1.0)
            val y = endY - (chartHeight * normalizedValue.toFloat())
            Offset(x, y)
        }
        
        // Draw area under smooth curve
        if (points.isNotEmpty()) {
            val areaPath = buildSmoothAreaPath(points, endY)
            val topY = (points.minOfOrNull { it.y } ?: endY).coerceAtMost(endY)
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(areaGradientTop, areaGradientBottom),
                    startY = topY,
                    endY = endY
                )
            )
        }
        
        // Draw smooth line
        if (points.size > 1) {
            val linePath = buildSmoothLinePath(points)
            drawPath(
                path = linePath,
                color = chartColor,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        
        // Markers: white ring + blue fill (matches “dot with thin white border”)
        points.forEachIndexed { index, point ->
            val isSelected = selectedIndex == index
            val radiusCore = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx()
            val border = 1.75.dp.toPx()
            drawCircle(
                color = Color.White,
                radius = radiusCore + border,
                center = point
            )
            drawCircle(
                color = chartColor,
                radius = radiusCore,
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
                        textSize = FontSize.Small.value.toPx()
                        typeface = axisTypeface
                        isAntiAlias = true
                    }
                    val bgPaint = android.graphics.Paint().apply {
                        color = PrimaryText.copy(alpha = 0.92f).toArgb()
                        isAntiAlias = true
                    }
                    
                    val textWidth = textPaint.measureText(label)
                    val textHeight = textPaint.fontMetrics.run { bottom - top }
                    val paddingPx = 8.dp.toPx()
                    val gapFromPoint = 12.dp.toPx()
                    val bubbleW = textWidth + paddingPx * 2
                    val bubbleH = textHeight + paddingPx * 2
                    
                    var rectLeft = (point.x - bubbleW / 2f).coerceIn(0f, size.width - bubbleW)
                    // Above the point; may use negative Y — layer uses clip = false so it is not cut off
                    var rectTop = point.y - gapFromPoint - bubbleH
                    var rectBottom = rectTop + bubbleH
                    val canvasBottom = size.height.toFloat()
                    if (rectBottom > canvasBottom - 2f) {
                        rectTop = canvasBottom - 2f - bubbleH
                        rectBottom = rectTop + bubbleH
                    }
                    val rectRight = rectLeft + bubbleW
                    
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
                    color = axisLabelColor.toArgb()
                    textSize = axisLabelTextPx
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = axisTypeface
                    isAntiAlias = true
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

/** Compact labels for the sales chart Y-axis (e.g. 56.7K, 1.2M). */
private fun formatYAxisValue(value: Double): String {
    val axisFormat = DecimalFormat("#0.0")
    return when {
        value >= 1_000_000 ->
            axisFormat.format(value / 1_000_000) + "M"
        value >= 1000 ->
            axisFormat.format(value / 1000) + "K"
        kotlin.math.abs(value) < 1e-6 -> "0"
        else -> formatCurrency(value)
    }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendItem(
                    color = PrimaryButton,
                    title = stringResource(id = R.string.graph_transfer_payment),
                    amount = revenueComparison.transferAmount,
                    modifier = Modifier.weight(1f)
                )
                LegendItem(
                    color = GreenComplete,
                    title = stringResource(id = R.string.graph_cash_payment),
                    amount = revenueComparison.cashAmount,
                    modifier = Modifier.weight(1f)
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
    amount: Double,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
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
            color = PrimaryText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProductStatsCard(
    productStats: List<ProductStatsData>,
    totalProductSalesInPeriod: Double
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
            
            Text(
                text = "${formatCurrency(totalProductSalesInPeriod)} ${stringResource(id = R.string.graph_currency_baht)}",
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
                            totalProductSalesInPeriod = totalProductSalesInPeriod
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
    totalProductSalesInPeriod: Double
) {
    val sharePercent = if (totalProductSalesInPeriod > 0) (product.amount / totalProductSalesInPeriod * 100) else 0.0
    val progressFraction = if (totalProductSalesInPeriod > 0) {
        (product.amount / totalProductSalesInPeriod).coerceIn(0.05, 1.0)
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
                    size = FontSize.Small
                ),
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    text = "${product.quantity} ${stringResource(id = R.string.home_orders_unit)}",
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor, RoundedCornerShape(12.dp))
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
                    style = AppFontStyle.Medium,
                    size = FontSize.Small
                ),
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "${formatCurrency(seller.totalSales)} ${stringResource(id = R.string.graph_currency_baht)}",
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
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
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E5E5), RoundedCornerShape(12.dp))
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

