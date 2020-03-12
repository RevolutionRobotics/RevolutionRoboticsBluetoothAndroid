package org.revolutionrobotics.bluetooth.android.communication

import android.util.Log
import no.nordicsemi.android.ble.data.DataSplitter
import org.revolutionrobotics.bluetooth.android.service.RoboticsConfigurationService

class LongMessageSplitter(
    private val firstByte: Byte,
    private val chunkLength: Int // for some reason setting the MTU doesn't work on some devices
) : DataSplitter {

    override fun chunk(message: ByteArray, index: Int, maxLength: Int): ByteArray? {
        val dataLength = chunkLength - 1
        val startIndex = index * dataLength
        return if (startIndex < message.size) {
            val endIndex = (startIndex + dataLength).coerceAtMost(message.size)
            val chunk = ByteArray(endIndex - startIndex + 1).apply {
                this[0] = firstByte
                message.copyInto(this, 1, startIndex, endIndex)
            }
            Log.d(RoboticsConfigurationService.TAG, "Write message sent: ${chunk.toStringCustom()}")
            chunk
        } else {
            null
        }
    }

    private fun ByteArray.toStringCustom(): String = this.joinToString { it.toString() }
}