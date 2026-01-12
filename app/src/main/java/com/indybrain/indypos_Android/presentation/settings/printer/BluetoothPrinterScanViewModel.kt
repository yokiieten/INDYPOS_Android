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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.indybrain.indypos_Android.core.printer.PrinterManager
import com.indybrain.indypos_Android.core.printer.PrinterType
import com.indybrain.indypos_Android.domain.repository.PrinterSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import javax.inject.Inject

/**
 * ViewModel for Bluetooth Printer Scan Screen
 */
@HiltViewModel
class BluetoothPrinterScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val printerManager: PrinterManager,
    private val printerSettingsRepository: PrinterSettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Get printer type from navigation argument
    private val printerType: PrinterType = savedStateHandle.get<String>("printerType")
        ?.let { PrinterType.valueOf(it) } 
        ?: PrinterType.RECEIPT
    
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
    
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
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
                    // Scanning finished
                }
            }
        }
    }
    
    private var currentConnectingDevice: BluetoothDeviceInfo? = null
    private var currentConnection: IDeviceConnection? = null
    
    private val connectListener = IConnectListener { code, connInfo, msg ->
        when (code) {
            POSConnect.CONNECT_SUCCESS -> {
                viewModelScope.launch {
                    // Connection successful
                    currentConnectingDevice?.let { deviceInfo ->
                        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceInfo.address)
                        if (bluetoothDevice != null && currentConnection != null) {
                            // Save to printer manager
                            printerManager.savePrinterConnection(
                                device = bluetoothDevice,
                                connection = currentConnection!!,
                                type = printerType
                            )
                            
                            // Save to printer settings repository
                            printerSettingsRepository.savePrinterSettings(
                                type = printerType,
                                printerName = deviceInfo.name,
                                macAddress = deviceInfo.address,
                                enabled = true,
                                autoConnect = false
                            )
                            
                            // Update connection status
                            printerSettingsRepository.updateConnectionStatus(printerType, true)
                            
                            // Show success state
                            deviceConnectionStates[deviceInfo.address] = ConnectionState.Connected
                            _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                        }
                    }
                }
            }
            POSConnect.CONNECT_FAIL -> {
                viewModelScope.launch {
                    // Connection failed - change back to disconnected
                    currentConnectingDevice?.let { deviceInfo ->
                        deviceConnectionStates[deviceInfo.address] = ConnectionState.Disconnected
                        _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                    }
                    currentConnection?.close()
                    currentConnection = null
                    currentConnectingDevice = null
                }
            }
            POSConnect.CONNECT_INTERRUPT -> {
                viewModelScope.launch {
                    // Connection interrupted - change to disconnected
                    currentConnectingDevice?.let { deviceInfo ->
                        deviceConnectionStates[deviceInfo.address] = ConnectionState.Disconnected
                        _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                    }
                    currentConnection?.close()
                    currentConnection = null
                    currentConnectingDevice = null
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
                currentConnectingDevice = device
                
                // Create connection
                currentConnection?.close()
                currentConnection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)
                
                // Connect
                currentConnection?.connect(device.address, connectListener)
            } catch (e: Exception) {
                // If connection fails, change state back to disconnected
                deviceConnectionStates[device.address] = ConnectionState.Disconnected
                _connectionStatesFlow.update { deviceConnectionStates.toMap() }
                currentConnection?.close()
                currentConnection = null
                currentConnectingDevice = null
                e.printStackTrace()
            }
        }
    }
    
    private fun disconnectFromDevice() {
        viewModelScope.launch {
            printerManager.disconnect(printerType)
            printerSettingsRepository.updateConnectionStatus(printerType, false)
        }
    }
    
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
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

