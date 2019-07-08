package org.revolutionrobotics.robotcontroller.bluetooth.communication

interface RoboticsConnectionStatusListener {
    fun onConnectionStateChanged(connected: Boolean, serviceDiscovered: Boolean)
}
