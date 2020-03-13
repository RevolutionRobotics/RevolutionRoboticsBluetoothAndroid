package org.revolutionrobotics.bluetooth.android.service

import org.revolutionrobotics.bluetooth.android.communication.NRoboticsDeviceConnector
import org.revolutionrobotics.bluetooth.android.exception.BLEConnectionException
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import java.util.*

class RoboticsBatteryService(
    deviceConnector: NRoboticsDeviceConnector
) : RoboticsBLEService(deviceConnector) {

    companion object {
        const val SERVICE_ID = "0000180f-0000-1000-8000-00805f9b34fb"
        val CHARACTERISTIC_PRIMARY_BATTERY: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_MOTOR_BATTERY: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fa")
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)

    fun getPrimaryBattery(onComplete: (Int) -> Unit, onError: (exception: BLEException) -> Unit) {
        service?.getCharacteristic(CHARACTERISTIC_PRIMARY_BATTERY)?.let { characteristic ->
            deviceConnector.readCharacteristic(characteristic)
                .with { _, data ->  onComplete(data.value?.get(0)?.toInt() ?: 0)}
                .fail { _, status -> onError(BLEConnectionException(status)) }
                .enqueue()
        }
    }

    fun getMotorBattery(onComplete: (Int) -> Unit, onError: (exception: BLEException) -> Unit) {
        service?.getCharacteristic(CHARACTERISTIC_MOTOR_BATTERY)?.let { characteristic ->
            deviceConnector.readCharacteristic(characteristic)
                .with { _, data -> onComplete(data.value?.get(0)?.toInt() ?: 0) }
                .fail { _, status -> onError(BLEConnectionException(status)) }
                .enqueue()
        }
    }
}
