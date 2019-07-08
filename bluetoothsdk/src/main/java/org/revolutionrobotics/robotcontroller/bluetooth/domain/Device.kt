package org.revolutionrobotics.robotcontroller.bluetooth.domain

import android.bluetooth.BluetoothDevice

data class Device(
    val name: String,
    val address: String,
    val bluetoothDevice: BluetoothDevice
)
