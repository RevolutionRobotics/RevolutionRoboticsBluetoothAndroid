package org.revolutionrobotics.robotcontroller.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.net.toUri
import com.revolution.robotics.core.utils.dynamicPermissions.DynamicPermissionHandler
import com.revolution.robotics.core.utils.dynamicPermissions.DynamicPermissionListener
import kotlinx.android.synthetic.main.acrtivity_example.*
import org.revolutionrobotics.robotcontroller.bluetooth.communication.RoboticsDeviceConnector
import org.revolutionrobotics.robotcontroller.bluetooth.discover.RoboticsDeviceDiscoverer
import org.revolutionrobotics.robotcontroller.bluetooth.service.RoboticsMotorService
import java.io.File
import java.nio.charset.Charset

class ExampleActivity : Activity(), DynamicPermissionListener {

    private val deviceDiscoverer = RoboticsDeviceDiscoverer()
    private val deviceConnector = RoboticsDeviceConnector()
    private val permissionRequest = DynamicPermissionHandler.PermissionRequest(
        DynamicPermissionHandler(),
        mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    )

    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acrtivity_example)
        permissionRequest.listener(this)
        permissionRequest.request(this)
        isConnected = false
    }

    override fun onAllPermissionsGranted() {
        initUI()
    }

    private fun initUI() {
        setupConnectionButtons()
        setupSeekbars()
        setupControllerButtons()
        setupBottomButtons()
    }

    @SuppressLint("MissingPermission")
    private fun setupConnectionButtons() {
        btn_connect.setOnClickListener {
            deviceDiscoverer.discoverRobots(this) { devices ->
                if (devices.isNotEmpty()) {
                    Toast.makeText(this, "Connecting", Toast.LENGTH_LONG).show()
                    deviceConnector.connect(this, devices.first(), onConnected = {
                        isConnected = true
                        Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show()
                    }, onDisconnected = {
                        isConnected = false
                        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show()
                    }, onError = {
                        isConnected = false
                        Toast.makeText(this, "Connection error: ${it.message}", Toast.LENGTH_LONG).show()
                    })
                }
            }
        }
        btn_disconnect.setOnClickListener {
            deviceDiscoverer.stopDiscovering()
            deviceConnector.disconnect()
        }

        btn_start_live_service.setOnClickListener {
            if (isConnected) {
                deviceConnector.configurationService.sendConfiguration(createConfigurationFile(), onSuccess = {
                    deviceConnector.liveControllerService.start()
                }, onError = {
                    Toast.makeText(this, "Config sending error: ${it.message}", Toast.LENGTH_LONG).show()
                })

            }
        }

        btn_stop_live_service.setOnClickListener {
            if (isConnected) {
                deviceConnector.liveControllerService.stop()
            }
        }
    }

    private fun setupSeekbars() {
        seekbar_forward.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isConnected) {
                    deviceConnector.liveControllerService.updateYDirection(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        seekbar_right.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isConnected) {
                    deviceConnector.liveControllerService.updateXDirection(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun setupControllerButtons() {
        btn_1.setOnClickListener { if (isConnected) deviceConnector.liveControllerService.onButtonPressed(0) }
        btn_2.setOnClickListener { if (isConnected) deviceConnector.liveControllerService.onButtonPressed(1) }
        btn_3.setOnClickListener { if (isConnected) deviceConnector.liveControllerService.onButtonPressed(2) }
        btn_4.setOnClickListener { if (isConnected) deviceConnector.liveControllerService.onButtonPressed(3) }
        btn_5.setOnClickListener { if (isConnected) deviceConnector.liveControllerService.onButtonPressed(4) }
        btn_6.setOnClickListener { if (isConnected) deviceConnector.liveControllerService.onButtonPressed(5) }
    }

    private fun setupBottomButtons() {
        btn_read_battery.setOnClickListener {
            if (isConnected) {
                deviceConnector.batteryService.getPrimaryBattery(onComplete = {
                    Toast.makeText(this, "Battery level $it", Toast.LENGTH_LONG).show()
                }, onError = {
                    Toast.makeText(this, "Battery reading error: ${it.message}", Toast.LENGTH_LONG).show()
                })
            }
        }

        btn_read_system_info.setOnClickListener {
            if (isConnected) {
                deviceConnector.deviceService.getSystemId(onCompleted = {
                    Toast.makeText(this, "System id $it", Toast.LENGTH_LONG).show()
                }, onError = {
                    Toast.makeText(this, "System info reading: ${it.message}", Toast.LENGTH_LONG).show()
                })
            }
        }

        btn_send_test_file.setOnClickListener {
            if (isConnected) {
                deviceConnector.configurationService.testKit(createTestFile(), onSuccess = {
                    Toast.makeText(this, "Test kit sent!", Toast.LENGTH_LONG).show()
                }, onError = {
                    Toast.makeText(this, "Test file sending: ${it.message}", Toast.LENGTH_LONG).show()
                })
            }
        }

        btn_read_motor_value.setOnClickListener {
            if (isConnected) {
                deviceConnector.motorService.read(RoboticsMotorService.Motor.M4, onComplete = {
                    Toast.makeText(this, "Motor info: ${it.joinToString()}", Toast.LENGTH_LONG).show()
                }, onError = {
                    Toast.makeText(this, "Read motor info error: ${it.message}", Toast.LENGTH_LONG).show()
                })
            }
        }
    }

    override fun onPermissionDenied(deniedPermissions: List<String>, showErrorMessage: Boolean) {
        Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionRequest.handler?.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onStop() {
        super.onStop()
        isConnected = false
        deviceDiscoverer.stopDiscovering()
        deviceConnector.disconnect()
    }

    private fun createConfigurationFile(): Uri = File("${applicationContext.filesDir}/config.json").apply {
        if (!exists()) {
            createNewFile()
        }
        writeText(assets.open("config.json").readBytes().toString(Charset.forName("UTF-8")))
    }.toUri()

    private fun createTestFile(): Uri = File("${applicationContext.filesDir}/led_test.py").apply {
        if (!exists()) {
            createNewFile()
        }
        writeText(assets.open("led_test.py").readBytes().toString(Charset.forName("UTF-8")))
    }.toUri()
}