package org.revolutionrobotics.bluetooth.android.communication

interface RoboticsConnectionStatusListener {
    fun onConnectionStateChanged(connected: Boolean)
}
