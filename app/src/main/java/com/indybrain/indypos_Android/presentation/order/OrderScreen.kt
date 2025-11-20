package com.indybrain.indypos_Android.presentation.order

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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.domain.model.Order
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun OrderScreen(
    viewModel: OrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    
    // Sync pager state with selected tab
    LaunchedEffect(uiState.selectedTab) {
        val page = when (uiState.selectedTab) {
            OrderTab.COMPLETED -> 0
            OrderTab.CANCELLED -> 1
        }
        if (pagerState.currentPage != page) {
            pagerState.animateScrollToPage(page)
        }
    }
    
    // Sync tab selection with pager state
    LaunchedEffect(pagerState.currentPage) {
        val tab = when (pagerState.currentPage) {
            0 -> OrderTab.COMPLETED
            1 -> OrderTab.CANCELLED
            else -> OrderTab.COMPLETED
        }
        if (uiState.selectedTab != tab) {
            viewModel.selectTab(tab)
        }
    }
    
    Scaffold(
        containerColor = BaseBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            OrderHeader(
                onRefreshClick = { viewModel.refreshOrders() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tabs
            OrderTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab ->
                    viewModel.selectTab(tab)
                    coroutineScope.launch {
                        val page = when (tab) {
                            OrderTab.COMPLETED -> 0
                            OrderTab.CANCELLED -> 1
                        }
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Filter button
            OrderFilterButton(
                filterOption = uiState.filterOption,
                sortOption = uiState.sortOption,
                onFilterSelected = { viewModel.selectFilter(it) },
                onSortSelected = { viewModel.selectSort(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Horizontal Pager for swipe
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OrderListContent(
                        orders = uiState.completedOrders,
                        isLoading = uiState.isLoading
                    )
                    1 -> OrderListContent(
                        orders = uiState.cancelledOrders,
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderHeader(
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.order_history_title),
            style = FontUtils.mainFont(
                style = AppFontStyle.Bold,
                size = FontSize.Large
            ),
            color = PrimaryText
        )
        
        IconButton(onClick = onRefreshClick) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(id = R.string.order_refresh),
                tint = GreenComplete,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun OrderTabs(
    selectedTab: OrderTab,
    onTabSelected: (OrderTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        OrderTabItem(
            text = stringResource(id = R.string.order_tab_completed),
            isSelected = selectedTab == OrderTab.COMPLETED,
            onClick = { onTabSelected(OrderTab.COMPLETED) },
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(24.dp))
        
        OrderTabItem(
            text = stringResource(id = R.string.order_tab_cancelled),
            isSelected = selectedTab == OrderTab.CANCELLED,
            onClick = { onTabSelected(OrderTab.CANCELLED) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OrderTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = FontUtils.mainFont(
                style = if (isSelected) AppFontStyle.Bold else AppFontStyle.Regular,
                size = FontSize.Medium
            ),
            color = if (isSelected) PrimaryText else PlaceholderText,
            modifier = Modifier.clickable(onClick = onClick)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        color = Color(0xFF5EA6ED),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun OrderFilterButton(
    filterOption: OrderFilter,
    sortOption: OrderSort,
    onFilterSelected: (OrderFilter) -> Unit,
    onSortSelected: (OrderSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            color = Color(0xFFE3F2FD),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${getFilterText(filterOption)} • ${getSortText(sortOption)}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Small
                    ),
                    color = PrimaryText
                )
                
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            // Title
            Text(
                text = stringResource(id = R.string.order_filter_sort_title),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            
            // Filter by Date section
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.order_filter_by_date),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                    }
                },
                onClick = { }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_filter_today),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (filterOption == OrderFilter.TODAY) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFilterSelected(OrderFilter.TODAY)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_filter_this_week),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (filterOption == OrderFilter.THIS_WEEK) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFilterSelected(OrderFilter.THIS_WEEK)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_filter_this_month),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (filterOption == OrderFilter.THIS_MONTH) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFilterSelected(OrderFilter.THIS_MONTH)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_filter_select_date),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (filterOption == OrderFilter.SELECT_DATE) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFilterSelected(OrderFilter.SELECT_DATE)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_filter_all),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (filterOption == OrderFilter.ALL) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onFilterSelected(OrderFilter.ALL)
                    expanded = false
                }
            )
            
            // Sort section
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.order_sort_title),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                    }
                },
                onClick = { }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_sort_latest),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (sortOption == OrderSort.LATEST) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onSortSelected(OrderSort.LATEST)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_sort_oldest),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (sortOption == OrderSort.OLDEST) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onSortSelected(OrderSort.OLDEST)
                    expanded = false
                }
            )
            
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.order_sort_highest),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = PrimaryText
                        )
                        if (sortOption == OrderSort.HIGHEST_AMOUNT) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenComplete,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onSortSelected(OrderSort.HIGHEST_AMOUNT)
                    expanded = false
                }
            )
        }
    }
}

