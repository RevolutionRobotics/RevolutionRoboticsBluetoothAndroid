package org.revolutionrobotics.bluetooth.android.service

import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.data.Data
import org.revolutionrobotics.bluetooth.android.communication.LongMessageSplitter
import org.revolutionrobotics.bluetooth.android.communication.RoboticsDeviceConnector
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import org.revolutionrobotics.bluetooth.android.exception.BLELongMessageIsAlreadyRunning
import org.revolutionrobotics.bluetooth.android.exception.BLELongMessageValidationException
import org.revolutionrobotics.bluetooth.android.file.MD5Checker
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.TimeUnit


// TODO Remove logs
@Suppress("TooManyFunctions")
class RoboticsConfigurationService(
    deviceConnector: RoboticsDeviceConnector
) : RoboticsBLEService(deviceConnector) {

    companion object {
        const val SERVICE_ID = "97148a03-5b9d-11e9-8647-d663bd873d93"

        const val FUNCTION_TYPE_FIRMWARE = 1.toByte()
        const val FUNCTION_TYPE_FRAMEWORK = 2.toByte()
        const val FUNCTION_TYPE_CONFIGURATION = 3.toByte()
        const val FUNCTION_TYPE_TESTKIT = 4.toByte()

        const val MESSAGE_TYPE_SELECT = 0.toByte()
        const val MESSAGE_TYPE_INIT = 1.toByte()
        const val MESSAGE_TYPE_UPLOAD = 2.toByte()
        const val MESSAGE_TYPE_FINALIZE = 3.toByte()

        const val STATUS_UNUSED = 0.toByte()
        const val STATUS_UPLOAD = 1.toByte()
        const val STATUS_VALIDATION = 2.toByte()
        const val STATUS_READY = 3.toByte()
        const val STATUS_VALIDATION_ERROR = 4.toByte()

        const val MD5_LENGTH = 16
        const val DEFAULT_MTU = 512

        const val TAG = "LongMessage"

        val CHARACTERISTIC: UUID = UUID.fromString("d59bb321-7218-4fb9-abac-2f6814f31a4d")
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)
    private val md5Checker = MD5Checker()

    var success: (() -> Unit)? = null
    var error: ((exception: BLEException) -> Unit)? = null
    var currentFile: Uri? = null
    var validationCounter = 0

    var uploadStarted = false

    fun updateFirmware(
        file: Uri,
        onSuccess: () -> Unit,
        onError: (exception: BLEException) -> Unit
    ) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_FIRMWARE)
    }

    fun updateFramework(
        file: Uri,
        onSuccess: () -> Unit,
        onError: (exception: BLEException) -> Unit
    ) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_FRAMEWORK)
    }

    fun testKit(file: Uri, onSuccess: () -> Unit, onError: (exception: BLEException) -> Unit) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_TESTKIT)
    }

    fun sendConfiguration(
        file: Uri,
        onSuccess: () -> Unit,
        onError: (exception: BLEException) -> Unit
    ) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_CONFIGURATION)
    }

    fun stop() {
        if (currentFile != null) {
            GlobalScope.launch {
                delay(TimeUnit.SECONDS.toMillis(1))
                sendFinalizeMessage()
            }
            resetVariables()
        }
    }

    private fun initLongMessage(
        file: Uri,
        onSuccess: () -> Unit,
        onError: (exception: BLEException) -> Unit,
        functionType: Byte
    ) {
        if (isUploadInProgress()) {
            onError.invoke(BLELongMessageIsAlreadyRunning())
            return
        }
        currentFile = file
        success = onSuccess
        error = onError
        sendTypeSelectMessage(functionType)
    }

    private fun sendTypeSelectMessage(functionType: Byte) {
        writeMessage(generateSelectTypeMessage(functionType)) {
            readMessage { data ->
                if (data.value?.get(0) == STATUS_UNUSED
                    || data.value?.get(0) == STATUS_UPLOAD
                ) {
                    Log.d(TAG, "Unused --> start uploading")
                    sendInit()
                } else if (data.value?.get(0) == STATUS_READY) {
                    Log.d(TAG, "Ready --> checkMd5")
                    service?.getCharacteristic(CHARACTERISTIC)?.let {
                        if (checkMd5(it.value.copyOfRange(1, MD5_LENGTH + 1))) {
                            sendFinalizeMessage()
                        } else {
                            sendInit()
                        }
                    }
                }
            }
        }
    }

    private fun generateSelectTypeMessage(functionType: Byte) = ByteArray(2).apply {
        set(0, MESSAGE_TYPE_SELECT)
        set(1, functionType)
    }


    private fun checkMd5(serverMd5: ByteArray): Boolean {
        currentFile?.let { uri ->
            val currentMD5 = MD5Checker().calculateMD5Hash(uri)
            return if (currentMD5.contentEquals(serverMd5)) {
                uploadStarted = true
                true
            } else {
                false
            }
        }
        return false
    }

    private fun sendInit() {
        uploadStarted = true
        currentFile?.let { currentFile ->
            val md5 = md5Checker.calculateMD5Hash(currentFile)
            Log.e(TAG, "MD5: ${Base64.encodeToString(md5, Base64.DEFAULT)}")
            ByteArray(MD5_LENGTH + 1).apply {
                set(0, MESSAGE_TYPE_INIT)
                for (index in 0 until MD5_LENGTH) {
                    set(index + 1, md5[index])
                }
                writeMessage(this) {
                    startChunkSending()
                }
            }
        }
    }

    private fun startChunkSending() {
        Log.d(TAG, "Starting chunk sending")
        currentFile?.let {
            writeLongMessage(
                FileInputStream(it.path).readBytes(),
                MESSAGE_TYPE_UPLOAD
            ) {
                sendFinalizeMessage()
            }
        }
    }

    private fun sendFinalizeMessage() {
        Log.d(TAG, "Sending finalize")
        ByteArray(1).apply {
            set(0, MESSAGE_TYPE_FINALIZE)
            writeMessage(this) {
                readMessage {
                    if (it.value?.get(0) == STATUS_READY) {
                        Log.d(TAG, "Ready --> send success event")
                        success?.invoke()
                        resetVariables()
                    } else {
                        Log.d(TAG, "Error --> send error event")
                        error?.invoke(BLELongMessageValidationException())
                        resetVariables()
                    }
                }
            }
        }
    }

    private fun readMessage(callback: (Data) -> Unit) {
        readMessage(service?.getCharacteristic(CHARACTERISTIC)) {
            Log.d(TAG, "Received: " + (it.value?.get(0) ?: "null"))
            callback.invoke(it)
        }
    }

    private fun writeMessage(byteArray: ByteArray, done: () -> Unit) {
        writeMessage(
            service?.getCharacteristic(CHARACTERISTIC),
            byteArray
        ) {
            Log.d(TAG, "Write message sent: ${byteArray.toStringCustom()}")
            done.invoke()
        }
    }

    private fun writeLongMessage(byteArray: ByteArray, messageType: Byte, done: () -> Unit) {
        service?.getCharacteristic(CHARACTERISTIC)?.let {
            deviceConnector.writeCharacteristic(
                it,
                byteArray
            )
                .split(LongMessageSplitter(messageType))
                .done { done.invoke() }
                .enqueue()
        }
    }

    private fun isUploadInProgress() = currentFile != null

    private fun resetVariables() {
        uploadStarted = false
        success = null
        error = null
        currentFile = null
        validationCounter = 0
    }

    override fun disconnect() {
        super.disconnect()
        resetVariables()
    }

    private fun ByteArray.toStringCustom(): String = this.joinToString { it.toString() }
}
