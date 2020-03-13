package org.revolutionrobotics.bluetooth.android.communication

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import no.nordicsemi.android.ble.*
import org.revolutionrobotics.bluetooth.android.domain.Device
import org.revolutionrobotics.bluetooth.android.exception.BLEConnectionException
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import org.revolutionrobotics.bluetooth.android.service.*

class RoboticsDeviceConnector(context: Context) : BleManager<BleManagerCallbacks>(context) {

    override fun getGattCallback(): BleManagerGattCallback {
        return GattCallback()
    }

    private var gattConnection: BluetoothGatt? = null
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
    }

    fun registerConnectionListener(listener: RoboticsConnectionStatusListener) {
        listener.onConnectionStateChanged(isConnected)
        callbacks.registerConnectionListener(listener)
    }

    fun unregisterConnectionListener(listener: RoboticsConnectionStatusListener) {
        callbacks.unregisterConnectionListener(listener)
    }

    private inner class GattCallback : BleManagerGattCallback() {

        override fun onDeviceDisconnected() {
            services.forEach {
                it.disconnect()
            }
        }

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            gattConnection = gatt
            services.forEach {
                it.init(gatt)
            }
            return true
        }

        override fun initialize() {
            super.initialize()
            requestConnectionPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH).enqueue()
            requestMtu(RoboticsConfigurationService.DEFAULT_MTU)
                .with { device, mtu -> configurationService.mtu = mtu }
                .enqueue()
        }

    }

    public override fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic?,
        data: ByteArray?
    ): WriteRequest {
        return super.writeCharacteristic(characteristic, data)
    }

    public override fun readCharacteristic(characteristic: BluetoothGattCharacteristic?): ReadRequest {
        return super.readCharacteristic(characteristic)
    }
}