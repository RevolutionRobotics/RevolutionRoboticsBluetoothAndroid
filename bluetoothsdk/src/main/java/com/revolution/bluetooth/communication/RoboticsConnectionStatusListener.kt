package com.revolution.bluetooth.communication

interface RoboticsConnectionStatusListener {
    fun onConnectionStateChanged(connected: Boolean, serviceDiscovered: Boolean)
}
