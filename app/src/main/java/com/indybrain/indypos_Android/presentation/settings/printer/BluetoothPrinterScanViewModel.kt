package com.indybrain.indypos_Android.presentation.settings.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.indybrain.indypos_Android.core.printer.PrinterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.posprinter.IConnectListener
import net.posprinter.POSConnect
import javax.inject.Inject

/**
 * ViewModel for Bluetooth Printer Scan Screen
 */
@HiltViewModel
class BluetoothPrinterScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val printerManager: PrinterManager
) : ViewModel() {
    
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    
    private val deviceConnectionStates = mutableMapOf<String, ConnectionState>()
    private val _connectionStatesFlow = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())
    val connectionStatesFlow: StateFlow<Map<String, ConnectionState>> = _connectionStatesFlow.asStateFlow()
    
    data class UiState(
        val isScanning: Boolean = false,
        val devices: List<BluetoothDeviceInfo> = emptyList()
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val allowedBluetoothActions = setOf(
        BluetoothDevice.ACTION_FOUND,
        BluetoothAdapter.ACTION_DISCOVERY_FINISHED
    )

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action !in allowedBluetoothActions) return

            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        // Filter out BLE devices (type 2) - but include classic Bluetooth
                        // Note: DEVICE_TYPE_LE = 2, DEVICE_TYPE_CLASSIC = 1, DEVICE_TYPE_DUAL = 3
                        if (it.type != BluetoothDevice.DEVICE_TYPE_LE || it.type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                            val deviceInfo = BluetoothDeviceInfo(
                                address = it.address,
                                name = it.name ?: "Unknown Device"
                            )
                            
                            // Check if device already exists
                            val existingIndex = _uiState.value.devices.indexOfFirst { d -> d.address == deviceInfo.address }
                            if (existingIndex == -1) {
                                // Add new device
                                _uiState.value = _uiState.value.copy(
                                    devices = _uiState.value.devices + deviceInfo
                                )
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _uiState.value = _uiState.value.copy(isScanning = false)
                }
            }
        }
    }
    
    private val connectListener = IConnectListener { code, connInfo, msg ->
        when (code) {
            POSConnect.CONNECT_SUCCESS -> {
                viewModelScope.launch {
                    // Connection successful - show checkmark
                    printerManager.currentPrinter?.let { printer ->
                        deviceConnectionStates[printer.address] = ConnectionState.Connected
                        _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                    }
                }
            }
            POSConnect.CONNECT_FAIL -> {
                viewModelScope.launch {
                    // Connection failed - change back to disconnected
                    printerManager.currentPrinter?.let { printer ->
                        deviceConnectionStates[printer.address] = ConnectionState.Disconnected
                        _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                    }
                }
            }
            POSConnect.CONNECT_INTERRUPT -> {
                viewModelScope.launch {
                    // Connection interrupted - change to disconnected
                    printerManager.currentPrinter?.let { printer ->
                        deviceConnectionStates[printer.address] = ConnectionState.Disconnected
                        _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                    }
                }
            }
            else -> {}
        }
    }
    
    init {
        registerBluetoothReceiver()
    }
    
    override fun onCleared() {
        super.onCleared()
        unregisterBluetoothReceiver()
        stopScanning()
    }
    
    fun checkAndRequestBluetooth() {
        if (!hasBluetoothPermissions()) {
            return
        }
        
        // For Android < 12, check GPS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isGpsEnabled()) {
            return
        }
        
        if (!isBluetoothEnabled()) {
            return
        }
        
        startScanning()
    }
    
    fun checkInitialConnectionState() {
        viewModelScope.launch {
            printerManager.currentPrinter?.let { printer ->
                if (printerManager.isConnected()) {
                    deviceConnectionStates[printer.address] = ConnectionState.Connected
                } else {
                    deviceConnectionStates[printer.address] = ConnectionState.Disconnected
                }
                _connectionStatesFlow.update { deviceConnectionStates.toMap() }
            }
        }
    }
    
    fun startScanning() {
        if (!hasBluetoothPermissions()) {
            return
        }
        
        if (!isBluetoothEnabled()) {
            return
        }
        
        // For Android < 12, GPS must be enabled
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !isGpsEnabled()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            
            // Clear previous devices
            _uiState.value = _uiState.value.copy(devices = emptyList())
            
            // Load bonded devices first
            loadBondedDevices()
            
            // Start discovery
            delay(300)
            bluetoothAdapter?.let { adapter ->
                try {
                    if (adapter.isDiscovering) {
                        adapter.cancelDiscovery()
                        delay(200)
                    }
                    val started = adapter.startDiscovery()
                    if (!started) {
                        // Retry after a delay
                        delay(500)
                        adapter.startDiscovery()
                    }
                } catch (e: SecurityException) {
                    // Permission issue
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun stopScanning() {
        viewModelScope.launch {
            bluetoothAdapter?.cancelDiscovery()
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                devices = emptyList()
            )
        }
    }
    
    fun onDeviceClick(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            val currentState = getConnectionState(device.address)
            
            when (currentState) {
                ConnectionState.Disconnected -> {
                    // Show loading when clicked
                    deviceConnectionStates[device.address] = ConnectionState.Connecting
                    _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                    // Save to printer manager
                    try {
                        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                        bluetoothDevice?.let {
                            printerManager.currentPrinter = it
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // Connect in background
                    connectToDevice(device)
                }
                ConnectionState.Connecting -> {
                    // Already connecting, do nothing
                }
                ConnectionState.Connected -> {
                    // Disconnect
                    deviceConnectionStates[device.address] = ConnectionState.Disconnected
                    _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                    disconnectFromDevice()
                }
            }
        }
    }
    
    fun getConnectionState(address: String): ConnectionState {
        return deviceConnectionStates[address] ?: ConnectionState.Disconnected
    }
    
    private fun loadBondedDevices() {
        if (!hasBluetoothPermissions()) return
        
        try {
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                val deviceInfo = BluetoothDeviceInfo(
                    address = device.address,
                    name = device.name ?: "Unknown Device"
                )
                
                if (!_uiState.value.devices.any { d -> d.address == deviceInfo.address }) {
                    _uiState.value = _uiState.value.copy(
                        devices = _uiState.value.devices + deviceInfo
                    )
                }
                
            // Check if this is the current printer
            if (device.address == printerManager.currentPrinter?.address) {
                if (printerManager.isConnected()) {
                    deviceConnectionStates[device.address] = ConnectionState.Connected
                } else {
                    deviceConnectionStates[device.address] = ConnectionState.Disconnected
                }
                _connectionStatesFlow.update { deviceConnectionStates.toMap() }
            }
            }
        } catch (e: SecurityException) {
            // Permission issue
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun connectToDevice(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            try {
                // Get BluetoothDevice from address
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                bluetoothDevice?.let {
                    // Connect in background - loading is already shown
                    printerManager.connectBluetooth(device.address, connectListener)
                }
            } catch (e: Exception) {
                // If connection fails, change state back to disconnected
                deviceConnectionStates[device.address] = ConnectionState.Disconnected
                _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                e.printStackTrace()
            }
        }
    }
    
    private fun disconnectFromDevice() {
        viewModelScope.launch {
            printerManager.disconnect()
        }
    }
    
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(bluetoothReceiver, filter)
        }
    }
    
    private fun unregisterBluetoothReceiver() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
    
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

