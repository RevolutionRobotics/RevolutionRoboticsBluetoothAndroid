package com.revolution.bluetooth.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.revolution.bluetooth.exception.BLEConnectionException
import com.revolution.bluetooth.exception.BLEException
import java.util.UUID

class RoboticsMotorService : RoboticsBLEService() {

    companion object {
        const val SERVICE_ID = "d2d5558c-5b9d-11e9-8647-d663bd873d93"
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)

    private val successCallbackMap = hashMapOf<UUID, (ByteArray) -> Unit>()
    private val errorCallbackMap = hashMapOf<UUID, (exception: BLEException) -> Unit>()

    enum class Motor(val characteristic: UUID) {
        M1(UUID.fromString("4bdfb409-93cc-433a-83bd-7f4f8e7eaf54")),
        M2(UUID.fromString("454885b9-c9d1-4988-9893-a0437d5e6e9f")),
        M3(UUID.fromString("00fcd93b-0c3c-4940-aac1-b4c21fac3420")),
        M4(UUID.fromString("49aaeaa4-bb74-4f84-aa8f-acf46e5cf922")),
        M5(UUID.fromString("ceea8e45-5ff9-4325-be13-48cf40c0e0c3")),
        M6(UUID.fromString("8e4c474f-188e-4d2a-910a-cf66f674f569"))
    }

    override fun disconnect() {
        successCallbackMap.clear()
        errorCallbackMap.clear()
        super.disconnect()
    }

    fun read(motor: Motor, onComplete: (ByteArray) -> Unit, onError: (exception: BLEException) -> Unit) {
        service?.getCharacteristic(motor.characteristic)?.let { characteristic ->
            bluetoothGatt?.let { bluetoothGatt ->
                successCallbackMap[motor.characteristic] = onComplete
                errorCallbackMap[motor.characteristic] = onError

                eventSerializer?.registerEvent {
                    bluetoothGatt.readCharacteristic(characteristic)
                }
            }
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            successCallbackMap[characteristic.uuid]?.let { callback ->
                callback.invoke(characteristic.value)
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

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) =
        Unit

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) = Unit
}
