package com.revolution.bluetooth.discover

import android.bluetooth.le.ScanResult

interface ScanResultListener {

    fun onScanResult(scanResult: List<ScanResult>)
}
