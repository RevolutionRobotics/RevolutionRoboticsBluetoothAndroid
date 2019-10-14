package org.revolutionrobotics.bluetooth.android.domain

import android.bluetooth.BluetoothDevice

data class Device(
    val name: String,
    val address: String,
    val bluetoothDevice: BluetoothDevice
)
