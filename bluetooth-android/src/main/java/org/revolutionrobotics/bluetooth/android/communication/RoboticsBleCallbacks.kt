package org.revolutionrobotics.bluetooth.android.communication

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.BleManagerCallbacks

class RoboticsBleCallbacks: BleManagerCallbacks {

    private var connectionListeners = mutableSetOf<RoboticsConnectionStatusListener>()

    fun registerConnectionListener(listener: RoboticsConnectionStatusListener) {
        connectionListeners.add(listener)
    }

    fun unregisterConnectionListener(listener: RoboticsConnectionStatusListener) {
        connectionListeners.remove(listener)
    }

    override fun onDeviceDisconnecting(device: BluetoothDevice) {
        Log.d("BLE", "Disconnecting")
    }

    override fun onDeviceDisconnected(device: BluetoothDevice) {
        Log.d("BLE", "Disconnected")
        connectionListeners.forEach {
            it.onConnectionStateChanged(false)
        }
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        Log.d("BLE", "Connected")
    }

    override fun onDeviceNotSupported(device: BluetoothDevice) {
        Log.d("BLE", "DeviceNotSupported")
    }

    override fun onBondingFailed(device: BluetoothDevice) {
        Log.d("BLE", "BondingFailed")
    }

    override fun onServicesDiscovered(device: BluetoothDevice, optionalServicesFound: Boolean) {
        Log.d("BLE", "ServicesDiscovered")
        connectionListeners.forEach {
            it.onConnectionStateChanged(true)
        }
    }

    override fun onBondingRequired(device: BluetoothDevice) {
        Log.d("BLE", "BondingRequired")
    }

    override fun onLinkLossOccurred(device: BluetoothDevice) {
        Log.d("BLE", "LinkLossOccurred")
    }

    override fun onBonded(device: BluetoothDevice) {
        Log.d("BLE", "Bonded")
    }

    override fun onDeviceReady(device: BluetoothDevice) {
        Log.d("BLE", "DeviceReady")
    }

    override fun onError(device: BluetoothDevice, message: String, errorCode: Int) {
        Log.d("BLE", "Error")
    }

    override fun onDeviceConnecting(device: BluetoothDevice) {
        Log.d("BLE", "Connecting")
    }
}