package org.revolutionrobotics.bluetooth.android.service

import androidx.annotation.IntRange
import org.revolutionrobotics.bluetooth.android.communication.RoboticsDeviceConnector
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Suppress("TooManyFunctions")
class RoboticsLiveControllerService(
    deviceConnector: RoboticsDeviceConnector
) : RoboticsBLEService(deviceConnector) {

    companion object {
        const val SERVICE_ID = "d2d5558c-5b9d-11e9-8647-d663bd873d93"

        val CHARACTERISTIC_ID: UUID = UUID.fromString("7486bec3-bb6b-4abd-a9ca-20adc281a0a4")

        const val DELAY_TIME_IN_MILLIS = 100L
        const val COUNTER_MAX = 16

        const val POSITION_KEEP_ALIVE = 1
        const val POSITION_X_COORD = 1
        const val POSITION_Y_COORD = 2
        const val POSITION_BUTTON = 11

        const val MESSAGE_LENGTH = 20
        const val MAX_BYTE_MASK = 255.toByte()
        const val DEFAULT_COORDINATE = 127.toByte()
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)

    private var x = DEFAULT_COORDINATE
    private var y = DEFAULT_COORDINATE
    private var buttonByte = 0.toByte()
    private var counter = 0
    private var timer: Timer? = null

    override fun disconnect() {
        stop()
        service = null
        super.disconnect()
    }

    fun start() {
        stop()
        counter = 0
        timer = fixedRateTimer("default", false, 0L, DELAY_TIME_IN_MILLIS) {
            incrementCounter()
            service?.getCharacteristic(CHARACTERISTIC_ID)?.let { characteristic ->
                deviceConnector.writeCharacteristic(
                    characteristic,
                    generateMessage(counter)
                ).enqueue()
            }
        }
    }

    fun stop() {
        buttonByte = 0.toByte()
        timer?.cancel()
    }

    private fun incrementCounter() {
        counter++
        if (counter == COUNTER_MAX) {
            counter = 0
        }
    }

    fun updateXDirection(@IntRange(from = 0, to = 255) x: Int) {
        this.x = x.toByte()
    }

    fun updateYDirection(@IntRange(from = 0, to = 255) y: Int) {
        this.y = y.toByte()
    }

    fun onButtonPressed(@IntRange(from = 0, to = 8) buttonIndex: Int) {
        buttonByte = buttonByte or getMaskBasedOnIndex(buttonIndex)
    }

    fun onButtonReleased(@IntRange(from = 0, to = 8) buttonIndex: Int) {
        buttonByte = buttonByte and getMaskBasedOnIndex(buttonIndex).inv()
    }

    private fun getMaskBasedOnIndex(buttonIndex: Int) = (2 pow buttonIndex).toByte()

    private fun generateMessage(counter: Int) = ByteArray(MESSAGE_LENGTH).apply {
        this[POSITION_KEEP_ALIVE] = counter.toByte()
        this[POSITION_X_COORD] = x
        this[POSITION_Y_COORD] = y
        this[POSITION_BUTTON] = buttonByte
    }

    @Suppress("UnusedPrivateMember")
    private infix fun Int.pow(exponent: Int): Int {
        var res = 1
        repeat(exponent.downTo(1).count()) {
            res *= this
        }
        return res
    }
}
