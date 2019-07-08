package com.revolution.bluetooth.service

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.revolution.bluetooth.exception.BLEConnectionException
import com.revolution.bluetooth.exception.BLEException
import com.revolution.bluetooth.exception.BLELongMessageIsAlreadyRunning
import com.revolution.bluetooth.exception.BLELongMessageValidationException
import com.revolution.bluetooth.exception.BLEMessageInterruptedException
import com.revolution.bluetooth.exception.BLESendingTimeoutException
import com.revolution.bluetooth.file.FileChunkHandler
import com.revolution.bluetooth.file.MD5Checker
import java.util.UUID

// TODO Remove logs
@Suppress("TooManyFunctions")
class RoboticsConfigurationService : RoboticsBLEService() {

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

        const val MAX_VALIDATION_COUNT = 30
        const val MD5_LENGTH = 16
        const val DEFAULT_MTU = 512

        const val MTU_DECREASE = 4

        const val TAG = "LongMessage"

        val CHARACTERISTIC: UUID = UUID.fromString("d59bb321-7218-4fb9-abac-2f6814f31a4d")
    }

    override val serviceId: UUID = UUID.fromString(SERVICE_ID)
    private val md5Checker = MD5Checker()
    private val fileChunkHandler = FileChunkHandler()

    var success: (() -> Unit)? = null
    var error: ((exception: BLEException) -> Unit)? = null
    var currentFile: Uri? = null
    var validationCounter = 0

    var uploadStarted = false
    var mtu = DEFAULT_MTU

    fun updateFirmware(file: Uri, onSuccess: () -> Unit, onError: (exception: BLEException) -> Unit) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_FIRMWARE)
    }

    fun updateFramework(file: Uri, onSuccess: () -> Unit, onError: (exception: BLEException) -> Unit) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_FRAMEWORK)
    }

    fun testKit(file: Uri, onSuccess: () -> Unit, onError: (exception: BLEException) -> Unit) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_TESTKIT)
    }

    fun sendConfiguration(file: Uri, onSuccess: () -> Unit, onError: (exception: BLEException) -> Unit) {
        initLongMessage(file, onSuccess, onError, FUNCTION_TYPE_CONFIGURATION)
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
        sendSelectLongMessage(functionType)
    }

    private fun sendSelectLongMessage(typeId: Byte) {
        writeMessage(ByteArray(2).apply {
            set(0, MESSAGE_TYPE_SELECT)
            set(1, typeId)
        })
    }

    private fun checkMd5(serverMd5: ByteArray) {
        currentFile?.let { uri ->
            val currentMD5 = MD5Checker().calculateMD5Hash(uri)
            if (currentMD5.contentEquals(serverMd5)) {
                uploadStarted = true
                sendFinalizeMessage()
            } else {
                startUploading(currentMD5)
            }
        }
    }

    private fun readStatus() {
        service?.getCharacteristic(CHARACTERISTIC)?.let { characteristic ->
            bluetoothGatt?.let { bluetoothGatt ->
                eventSerializer?.registerEvent {
                    bluetoothGatt.readCharacteristic(characteristic)
                }
            }
        }
    }

    private fun startUploading(fileMD5: ByteArray?) {
        uploadStarted = true
        currentFile?.let { currentFile ->
            val md5 = fileMD5 ?: md5Checker.calculateMD5Hash(currentFile)
            Log.e(TAG, "MD5: ${Base64.encodeToString(md5, Base64.DEFAULT)}")
            ByteArray(MD5_LENGTH + 1).apply {
                set(0, MESSAGE_TYPE_INIT)
                for (index in 0 until MD5_LENGTH) {
                    set(index + 1, md5[index])
                }
                writeMessage(this)
            }
        }
    }

    private fun startChunkSending() {
        currentFile?.let {
            fileChunkHandler.init(it, mtu - MTU_DECREASE, MESSAGE_TYPE_UPLOAD)
        }
        sendNextChunk()
    }

    private fun sendNextChunk() {
        val nextChunk = fileChunkHandler.getNextChunk()
        if (nextChunk != null) {
            writeMessage(nextChunk)
        } else {
            sendFinalizeMessage()
        }
    }

    private fun sendFinalizeMessage() {
        ByteArray(1).apply {
            set(0, MESSAGE_TYPE_FINALIZE)
            writeMessage(this)
        }
    }

    private fun writeMessage(byteArray: ByteArray) {
        service?.getCharacteristic(CHARACTERISTIC)?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = byteArray
            val wasMessageSent = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            Log.d(TAG, "Write message sent $wasMessageSent  ${byteArray.toStringCustom()}")
            if (!wasMessageSent) {
                onError(BLEMessageInterruptedException())
            }
        }
    }

    private fun isUploadInProgress() = currentFile != null

    @Suppress("ReturnCount")
    private fun validateCharacteristicEvent(characteristic: BluetoothGattCharacteristic, status: Int): Boolean {
        if (characteristic.uuid != CHARACTERISTIC) {
            return false
        }
        if (status != BluetoothGatt.GATT_SUCCESS) {
            onError(BLEConnectionException(status))
            return false
        }

        return true
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (!validateCharacteristicEvent(characteristic, status)) {
            return
        }
        Log.d(TAG, "Read happened: ${characteristic.value.toStringCustom()}")
        when (characteristic.value[0]) {
            STATUS_UNUSED -> {
                Log.d(TAG, "Unused --> start uploading")
                startUploading(null)
            }
            STATUS_UPLOAD -> {
                Log.d(TAG, "Upload --> checkMd5")
                startUploading(null)
            }
            STATUS_VALIDATION -> handleValidationStatus()
            STATUS_READY -> handleReadyStatus(characteristic)
            STATUS_VALIDATION_ERROR -> onError(BLELongMessageValidationException())
        }
    }

    private fun onError(exception: BLEException) {
        Log.d(TAG, "Error --> send error event")
        error?.invoke(exception)
        resetVariables()
    }

    private fun handleValidationStatus() {
        Log.d(TAG, "Validation attempt:$validationCounter")
        if (validationCounter < MAX_VALIDATION_COUNT) {
            readStatus()
            validationCounter++
        } else {
            onError(BLESendingTimeoutException())
        }
    }

    private fun handleReadyStatus(characteristic: BluetoothGattCharacteristic) {
        if (!uploadStarted) {
            Log.d(TAG, "Ready --> checkMd5")
            checkMd5(characteristic.value.copyOfRange(1, MD5_LENGTH + 1))
        } else {
            Log.d(TAG, "Ready --> send success event")
            success?.invoke()
            resetVariables()
        }
    }

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

    override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
        if (!validateCharacteristicEvent(characteristic, status)) {
            return
        }
        Log.d(
            TAG,
            "Write happened! status: $status First byte: ${characteristic.value[0]} " +
                    "Total message: ${characteristic.value.toStringCustom()}"
        )
        when (characteristic.value[0]) {
            MESSAGE_TYPE_SELECT -> {
                Log.d(TAG, "Select --> read status")
                readStatus()
            }
            MESSAGE_TYPE_INIT -> {
                Log.d(TAG, "Init --> startChunkSending")
                startChunkSending()
            }
            MESSAGE_TYPE_UPLOAD -> {
                Log.d(TAG, "Upload --> sendNextChunk")
                sendNextChunk()
            }
            MESSAGE_TYPE_FINALIZE -> {
                Log.d(TAG, "Finalize --> startStatusReadingLoop")
                readStatus()
            }
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) = Unit

    private fun ByteArray.toStringCustom(): String = this.joinToString { it.toString() }
}
