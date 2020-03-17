package org.revolutionrobotics.bluetooth.android.service

import org.revolutionrobotics.bluetooth.android.communication.RoboticsDeviceConnector
import org.revolutionrobotics.bluetooth.android.exception.BLEConnectionException
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import java.util.*

@Suppress("TooManyFunctions")
class RoboticsDeviceService(
    deviceConnector: RoboticsDeviceConnector
) : RoboticsBLEService(deviceConnector) {

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

    private fun readCharacteristic(
        uuid: UUID,
        onCompleted: (String) -> Unit,
        onError: (exception: BLEException) -> Unit
    ) {
        service?.let { service ->
            deviceConnector.readCharacteristic(service.getCharacteristic(uuid))
                .with { _, data -> onCompleted(data.getStringValue(0) ?: "") }
                .fail { _, status -> onError(BLEConnectionException(status)) }
                .enqueue()
        }
    }
}
