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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.presentation.navigation.HomeBottomDestination
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

@Composable
fun GraphScreen(
    viewModel: GraphViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedDestination by rememberSaveable { mutableStateOf(HomeBottomDestination.Charts) }
    
    Scaffold(
        containerColor = BaseBackground,
        bottomBar = {
            HomeBottomBar(
                selected = selectedDestination,
                onSelected = { selectedDestination = it }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Title
            Text(
                text = stringResource(id = R.string.graph_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Large
                ),
                color = PrimaryText,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Date selector
            TimePeriodSelector(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = { viewModel.selectPeriod(it) }
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
            
            SummaryCardsSection(summary = uiState.summary)
            
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
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
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
                    text = selectedPeriod.displayName,
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
                            text = period.displayName,
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

@Composable
private fun SummaryCardsSection(summary: GraphSummary) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top row: Sales and Cost
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = stringResource(id = R.string.graph_today_sales),
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
        
        // Middle row: Orders Today (full width)
        SummaryCard(
            title = stringResource(id = R.string.graph_orders_today),
            value = "${summary.ordersToday} ${stringResource(id = R.string.home_orders_unit)}",
            valueColor = GreenComplete,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Bottom row: Orders Today and Cancelled Orders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = stringResource(id = R.string.graph_orders_today),
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
                LineChart(
                    data = chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ไม่มีข้อมูล",
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
    
    val maxValue = data.maxOfOrNull { it.value } ?: 1.0
    val minValue = data.minOfOrNull { it.value } ?: 0.0
    val valueRange = (maxValue - minValue).coerceAtLeast(1.0)
    
    val padding = 40.dp
    val chartColor = PrimaryButton
    val gridColor = Color(0xFFE5E5E5)
    
    Canvas(modifier = modifier) {
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
        points.forEach { point ->
            drawCircle(
                color = chartColor,
                radius = 6.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = point
            )
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

@Composable
private fun HomeBottomBar(
    selected: HomeBottomDestination,
    onSelected: (HomeBottomDestination) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        HomeBottomDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (destination == selected) destination.selectedIcon else destination.icon,
                        contentDescription = stringResource(destination.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = FontUtils.mainFont(
                            style = if (destination == selected) AppFontStyle.Bold else AppFontStyle.Regular,
                            size = FontSize.Smallest
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White,
                    selectedIconColor = PrimaryText,
                    selectedTextColor = PrimaryText,
                    unselectedIconColor = PlaceholderText,
                    unselectedTextColor = PlaceholderText
                )
            )
        }
    }
}


private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(value)
}

