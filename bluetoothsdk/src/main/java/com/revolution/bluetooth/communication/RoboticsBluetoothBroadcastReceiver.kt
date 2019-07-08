package com.revolution.bluetooth.communication

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RoboticsBluetoothBroadcastReceiver(private val deviceConnector: RoboticsDeviceConnector) : BroadcastReceiver() {

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action?.equals(BluetoothAdapter.ACTION_STATE_CHANGED) == true &&
            bluetoothAdapter.state == BluetoothAdapter.STATE_TURNING_OFF
        ) {
            deviceConnector.disconnect()
        }
    }
}
