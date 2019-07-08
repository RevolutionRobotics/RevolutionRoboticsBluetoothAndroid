package com.revolution.bluetooth.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.revolution.bluetooth.exception.BLEConnectionException
import com.revolution.bluetooth.exception.BLEException
import java.util.UUID

@Suppress("TooManyFunctions")
class RoboticsDeviceService : RoboticsBLEService() {

    companion object {
        const val SERVICE_ID = "0000180a-0000-1000-8000-00805f9b34fb"

        val SERIAL_NUMBER_CHARACTERISTIC: UUID = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
        val MANUFACTURER_CHARACTERISTIC: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        val HW_REVISION_CHARACTERISTIC: UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
        val SW_REVISION_CHARACTERISTIC: UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")
        val FW_REVISION_CHARACTERISTIC: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        val SYSTEM_ID_CHARACTERISTIC: UUID = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb")
        val MODEL_NUMBER_CHARACTERISTIC: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)
    private val successCallbackMap = hashMapOf<UUID, (String) -> Unit>()
    private val errorCallbackMap = hashMapOf<UUID, (exception: BLEException) -> Unit>()

    override fun disconnect() {
        successCallbackMap.clear()
        errorCallbackMap.clear()
        super.disconnect()
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            successCallbackMap[characteristic.uuid]?.let { callback ->
                callback.invoke(characteristic.getStringValue(0))
                successCallbackMap.remove(characteristic.uuid)
                errorCallbackMap.remove(characteristic.uuid)
            }
        } else {
            errorCallbackMap[characteristic.uuid]?.let { callback ->
                callback.invoke(BLEConnectionException(status))
                successCallbackMap.remove(characteristic.uuid)
                errorCallbackMap.remove(characteristic.uuid)
            }
        }
    }

    fun getSerialNumber(onCompleted: (String) -> Unit, onError: (exception: BLEException) -> Unit) {
        readCharacteristic(SERIAL_NUMBER_CHARACTERISTIC, onCompleted, onError)
    }

    fun getManufacturerName(onCompleted: (String) -> Unit, onError: (exception: BLEException) -> Unit) {
        readCharacteristic(MANUFACTURER_CHARACTERISTIC, onCompleted, onError)
    }

    fun getHardwareRevision(onCompleted: (String) -> Unit, onError: (exception: BLEException) -> Unit) {
        readCharacteristic(HW_REVISION_CHARACTERISTIC, onCompleted, onError)
    }

    fun getSoftwareRevision(onCompleted: (String) -> Unit, onError: (exception: BLEException) -> Unit) {
        readCharacteristic(SW_REVISION_CHARACTERISTIC, onCompleted, onError)
    }

    fun getFirmwareRevision(onCompleted: (String) -> Unit, onError: (exception: BLEException) -> Unit) {
        readCharacteristic(FW_REVISION_CHARACTERISTIC, onCompleted, onError)
    }

    fun getSystemId(onCompleted: (String) -> Unit, onError: (exception: BLEException) -> Unit) {
        readCharacteristic(SYSTEM_ID_CHARACTERISTIC, onCompleted, onError)
    }

    fun getModelNumber(onCompleted: (String) -> Unit, onError: (exception: BLEException) -> Unit) {
        readCharacteristic(MODEL_NUMBER_CHARACTERISTIC, onCompleted, onError)
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) =
        Unit

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic
    ) = Unit

    private fun readCharacteristic(
        uuid: UUID,
        onCompleted: (String) -> Unit,
        onError: (exception: BLEException) -> Unit
    ) {
        successCallbackMap[uuid] = onCompleted
        errorCallbackMap[uuid] = onError
        bluetoothGatt?.let { gatt ->
            service?.let { service ->
                eventSerializer?.registerEvent {
                    gatt.readCharacteristic(service.getCharacteristic(uuid))
                }
            }
        }
    }
}
