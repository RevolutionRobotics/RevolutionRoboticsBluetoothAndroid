package org.revolutionrobotics.bluetooth.android.discover

import android.bluetooth.le.ScanResult

interface ScanResultListener {

    fun onScanResult(scanResult: List<ScanResult>)
}
