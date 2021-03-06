package org.revolutionrobotics.bluetooth.android.service

import org.revolutionrobotics.bluetooth.android.communication.RoboticsDeviceConnector
import org.revolutionrobotics.bluetooth.android.exception.BLEConnectionException
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class RoboticsSensorService(
    deviceConnector: RoboticsDeviceConnector
) : RoboticsBLEService(deviceConnector) {

    companion object {
        const val SERVICE_ID = "d2d5558c-5b9d-11e9-8647-d663bd873d93"
        const val ULTRASOUND_MESSAGE_SIZE = 5

        fun getBumberInfo(bytes: ByteArray): BumperInfo? = if (bytes.isEmpty()) null else BumperInfo(
            pressed = bytes[1] == 2.toByte()
        )

        fun getUltrasoundInfo(bytes: ByteArray): UltrasoundInfo? {
            return if (bytes.size >= ULTRASOUND_MESSAGE_SIZE) {
                val buffer = ByteBuffer.wrap(bytes)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                UltrasoundInfo(distance = buffer.int)
            } else {
                null
            }
        }
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)

    enum class Sensor(val characteristic: UUID) {
        S1(UUID.fromString("135032e6-3e86-404f-b0a9-953fd46dcb17")),
        S2(UUID.fromString("36e944ef-34fe-4de2-9310-394d482e20e6")),
        S3(UUID.fromString("b3a71566-9af2-4c9d-bc4a-6f754ab6fcf0")),
        S4(UUID.fromString("9ace575c-0b70-4ed5-96f1-979a8eadbc6b")),
    }

    fun read(sensor: Sensor, onComplete: (ByteArray) -> Unit, onError: (exception: BLEException) -> Unit) {
        service?.getCharacteristic(sensor.characteristic)?.let { characteristic ->
            deviceConnector.readCharacteristic(characteristic)
                .with { _, data -> onComplete(data.value?: ByteArray(0)) }
                .fail { _, status -> onError(BLEConnectionException(status)) }
                .enqueue()
        }
    }
}
