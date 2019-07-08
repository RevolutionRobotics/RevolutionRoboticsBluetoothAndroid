package com.revolution.bluetooth.extensions

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import com.revolution.bluetooth.exception.BLEDisabledException
import com.revolution.bluetooth.exception.MissingBLEFeatureException

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
