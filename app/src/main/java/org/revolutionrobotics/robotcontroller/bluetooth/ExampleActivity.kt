package org.revolutionrobotics.robotcontroller.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.revolution.robotics.core.utils.dynamicPermissions.DynamicPermissionHandler
import com.revolution.robotics.core.utils.dynamicPermissions.DynamicPermissionListener
import kotlinx.android.synthetic.main.acrtivity_example.*
import org.revolutionrobotics.bluetooth.android.communication.RoboticsConnectionStatusListener
import org.revolutionrobotics.bluetooth.android.communication.RoboticsDeviceConnector
import org.revolutionrobotics.bluetooth.android.discover.RoboticsDeviceDiscoverer
import org.revolutionrobotics.bluetooth.android.domain.Device
import org.revolutionrobotics.bluetooth.android.service.RoboticsMotorService
import org.revolutionrobotics.robotcontroller.bluetooth.connect.ConnectDialog
import java.io.File
import java.nio.charset.Charset

class ExampleActivity : AppCompatActivity(), DynamicPermissionListener,
    RoboticsConnectionStatusListener {

    val deviceConnector = RoboticsDeviceConnector()

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
        deviceConnector.registerConnectionListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceConnector.unregisterConnectionListener(this)
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
            ConnectDialog().show(supportFragmentManager, "connect")
        }
        btn_disconnect.setOnClickListener {
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
        btn_1.setOnTouchListener(ButtonPressListener { onButtonChanged(0, it)})
        btn_2.setOnTouchListener(ButtonPressListener { onButtonChanged(1, it)})
        btn_3.setOnTouchListener(ButtonPressListener { onButtonChanged(2, it)})
        btn_4.setOnTouchListener(ButtonPressListener { onButtonChanged(3, it)})
        btn_5.setOnTouchListener(ButtonPressListener { onButtonChanged(4, it)})
        btn_6.setOnTouchListener(ButtonPressListener { onButtonChanged(5, it)})
    }

    private fun onButtonChanged(buttonIndex: Int, pressed: Boolean) {
        if (isConnected) {
            if (pressed){
                deviceConnector.liveControllerService.onButtonPressed(buttonIndex)
            } else {
                deviceConnector.liveControllerService.onButtonReleased(buttonIndex)
            }
        }
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

    override fun onConnectionStateChanged(connected: Boolean, serviceDiscovered: Boolean) {

        isConnected = connected && serviceDiscovered
        if (isConnected) {
            connection_status_text.text = "Connected"
            btn_connect.visibility = View.GONE
            btn_disconnect.visibility = View.VISIBLE
        } else {
            connection_status_text.text = "Not connected"
            btn_connect.visibility = View.VISIBLE
            btn_disconnect.visibility = View.GONE
        }
    }
}