package com.revolution.bluetooth.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

abstract class RoboticsBLEService {

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

    abstract fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int)
    abstract fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int)
    abstract fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic)
}
