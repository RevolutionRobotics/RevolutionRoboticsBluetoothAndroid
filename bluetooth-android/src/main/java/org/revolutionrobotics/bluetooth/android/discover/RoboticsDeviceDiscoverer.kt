package org.revolutionrobotics.bluetooth.android.discover

import android.Manifest
import android.content.Context
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import no.nordicsemi.android.support.v18.scanner.*
import org.revolutionrobotics.bluetooth.android.domain.Device
import org.revolutionrobotics.bluetooth.android.exception.BLEScanFailedException
import java.util.*

class RoboticsDeviceDiscoverer {

    companion object {
        val SERVICE_ID_LIVE: UUID = UUID.fromString("d2d5558c-5b9d-11e9-8647-d663bd873d93")
        val SERVICE_ID_DEVICE_INFO: UUID = UUID.fromString("F000180A-0451-4000-B000-000000000000")
    }

    private var scanResultListener: ((List<Device>) -> Unit)? = null
    private var scanner = BluetoothLeScannerCompat.getScanner()
    private var callback = object: ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            scanner.stopScan(this)
            stopDiscovering()
            throw BLEScanFailedException(errorCode)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            scanResultListener?.invoke(results.map {
                Device(it.device.name, it.device.address, it.device)
            })
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            scanResultListener?.invoke(
                listOf(
                    Device(
                        result.device.name,
                        result.device.address,
                        result.device
                    )
                )
            )
        }
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH]
    )
    fun discoverRobots(context: Context, listener: (List<Device>) -> Unit) {
        scanResultListener = listener
        scanner.startScan(
            listOf(
                ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_ID_LIVE)).build(),
                ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_ID_DEVICE_INFO)).build()
            ), ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(true)
                .build(), callback
        )
    }

    fun stopDiscovering() {
        scanner.stopScan(callback)
        scanResultListener = null
    }
}