private fun getFilterText(filter: OrderFilter): String {
    return when (filter) {
        OrderFilter.ALL -> "ทั้งหมด"
        OrderFilter.TODAY -> "วันนี้"
        OrderFilter.THIS_WEEK -> "สัปดาห์นี้"
        OrderFilter.THIS_MONTH -> "เดือนนี้"
        OrderFilter.SELECT_DATE -> "เลือกวันที่"
    }
}

private fun getSortText(sort: OrderSort): String {
    return when (sort) {
        OrderSort.LATEST -> "ล่าสุด"
        OrderSort.OLDEST -> "เก่าสุด"
        OrderSort.HIGHEST_AMOUNT -> "ยอดสูงสุด"
    }
}

@Composable
private fun OrderListContent(
    orders: List<Order>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.order_loading),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = SecondaryText
            )
        }
    } else if (orders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.order_empty),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Regular,
                    size = FontSize.Medium
                ),
                color = PlaceholderText
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                OrderItem(order = order)
            }
        }
    }
}

@Composable
private fun OrderItem(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ViewList,
                    contentDescription = null,
                    tint = Color(0xFF5EA6ED),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Order details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = order.orderId,
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Bold,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${stringResource(id = R.string.order_created_at)} ${formatDate(order.createdAt)}",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = SecondaryText
                )
                
                if (order.cancelledAt != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stringResource(id = R.string.order_cancelled_at)} ${formatDate(order.cancelledAt)}",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = SecondaryText
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when (order.status) {
                        com.indybrain.indypos_Android.domain.model.OrderStatus.COMPLETED -> stringResource(id = R.string.order_status_completed)
                        com.indybrain.indypos_Android.domain.model.OrderStatus.CANCELLED -> stringResource(id = R.string.order_status_cancelled)
                        com.indybrain.indypos_Android.domain.model.OrderStatus.FAILED -> stringResource(id = R.string.order_status_failed)
                    },
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Small
                    ),
                    color = when (order.status) {
                        com.indybrain.indypos_Android.domain.model.OrderStatus.COMPLETED -> GreenComplete
                        com.indybrain.indypos_Android.domain.model.OrderStatus.CANCELLED -> RedFailure
                        com.indybrain.indypos_Android.domain.model.OrderStatus.FAILED -> RedFailure
                    }
                )
            }
            
            // Price
            Text(
                text = formatCurrency(order.totalAmount),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Bold,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
        }
    }
}

private fun formatDate(date: java.util.Date): String {
    val calendar = java.util.Calendar.getInstance()
    calendar.time = date
    
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val month = calendar.get(java.util.Calendar.MONTH)
    val year = calendar.get(java.util.Calendar.YEAR) % 100 // Last 2 digits
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    
    val monthNames = arrayOf(
        "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
        "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."
    )
    
    return "$day ${monthNames[month]} $year, ${String.format("%02d:%02d", hour, minute)}"
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

