package org.revolutionrobotics.robotcontroller.bluetooth.connect

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.revolutionrobotics.bluetooth.android.discover.RoboticsDeviceDiscoverer
import org.revolutionrobotics.bluetooth.android.domain.Device
import org.revolutionrobotics.robotcontroller.bluetooth.ExampleActivity
import org.revolutionrobotics.robotcontroller.bluetooth.R


class ConnectDialog : DialogFragment() {

    private val deviceDiscoverer = RoboticsDeviceDiscoverer()
    private var connecting = false

    private lateinit var robotList: RecyclerView
    private val adapter = RobotListAdapter()

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.dialog_connect, container, false)
        robotList = v.findViewById(R.id.robot_list)
        robotList.layoutManager = LinearLayoutManager(activity)
        robotList.adapter = adapter
        adapter.onRobotSelected = { connect(it) }
        deviceDiscoverer.discoverRobots(activity!!) { devices ->
            if (devices.isNotEmpty()) {
                robotList.visibility = View.VISIBLE
                adapter.addItems(devices)
            }
        }
        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deviceDiscoverer.stopDiscovering()
    }

    fun connect(device: Device) {
        if (!connecting) {
            connecting = true
            deviceDiscoverer.stopDiscovering()
            adapter.showConnecting(device)
            getExampleActivity().deviceConnector.connect(activity!!, device, onConnected = {
                Toast.makeText(activity, "Connected", Toast.LENGTH_SHORT).show()
                dismiss()
            }, onDisconnected = {
            }, onError = {
                Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show()
            })
        }

    }

    private fun getExampleActivity() = activity as ExampleActivity
}