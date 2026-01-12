package com.indybrain.indypos_Android.presentation.settings.printer

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.printer.PrinterType
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton

/**
 * Printer Settings Screen
 * Displays printer configuration for Receipt and Label printers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToBluetoothScan: (PrinterType) -> Unit = {},
    viewModel: PrinterSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        containerColor = BaseBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.printer_settings_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "กลับ",
                            tint = PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BaseBackground,
                    titleContentColor = PrimaryText
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Receipt Printer Card
            uiState.receiptPrinter?.let { printer ->
                PrinterCard(
                    printerInfo = printer,
                    icon = Icons.Outlined.Print,
                    iconBackgroundColor = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF2196F3),
                    onToggle = { enabled ->
                        viewModel.togglePrinter(PrinterType.RECEIPT, enabled)
                    },
                    onSelectPrinter = {
                        onNavigateToBluetoothScan(PrinterType.RECEIPT)
                    },
                    onTestPrint = null
                )
            }
            
            // Label Printer Card
            uiState.labelPrinter?.let { printer ->
                PrinterCard(
                    printerInfo = printer,
                    icon = Icons.Outlined.Label,
                    iconBackgroundColor = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF4CAF50),
                    onToggle = { enabled ->
                        viewModel.togglePrinter(PrinterType.LABEL, enabled)
                    },
                    onSelectPrinter = {
                        onNavigateToBluetoothScan(PrinterType.LABEL)
                    },
                    onTestPrint = if (printer.type == PrinterType.LABEL && printer.isConnected) {
                        { viewModel.testLabelPrint() }
                    } else null
                )
            }
        }
    }
}

/**
 * Printer Card Component
 */
@Composable
private fun PrinterCard(
    printerInfo: PrinterSettingsViewModel.PrinterInfo,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    onToggle: (Boolean) -> Unit,
    onSelectPrinter: () -> Unit,
    onTestPrint: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with Icon and Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon Container
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBackgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Title
                    Text(
                        text = printerInfo.title,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.SemiBold,
                            size = FontSize.Medium
                        ),
                        color = PrimaryText
                    )
                }
                
                // Toggle Switch
                Switch(
                    checked = printerInfo.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryButton,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = PlaceholderText
                    )
                )
            }
            
            // Printer Info
            if (printerInfo.printerName != null || printerInfo.macAddress != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    printerInfo.printerName?.let { name ->
                        Text(
                            text = "ชื่อ: $name",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = SecondaryText
                        )
                    }
                    
                    printerInfo.macAddress?.let { mac ->
                        Text(
                            text = "MAC: $mac",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = SecondaryText
                        )
                    }
                    
                    // Connection Status
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (printerInfo.isConnected) Color(0xFF4CAF50)
                                    else Color(0xFFFF5252)
                                )
                        )
                        Text(
                            text = if (printerInfo.isConnected) "เชื่อมต่อแล้ว" else "ไม่ได้เชื่อมต่อ",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Regular,
                                size = FontSize.Small
                            ),
                            color = if (printerInfo.isConnected) Color(0xFF4CAF50)
                            else Color(0xFFFF5252)
                        )
                    }
                }
            } else {
                Text(
                    text = "ยังไม่ได้เลือกเครื่องพิมพ์",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Small
                    ),
                    color = PlaceholderText
                )
            }
            
            // Buttons Row
            if (onTestPrint != null && printerInfo.isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select Printer Button
                    Button(
                        onClick = onSelectPrinter,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryButton
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (printerInfo.printerName != null) "เปลี่ยนเครื่องพิมพ์" else "เลือกเครื่องพิมพ์",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = Color.White
                        )
                    }
                    
                    // Test Print Button
                    OutlinedButton(
                        onClick = onTestPrint,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryButton
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ทดสอบพิมพ์",
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            )
                        )
                    }
                }
            } else {
                // Select Printer Button only
                Button(
                    onClick = onSelectPrinter,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryButton
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (printerInfo.printerName != null) "เปลี่ยนเครื่องพิมพ์" else "เลือกเครื่องพิมพ์",
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Medium,
                            size = FontSize.Medium
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

