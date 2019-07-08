package com.revolution.bluetooth.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.revolution.bluetooth.exception.BLEConnectionException
import com.revolution.bluetooth.exception.BLEException
import java.util.UUID

class RoboticsBatteryService : RoboticsBLEService() {

    companion object {
        const val SERVICE_ID = "0000180f-0000-1000-8000-00805f9b34fb"
        val CHARACTERISTIC_PRIMARY_BATTERY: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_MOTOR_BATTERY: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fa")
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)
    private val successCallbackMap = hashMapOf<UUID, (Int) -> Unit>()
    private val errorCallbackMap = hashMapOf<UUID, (exception: BLEException) -> Unit>()

    override fun disconnect() {
        successCallbackMap.clear()
        errorCallbackMap.clear()
        super.disconnect()
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            successCallbackMap[characteristic.uuid]?.let { callback ->
                callback.invoke(characteristic.value[0].toInt())
                errorCallbackMap.remove(characteristic.uuid)
                successCallbackMap.remove(characteristic.uuid)
            }
        } else {
            errorCallbackMap[characteristic.uuid]?.let { callback ->
                callback.invoke(BLEConnectionException(status))
                successCallbackMap.remove(characteristic.uuid)
                errorCallbackMap.remove(characteristic.uuid)
            }
        }
    }

    fun getPrimaryBattery(onComplete: (Int) -> Unit, onError: (exception: BLEException) -> Unit) {
        service?.getCharacteristic(CHARACTERISTIC_PRIMARY_BATTERY)?.let { characteristic ->
            bluetoothGatt?.let { bluetoothGatt ->
                successCallbackMap[CHARACTERISTIC_PRIMARY_BATTERY] = onComplete
                errorCallbackMap[CHARACTERISTIC_PRIMARY_BATTERY] = onError

                eventSerializer?.registerEvent {
                    bluetoothGatt.readCharacteristic(characteristic)
                }
            }
        }
    }

    fun getMotorBattery(onComplete: (Int) -> Unit, onError: (exception: BLEException) -> Unit) {
        service?.getCharacteristic(CHARACTERISTIC_MOTOR_BATTERY)?.let { characteristic ->
            bluetoothGatt?.let { bluetoothGatt ->
                successCallbackMap[CHARACTERISTIC_MOTOR_BATTERY] = onComplete
                errorCallbackMap[CHARACTERISTIC_MOTOR_BATTERY] = onError

                eventSerializer?.registerEvent {
                    bluetoothGatt.readCharacteristic(characteristic)
                }
            }
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) =
        Unit

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) = Unit
}
