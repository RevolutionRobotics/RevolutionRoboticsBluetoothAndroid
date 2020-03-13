package org.revolutionrobotics.bluetooth.android.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import no.nordicsemi.android.ble.data.Data
import org.revolutionrobotics.bluetooth.android.communication.RoboticsDeviceConnector
import java.util.UUID

abstract class RoboticsBLEService(
    protected val deviceConnector: RoboticsDeviceConnector
) {

    abstract val serviceId: UUID

    protected var service: BluetoothGattService? = null
    protected var bluetoothGatt: BluetoothGatt? = null

    open fun init(bluetoothGatt: BluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt
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
