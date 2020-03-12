package org.revolutionrobotics.bluetooth.android.communication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.IntentFilter
import org.revolutionrobotics.bluetooth.android.domain.ConnectionState
import org.revolutionrobotics.bluetooth.android.domain.Device
import org.revolutionrobotics.bluetooth.android.exception.BLEConnectionException
import org.revolutionrobotics.bluetooth.android.exception.BLEException
import org.revolutionrobotics.bluetooth.android.service.*
import org.revolutionrobotics.bluetooth.android.threading.moveToUIThread

@Suppress("TooManyFunctions")
class RoboticsDeviceConnector : BluetoothGattCallback() {

    companion object {
        // TODO Modify to 512 when the robot will support bigger mtu than 256
        const val REQUESTED_MTU = 256
    }

    private var device: BluetoothDevice? = null
    private var gattConnection: BluetoothGatt? = null

    private var onConnected: (() -> Unit)? = null
    private var onDisconnected: (() -> Unit)? = null
    private var onError: ((exception: BLEException) -> Unit)? = null

    private var connectionListeners = mutableSetOf<RoboticsConnectionStatusListener>()
    private var isConnected = false
    private var isServiceDiscovered = false
    private var context: Context? = null

    private val roboticEventSerializer = RoboticsEventSerializer()
    private val bluetoothBroadcastReceiver = null

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

    private val services = emptySet<RoboticsBLEService>()

    fun connect(
        context: Context,
        device: Device,
        onConnected: () -> Unit,
        onDisconnected: () -> Unit,
        onError: (exception: BLEException) -> Unit
    ) {
        disconnect()
        this.device = device.bluetoothDevice
        this.onConnected = onConnected
        this.onDisconnected = onDisconnected
        this.onError = onError
        this.device?.connectGatt(context, true, this)
        this.context = context
        context.registerReceiver(bluetoothBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun registerConnectionListener(listener: RoboticsConnectionStatusListener) {
        listener.onConnectionStateChanged(isConnected)
        connectionListeners.add(listener)
    }

    fun unregisterConnectionListener(listener: RoboticsConnectionStatusListener) {
        connectionListeners.remove(listener)
    }

    fun disconnect() {
        isConnected = false
        isServiceDiscovered = false
        roboticEventSerializer.clear()
        connectionListeners.forEach {
            it.onConnectionStateChanged(connected = false)
        }
        services.forEach {
            it.disconnect()
        }
        gattConnection?.disconnect()
        gattConnection?.close()
        gattConnection = null
        device = null
        context?.unregisterReceiver(bluetoothBroadcastReceiver)
        this.context = null
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        moveToUIThread {
            when (ConnectionState.parseConnectionId(newState)) {
                ConnectionState.CONNECTED -> {
                    isConnected = true
                    connectionListeners.forEach {
                        it.onConnectionStateChanged(true)
                    }
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        gatt?.requestMtu(REQUESTED_MTU)
                    } else {
                        onError?.invoke(BLEConnectionException(status))
                        onError = null
                    }
                }
                ConnectionState.DISCONNECTED -> {
                    onDisconnected?.invoke()
                    isConnected = false
                    isServiceDiscovered = false
                    connectionListeners.forEach {
                        it.onConnectionStateChanged(false)
                    }
                }
                ConnectionState.DISCONNECTING, ConnectionState.CONNECTING -> Unit
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        gattConnection = gatt
        services.forEach {
            it.init(gatt, roboticEventSerializer)
        }
        isServiceDiscovered = true
        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
        moveToUIThread {
            onConnected?.invoke()
            onConnected = null
            connectionListeners.forEach {
                it.onConnectionStateChanged(isConnected)
            }
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
        moveToUIThread {
            //services.forEach { it.onCharacteristicChanged(gatt, characteristic) }
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic, status: Int) {
        moveToUIThread {
            //services.forEach { it.onCharacteristicRead(gatt, characteristic, status) }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        moveToUIThread {
            //services.forEach { it.onCharacteristicWrite(gatt, characteristic, status) }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        configurationService.mtu = mtu
        gatt?.discoverServices()
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) = Unit
}
