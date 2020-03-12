package org.revolutionrobotics.bluetooth.android.service

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import org.revolutionrobotics.bluetooth.android.communication.NRoboticsDeviceConnector
import java.util.UUID

abstract class RoboticsBLEService(
    protected val deviceConnector: NRoboticsDeviceConnector
) {

    abstract val serviceId: UUID

    protected var service: BluetoothGattService? = null
    protected var bluetoothGatt: BluetoothGatt? = null

    protected var eventSerializer: RoboticsEventSerializer? = null

    open fun init(bluetoothGatt: BluetoothGatt, roboticsEventSerializer: RoboticsEventSerializer) {
        this.bluetoothGatt = bluetoothGatt
        eventSerializer = roboticsEventSerializer
        service = bluetoothGatt.getService(serviceId)
    }

    open fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt = null
        service = null
    }

    protected fun readMessage(characteristic: BluetoothGattCharacteristic?, callback: (Data) -> Unit) {
        deviceConnector.readCharacteristic(characteristic)
            .with { device, data -> callback.invoke(data) }
            .enqueue()
    }

    protected fun writeMessage(characteristic: BluetoothGattCharacteristic?, byteArray: ByteArray, done: () -> Unit) {
        characteristic?.let {
            deviceConnector.writeCharacteristic(
                characteristic,
                byteArray
            )
                .done { done.invoke() }
                .enqueue()
        }

    }
}
