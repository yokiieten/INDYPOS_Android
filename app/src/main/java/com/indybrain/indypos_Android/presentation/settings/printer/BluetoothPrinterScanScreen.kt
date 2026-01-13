package com.indybrain.indypos_Android.presentation.settings.printer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PlaceholderText
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText

/**
 * Bluetooth Printer Scan Screen
 * Displays available Bluetooth printers and allows connection
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPrinterScanScreen(
    onBackClick: () -> Unit = {},
    viewModel: BluetoothPrinterScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Request Bluetooth permissions
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    val permissionsState = rememberMultiplePermissionsState(permissions)
    
    // Bluetooth enable launcher
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startScanning()
        }
    }
    
    // Location settings launcher (for Android < 12)
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if GPS is now enabled and start scanning
        if (viewModel.isGpsEnabled()) {
            viewModel.startScanning()
        }
    }
    
    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        } else {
            viewModel.checkInitialConnectionState()
            // Check and request Bluetooth/Location after permissions granted
            if (viewModel.isBluetoothEnabled()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    if (viewModel.isGpsEnabled()) {
                        viewModel.startScanning()
                    } else {
                        // Open location settings
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        locationSettingsLauncher.launch(intent)
                    }
                } else {
                    viewModel.startScanning()
                }
            } else {
                // Request to enable Bluetooth
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(intent)
            }
        }
    }
    
    // Start scanning when permissions are granted
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.checkInitialConnectionState()
            // Check and request Bluetooth/Location after permissions granted
            if (viewModel.isBluetoothEnabled()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    if (viewModel.isGpsEnabled()) {
                        viewModel.startScanning()
                    } else {
                        // Open location settings
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        locationSettingsLauncher.launch(intent)
                    }
                } else {
                    viewModel.startScanning()
                }
            } else {
                // Request to enable Bluetooth
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(intent)
            }
        }
    }
    
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
        ) {
            // Header Section with Printer Image and Info
            HeaderSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            )
            
            // Section Title
            Text(
                text = stringResource(id = R.string.printer_select_device),
                style = FontUtils.mainFont(
                    style = AppFontStyle.SemiBold,
                    size = FontSize.Large
                ),
                color = PrimaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Printer Toggle Switch
            PrinterToggleRow(
                isEnabled = uiState.isScanning,
                onToggleChange = { enabled ->
                    if (enabled) {
                        if (!permissionsState.allPermissionsGranted) {
                            permissionsState.launchMultiplePermissionRequest()
                        } else if (!viewModel.isBluetoothEnabled()) {
                            // Request to enable Bluetooth
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            bluetoothEnableLauncher.launch(intent)
                        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !viewModel.isGpsEnabled()) {
                            // Open location settings for Android < 12
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            locationSettingsLauncher.launch(intent)
                        } else {
                            viewModel.startScanning()
                        }
                    } else {
                        viewModel.stopScanning()
                    }
                }
            )
            
            // Available Devices Section
            if (uiState.isScanning && uiState.devices.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.printer_available_devices),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Small
                    ),
                    color = PlaceholderText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Devices List
            val connectionStates by viewModel.connectionStatesFlow.collectAsStateWithLifecycle()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(uiState.devices) { device ->
                    val connectionState = connectionStates[device.address] ?: ConnectionState.Disconnected
                    
                    PrinterDeviceRow(
                        device = device,
                        connectionState = connectionState,
                        onClick = {
                            viewModel.onDeviceClick(device)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Header section with printer image and support message
 */
@Composable
private fun HeaderSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Printer Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F7)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_printer_connect),
                contentDescription = "Printer",
                modifier = Modifier.size(100.dp)
            )
        }
        
        Text(
            text = stringResource(id = R.string.printer_support_message),
            style = FontUtils.mainFont(
                style = AppFontStyle.Medium,
                size = FontSize.Medium
            ),
            color = PrimaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Text(
            text = stringResource(id = R.string.printer_model),
            style = FontUtils.mainFont(
                style = AppFontStyle.Regular,
                size = FontSize.Small
            ),
            color = PlaceholderText
        )
    }
}

/**
 * Printer Toggle Row
 */
@Composable
private fun PrinterToggleRow(
    isEnabled: Boolean,
    onToggleChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.printer_label),
                style = FontUtils.mainFont(
                    style = AppFontStyle.Medium,
                    size = FontSize.Medium
                ),
                color = PrimaryText
            )
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryButton
                )
            )
        }
    }
}

/**
 * Printer Device Row
 */
@Composable
private fun PrinterDeviceRow(
    device: BluetoothDeviceInfo,
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Printer Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFF1F3)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🖨️",
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Regular,
                        size = FontSize.Medium
                    )
                )
            }
            
            // Device Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name ?: stringResource(id = R.string.printer_unknown),
                    style = FontUtils.mainFont(
                        style = AppFontStyle.Medium,
                        size = FontSize.Medium
                    ),
                    color = PrimaryText
                )
            }
            
            // Connection Status Indicator
            when (connectionState) {
                ConnectionState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryButton
                    )
                }
                ConnectionState.Connected -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryButton,
                        modifier = Modifier.size(24.dp)
                    )
                }
                ConnectionState.Disconnected -> {
                    // Empty space
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/**
 * Bluetooth Device Info
 */
data class BluetoothDeviceInfo(
    val address: String,
    val name: String?
)

/**
 * Connection State
 */
enum class ConnectionState {
    Connecting,
    Connected,
    Disconnected
}

