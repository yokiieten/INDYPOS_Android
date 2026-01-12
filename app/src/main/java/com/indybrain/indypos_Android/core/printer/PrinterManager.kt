package com.indybrain.indypos_Android.core.printer

import android.bluetooth.BluetoothDevice
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Printer type enumeration
 */
enum class PrinterType {
    RECEIPT,  // เครื่องพิมพ์ใบเสร็จ (Xprinter)
    LABEL     // เครื่องพิมพ์สติกเกอร์ (XP 420B)
}

/**
 * Data class to hold printer information
 */
data class PrinterInfo(
    val device: BluetoothDevice,
    val connection: IDeviceConnection,
    val type: PrinterType
)

/**
 * Singleton manager for multiple printer connections
 * Supports both Receipt and Label printers simultaneously
 */
@Singleton
class PrinterManager @Inject constructor() {
    // Map to store multiple printer connections by type
    private val printerConnections = mutableMapOf<PrinterType, PrinterInfo>()
    
    /**
     * Get current printer device by type
     */
    fun getCurrentPrinter(type: PrinterType): BluetoothDevice? {
        return printerConnections[type]?.device
    }
    
    /**
     * Get current connection by type
     */
    fun getCurrentConnection(type: PrinterType): IDeviceConnection? {
        return printerConnections[type]?.connection
    }
    
    /**
     * Check if printer is connected by type
     */
    fun isConnected(type: PrinterType): Boolean {
        return printerConnections[type]?.connection?.isConnect == true
    }
    
    /**
     * Connect to Bluetooth printer
     */
    fun connectBluetooth(
        macAddress: String,
        type: PrinterType,
        listener: IConnectListener
    ) {
        // Close existing connection for this type
        printerConnections[type]?.connection?.close()
        
        // Create new connection
        val connection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)
        connection?.connect(macAddress, listener)
    }
    
    /**
     * Save printer connection after successful connection
     */
    fun savePrinterConnection(
        device: BluetoothDevice,
        connection: IDeviceConnection,
        type: PrinterType
    ) {
        printerConnections[type] = PrinterInfo(device, connection, type)
    }
    
    /**
     * Disconnect specific printer type
     */
    fun disconnect(type: PrinterType) {
        printerConnections[type]?.connection?.close()
        printerConnections.remove(type)
    }
    
    /**
     * Disconnect all printers
     */
    fun disconnectAll() {
        printerConnections.values.forEach { it.connection.close() }
        printerConnections.clear()
    }
    
    /**
     * Legacy support: Get receipt printer as default
     */
    @Deprecated("Use getCurrentPrinter(PrinterType.RECEIPT) instead")
    var currentPrinter: BluetoothDevice?
        get() = getCurrentPrinter(PrinterType.RECEIPT)
        set(value) {
            // This is for backward compatibility only
        }
    
    /**
     * Legacy support: Get receipt printer connection as default
     */
    @Deprecated("Use getCurrentConnection(PrinterType.RECEIPT) instead")
    var currentConnection: IDeviceConnection?
        get() = getCurrentConnection(PrinterType.RECEIPT)
        set(value) {
            // This is for backward compatibility only
        }
    
    /**
     * Legacy support: Check if receipt printer is connected
     */
    @Deprecated("Use isConnected(PrinterType.RECEIPT) instead")
    fun isConnected(): Boolean {
        return isConnected(PrinterType.RECEIPT)
    }
    
    /**
     * Legacy support: Connect to receipt printer
     */
    @Deprecated("Use connectBluetooth with PrinterType parameter instead")
    fun connectBluetooth(macAddress: String, listener: IConnectListener) {
        connectBluetooth(macAddress, PrinterType.RECEIPT, listener)
    }
    
    /**
     * Legacy support: Disconnect receipt printer
     */
    @Deprecated("Use disconnect(PrinterType.RECEIPT) instead")
    fun disconnect() {
        disconnect(PrinterType.RECEIPT)
    }
}

