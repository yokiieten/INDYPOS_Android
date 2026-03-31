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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.locale.LocaleHelper
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.domain.model.Order
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.GreenComplete
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.RedFailure
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    viewModel: OrderViewModel = hiltViewModel(),
    onOrderClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var showCustomRangeSheet by rememberSaveable { mutableStateOf(false) }
    var customStartDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var customEndDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    // ทุกครั้งที่เข้าหน้ารายการออเดอร์ (compose ใหม่) ให้ดึง API ล่าสุด — ViewModel อาจยังอยู่จากแท็บก่อนหน้า
    LaunchedEffect(Unit) {
        viewModel.refreshOrders()
    }

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
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
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
                customRangeLabel = if (
                    uiState.filterOption == OrderFilter.SELECT_DATE &&
                    uiState.customStartDateMillis != null &&
                    uiState.customEndDateMillis != null
                ) {
                    formatOrderCustomRange(
                        startMillis = uiState.customStartDateMillis,
                        endMillis = uiState.customEndDateMillis
                    )
                } else null,
                onFilterSelected = { viewModel.selectFilter(it) },
                onSortSelected = { viewModel.selectSort(it) },
                onSelectDateRangeClick = {
                    customStartDateMillis = uiState.customStartDateMillis
                    customEndDateMillis = uiState.customEndDateMillis
                    showCustomRangeSheet = true
                }
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
                        isLoading = uiState.isLoading,
                        isLoadingMore = uiState.isLoadingMore,
                        hasMore = uiState.hasMore,
                        enableAutoLoadMore = page == pagerState.currentPage,
                        onLoadMore = { viewModel.loadMore() },
                        onRefresh = { viewModel.refreshOrders() },
                        onOrderClick = onOrderClick
                    )
                    1 -> OrderListContent(
                        orders = uiState.cancelledOrders,
                        isLoading = uiState.isLoading,
                        isLoadingMore = uiState.isLoadingMore,
                        hasMore = uiState.hasMore,
                        enableAutoLoadMore = page == pagerState.currentPage,
                        onLoadMore = { viewModel.loadMore() },
                        onRefresh = { viewModel.refreshOrders() },
                        onOrderClick = onOrderClick
                    )
                }
            }
    }

    if (showCustomRangeSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current

        // Initialize default dates (current month) when sheet opens and dates are null
        LaunchedEffect(showCustomRangeSheet) {
            if (customStartDateMillis == null || customEndDateMillis == null) {
                val startCalendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
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
                                viewModel.setCustomRange(
                                    customStartDateMillis!!,
                                    customEndDateMillis!!
                                )
                                showCustomRangeSheet = false
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
                            text = formatOrderCustomDate(customStartDateMillis, locale),
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
                            text = formatOrderCustomDate(customEndDateMillis, locale),
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
    customRangeLabel: String? = null,
    onFilterSelected: (OrderFilter) -> Unit,
    onSortSelected: (OrderSort) -> Unit,
    onSelectDateRangeClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filterText = if (filterOption == OrderFilter.SELECT_DATE && !customRangeLabel.isNullOrBlank()) {
        customRangeLabel
    } else {
        getFilterText(filterOption)
    }
    val sortText = getSortText(sortOption)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            color = Color(0xFFE3F2FD),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$filterText • $sortText",
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
                    expanded = false
                    onSelectDateRangeClick()
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

@Composable
private fun getFilterText(filter: OrderFilter): String {
    return when (filter) {
        OrderFilter.ALL -> stringResource(R.string.order_filter_all)
        OrderFilter.TODAY -> stringResource(R.string.order_filter_today)
        OrderFilter.THIS_WEEK -> stringResource(R.string.order_filter_this_week)
        OrderFilter.THIS_MONTH -> stringResource(R.string.order_filter_this_month)
        OrderFilter.SELECT_DATE -> stringResource(R.string.order_filter_select_date)
    }
}

@Composable
private fun getSortText(sort: OrderSort): String {
    return when (sort) {
        OrderSort.LATEST -> stringResource(R.string.order_sort_latest)
        OrderSort.OLDEST -> stringResource(R.string.order_sort_oldest)
        OrderSort.HIGHEST_AMOUNT -> stringResource(R.string.order_sort_highest)
    }
}

private fun formatOrderCustomDate(millis: Long?, locale: Locale): String {
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
private fun formatOrderCustomRange(startMillis: Long?, endMillis: Long?): String {
    if (startMillis == null || endMillis == null) return stringResource(R.string.order_filter_select_date)
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
private fun OrderListContent(
    orders: List<Order>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    /** เฉพาะแท็บที่เปิดอยู่ — กัน HorizontalPager คอมโพสสองหน้าแล้วยิง loadMore พร้อมกัน */
    enableAutoLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onOrderClick: (String) -> Unit = {}
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)
    // SwipeRefresh (Accompanist) ต้องมีลูกที่ร่วม nested scroll — Box เปล่าไม่ดึงรีเฟรชได้
    val refreshScrollState = rememberScrollState()

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = onRefresh
    ) {
        when {
            isLoading && orders.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(refreshScrollState),
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
            }
            orders.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(refreshScrollState),
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
            }
            else -> {
                val listState = rememberLazyListState()

                // โหลดเพิ่มเมื่อเลื่อนใกล้ท้ายรายการเท่านั้น — ถ้ารายการสั้นมองเห็นหมดจอ (เลื่อนไม่ได้) จะไม่ยิง API
                LaunchedEffect(listState, hasMore, isLoadingMore, enableAutoLoadMore) {
                    if (!enableAutoLoadMore) return@LaunchedEffect
                    snapshotFlow {
                        try {
                            val listFullyVisible =
                                !listState.canScrollForward && !listState.canScrollBackward
                            if (listFullyVisible) {
                                false
                            } else {
                                val lastVisible =
                                    listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                val totalItems = listState.layoutInfo.totalItemsCount
                                lastVisible >= totalItems - 3
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                        .distinctUntilChanged()
                        .collect { shouldLoadMore ->
                            if (shouldLoadMore && hasMore && !isLoadingMore && enableAutoLoadMore) {
                                try {
                                    onLoadMore()
                                } catch (e: Exception) {
                                    // Handle error silently
                                }
                            }
                        }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        OrderItem(
                            order = order,
                            onClick = { 
                                try {
                                    onOrderClick(order.id)
                                } catch (e: Exception) {
                                    // Handle error silently
                                }
                            }
                        )
                    }

                    // Footer: loading indicator while fetching more
                    item {
                        if (isLoadingMore && hasMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.order_loading),
                                    style = FontUtils.mainFont(
                                        style = AppFontStyle.Regular,
                                        size = FontSize.Small
                                    ),
                                    color = SecondaryText
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItem(
    order: Order,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                        color = RedFailure
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when (order.status) {
                        com.indybrain.indypos_Android.domain.model.OrderStatus.DRAFT -> stringResource(id = R.string.order_status_draft)
                        com.indybrain.indypos_Android.domain.model.OrderStatus.CONFIRMED -> stringResource(id = R.string.order_status_confirmed)
                        com.indybrain.indypos_Android.domain.model.OrderStatus.PREPARING -> stringResource(id = R.string.order_status_preparing)
                        com.indybrain.indypos_Android.domain.model.OrderStatus.READY -> stringResource(id = R.string.order_status_ready)
                        com.indybrain.indypos_Android.domain.model.OrderStatus.DELIVERED -> stringResource(id = R.string.order_status_delivered)
                        com.indybrain.indypos_Android.domain.model.OrderStatus.CANCELLED -> stringResource(id = R.string.order_status_cancelled)
                    },
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Small
                    ),
                    color = when (order.status) {
                        com.indybrain.indypos_Android.domain.model.OrderStatus.CANCELLED -> RedFailure
                        else -> GreenComplete
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

@Composable
private fun formatDate(date: java.util.Date): String {
    val context = LocalContext.current
    val locale = LocaleHelper.getCurrentLocale(context)
    val isEnglish = locale.language == "en"

    // Date object is UTC timestamp, convert to Asia/Bangkok timezone
    val bangkokTimeZone = java.util.TimeZone.getTimeZone("Asia/Bangkok")
    val calendar = java.util.Calendar.getInstance(bangkokTimeZone)
    calendar.timeInMillis = date.time

    return if (isEnglish) {
        // English format: "Dec 11, 2025, 11:15" (24-hour, no AM/PM)
        val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy, HH:mm", locale)
        dateFormat.timeZone = bangkokTimeZone
        dateFormat.format(date)
    } else {
        // Thai format: "11 ธ.ค. 25, 11:15" (year as พ.ศ. + 543 for display)
        val year = calendar.get(java.util.Calendar.YEAR) + 543
        val monthFormat = java.text.SimpleDateFormat("MMM", locale)
        monthFormat.timeZone = bangkokTimeZone
        val monthStr = monthFormat.format(date)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val shortYear = year % 100
        "$day $monthStr $shortYear, ${String.format(locale, "%02d:%02d", hour, minute)}"
    }
}

private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return "฿${formatter.format(value)}"
}

