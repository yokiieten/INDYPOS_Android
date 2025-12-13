package com.indybrain.indypos_Android.core.printer

import android.bluetooth.BluetoothDevice
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for printer connection state
 * Similar to PrinterManager in iOS
 */
@Singleton
class PrinterManager @Inject constructor() {
    var currentPrinter: BluetoothDevice? = null
    var currentConnection: IDeviceConnection? = null
    
    fun isConnected(): Boolean {
        return currentConnection?.isConnect == true
    }
    
    fun connectBluetooth(macAddress: String, listener: IConnectListener) {
        currentConnection?.close()
        currentConnection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)
        currentConnection?.connect(macAddress, listener)
    }
    
    fun disconnect() {
        currentConnection?.close()
        currentConnection = null
        currentPrinter = null
    }
}

