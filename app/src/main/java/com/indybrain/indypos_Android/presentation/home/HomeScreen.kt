package com.indybrain.indypos_Android.presentation.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.ui.components.ShopTopAppBar
import com.indybrain.indypos_Android.presentation.graph.GraphScreen
import com.indybrain.indypos_Android.presentation.navigation.HomeBottomDestination
import com.indybrain.indypos_Android.presentation.order.OrderScreen
import com.indybrain.indypos_Android.presentation.settings.SettingsScreen
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import java.text.DecimalFormat

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToMainProduct: () -> Unit = {},
    onLogoutSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var selectedDestination by rememberSaveable { mutableStateOf(HomeBottomDestination.Home) }
    
    // Fetch data when screen appears (like viewWillAppear in iOS)
    // This will trigger when:
    // 1. Screen first appears (selectedDestination is Home)
    // 2. User navigates back to Home tab from other tabs
    LaunchedEffect(selectedDestination) {
        if (selectedDestination == HomeBottomDestination.Home) {
            viewModel.refreshData()
        }
    }
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            if (selectedDestination != HomeBottomDestination.Charts && selectedDestination != HomeBottomDestination.Orders) {
                @androidx.compose.runtime.Composable {
                    ShopTopAppBar(
                        shopName = uiState.shopName,
                        onEditClick = { /* TODO hook edit */ }
                    )
                }
            } else null
        },
        bottomBar = {
            HomeBottomBar(
                selected = selectedDestination,
                onSelected = { selectedDestination = it }
            )
        }
    ) { padding ->
        when (selectedDestination) {
            HomeBottomDestination.Charts -> {
                GraphScreen()
            }
            HomeBottomDestination.Orders -> {
                OrderScreen()
            }
            HomeBottomDestination.Settings -> {
                SettingsScreen(
                    onLogoutSuccess = onLogoutSuccess
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    ShopCoverCard(
                        description = uiState.shopDescription,
                        onDescriptionClick = { /* TODO open edit */ }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    DailyOverviewSection(statistics = uiState.statistics)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    TopProductSection(statistics = uiState.statistics)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    ShortcutsSection(
                        shortcuts = uiState.shortcuts,
                        onShortcutClick = { shortcutId ->
                            when (shortcutId) {
                                "start_order" -> onNavigateToMainProduct()
                                // Add other shortcut handlers here
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopCoverCard(
    description: String,
    onDescriptionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFCCE0F6), Color(0xFFEEF6FF))
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_appstore),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp)
                )
                
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF5EA6ED)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_eye),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = stringResource(id = R.string.home_change_cover),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Small
                            ),
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onDescriptionClick),
        color = Color(0xFFF5F5F7)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = description.ifBlank {
                            stringResource(id = R.string.home_description_placeholder)
                        },
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Small
                        ),
                        color = if (description.isBlank()) PlaceholderText else PrimaryText,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.home_edit_shop_cd),
                        tint = SecondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyOverviewSection(statistics: HomeStatistics) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.home_daily_sales_title),
            style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
            color = PrimaryText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = stringResource(id = R.string.home_daily_sales_label),
                value = "${formatCurrency(statistics.todaysSales)} ${stringResource(id = R.string.home_currency_suffix)}"
            )
            SummaryCard(
                title = stringResource(id = R.string.home_orders_today_label),
                value = "${statistics.ordersToday} ${stringResource(id = R.string.home_orders_unit)}"
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier
//            .weight(1f)
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Small),
                color = SecondaryText
            )
            Text(
                text = value,
                style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Large),
                color = PrimaryText
            )
        }
    }
}

@Composable
private fun TopProductSection(statistics: HomeStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.home_best_seller_title),
                style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
                color = PrimaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            val bestSellerText = if (statistics.topProductName.isBlank()) {
                stringResource(id = R.string.home_best_seller_empty)
            } else {
                val qty = "${statistics.topProductQuantity} ${stringResource(id = R.string.home_orders_unit)}"
                val amount = "${formatCurrency(statistics.topProductAmount)} ${stringResource(id = R.string.home_currency_suffix)}"
                "${statistics.topProductName} $qty ($amount)"
            }
            Text(
                text = bestSellerText,
                style = FontUtils.mainFont(style = AppFontStyle.Medium, size = FontSize.Medium),
                color = SecondaryText
            )
        }
    }
}

@Composable
private fun ShortcutsSection(
    shortcuts: List<HomeShortcut>,
    onShortcutClick: (String) -> Unit
) {
    Text(
        text = stringResource(id = R.string.home_shortcuts_title),
        style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
        color = PrimaryText
    )
    Spacer(modifier = Modifier.height(12.dp))
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        shortcuts.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (shortcut in rowItems) {
                    ShortcutCard(
                        shortcut = shortcut,
                        onClick = { onShortcutClick(shortcut.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    shortcut: HomeShortcut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shortcut.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = shortcut.icon,
                    contentDescription = null,
                    tint = Color(0xFF3366CC)
                )
            }
            Text(
                text = shortcut.title,
                style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Small),
                color = PrimaryText
            )
            Text(
                text = shortcut.subtitle,
                style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Small),
                color = SecondaryText
            )
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
                    indicatorColor = Color.White, // Background สีขาวเมื่อเลือก
                    selectedIconColor = PrimaryText, // ไอคอนสีดำเมื่อเลือก
                    selectedTextColor = PrimaryText, // ข้อความสีดำเมื่อเลือก
                    unselectedIconColor = PlaceholderText, // ไอคอนสีเทาเมื่อไม่เลือก
                    unselectedTextColor = PlaceholderText // ข้อความสีเทาเมื่อไม่เลือก
                )
            )
        }
    }
}


private fun formatCurrency(value: Double): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(value)
}

