package org.revolutionrobotics.bluetooth.android.service

import org.revolutionrobotics.bluetooth.android.communication.RoboticsDeviceConnector
import org.revolutionrobotics.bluetooth.android.exception.BLEConnectionException
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import java.nio.ByteBuffer
import java.util.*

class RoboticsMotorService(
    deviceConnector: RoboticsDeviceConnector
) : RoboticsBLEService(deviceConnector) {

    companion object {
        const val SERVICE_ID = "d2d5558c-5b9d-11e9-8647-d663bd873d93"
        const val MOTOR_MESSAGE_SIZE = 9

        @ExperimentalUnsignedTypes
        fun getMotorInfoFromBytes(bytes: ByteArray): MotorInfo? {
            val buffer = ByteBuffer.wrap(bytes)
            return if (bytes.size >= MOTOR_MESSAGE_SIZE) {
                MotorInfo(
                    speed = buffer.float,
                    position = buffer.int,
                    power = buffer.get().toUByte().toInt()
                )
            } else {
                null
            }
        }
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)

    enum class Motor(val characteristic: UUID) {
        M1(UUID.fromString("4bdfb409-93cc-433a-83bd-7f4f8e7eaf54")),
        M2(UUID.fromString("454885b9-c9d1-4988-9893-a0437d5e6e9f")),
        M3(UUID.fromString("00fcd93b-0c3c-4940-aac1-b4c21fac3420")),
        M4(UUID.fromString("49aaeaa4-bb74-4f84-aa8f-acf46e5cf922")),
        M5(UUID.fromString("ceea8e45-5ff9-4325-be13-48cf40c0e0c3")),
        M6(UUID.fromString("8e4c474f-188e-4d2a-910a-cf66f674f569"))
    }

    fun read(motor: Motor, onComplete: (ByteArray) -> Unit, onError: (exception: BLEException) -> Unit) {
        service?.getCharacteristic(motor.characteristic)?.let { characteristic ->
            deviceConnector.readCharacteristic(characteristic)
                .with { _, data -> onComplete(data.value ?: ByteArray(0)) }
                .fail { _, status -> onError(BLEConnectionException(status)) }
                .enqueue()
        }
    }
}
