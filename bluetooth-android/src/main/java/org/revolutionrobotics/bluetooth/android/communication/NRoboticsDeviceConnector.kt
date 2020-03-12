package org.revolutionrobotics.bluetooth.android.communication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.IntentFilter
import no.nordicsemi.android.ble.*
import no.nordicsemi.android.ble.data.Data
import org.revolutionrobotics.bluetooth.android.domain.Device
import org.revolutionrobotics.bluetooth.android.exception.BLEConnectionException
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import org.revolutionrobotics.bluetooth.android.service.*
import org.revolutionrobotics.bluetooth.android.threading.moveToUIThread

class NRoboticsDeviceConnector(context: Context): BleManager<BleManagerCallbacks>(context) {

    override fun getGattCallback(): BleManagerGattCallback {
        return GattCallback()
    }

    private var gattConnection: BluetoothGatt? = null

    private val roboticEventSerializer = RoboticsEventSerializer()
    private val bluetoothBroadcastReceiver =
        RoboticsBluetoothBroadcastReceiver(this)
    private val callbacks = RoboticsBleCallbacks()


    val deviceService: RoboticsDeviceService
        get() {
            return services.first { it is RoboticsDeviceService } as RoboticsDeviceService
        }
    val liveControllerService: RoboticsLiveControllerService
        get() {
            return services.first { it is RoboticsLiveControllerService } as RoboticsLiveControllerService
        }
    val batteryService: RoboticsBatteryService
        get() {
            return services.first { it is RoboticsBatteryService } as RoboticsBatteryService
        }
    val configurationService: RoboticsConfigurationService
        get() {
            return services.first { it is RoboticsConfigurationService } as RoboticsConfigurationService
        }
    val motorService: RoboticsMotorService
        get() {
            return services.first { it is RoboticsMotorService } as RoboticsMotorService
        }
    val sensorService: RoboticsSensorService
        get() {
            return services.first { it is RoboticsSensorService } as RoboticsSensorService
        }

    private val services = setOf(
        RoboticsDeviceService(this),
        RoboticsLiveControllerService(this),
        RoboticsBatteryService(this),
        RoboticsConfigurationService(this),
        RoboticsMotorService(this),
        RoboticsSensorService(this)
    )

    init {
        setGattCallbacks(callbacks)
    }

    fun connect(
        context: Context,
        device: Device,
        onConnected: () -> Unit,
        onError: (exception: BLEException) -> Unit
    ) {
        connect(device.bluetoothDevice)
            .timeout(100000)
            .retry(3, 100)
            .done { onConnected() }
            .fail { device, status -> onError.invoke(BLEConnectionException(status)) }
            .enqueue()
        //Might not be necessary
        //context.registerReceiver(bluetoothBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun registerConnectionListener(listener: RoboticsConnectionStatusListener) {
        listener.onConnectionStateChanged(isConnected)
        callbacks.registerConnectionListener(listener)
    }

    fun unregisterConnectionListener(listener: RoboticsConnectionStatusListener) {
        callbacks.unregisterConnectionListener(listener)
    }

    private inner class GattCallback: BleManagerGattCallback() {

        override fun onDeviceDisconnected() {
            roboticEventSerializer.clear()
            services.forEach {
                it.disconnect()
            }
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            gattConnection = gatt
            services.forEach {
                it.init(gatt, roboticEventSerializer)
            }
            return true
        }

        override fun initialize() {
            super.initialize()
            gattConnection?.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
        }

    }

    public override fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic?,
        data: ByteArray?
    ): WriteRequest {
        return super.writeCharacteristic(characteristic, data)
    }

    public override fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic?,
        data: Data?
    ): WriteRequest {
        return super.writeCharacteristic(characteristic, data)
    }

    public override fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic?,
        data: ByteArray?,
        offset: Int,
        length: Int
    ): WriteRequest {
        return super.writeCharacteristic(characteristic, data, offset, length)
    }

    public override fun readCharacteristic(characteristic: BluetoothGattCharacteristic?): ReadRequest {
        return super.readCharacteristic(characteristic)
    }

}