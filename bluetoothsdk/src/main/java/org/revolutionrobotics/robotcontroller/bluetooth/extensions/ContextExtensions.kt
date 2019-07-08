package org.revolutionrobotics.robotcontroller.bluetooth.extensions

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import org.revolutionrobotics.robotcontroller.bluetooth.exception.BLEDisabledException
import org.revolutionrobotics.robotcontroller.bluetooth.exception.MissingBLEFeatureException

fun Context.getBLEManager(): BluetoothManager {
    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        throw MissingBLEFeatureException()
    }

    val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    if (!bluetoothManager.adapter.isEnabled) {
        throw BLEDisabledException()
    }
    return bluetoothManager
}
