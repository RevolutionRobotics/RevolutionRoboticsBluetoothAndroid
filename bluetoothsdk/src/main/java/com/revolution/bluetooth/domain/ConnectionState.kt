package com.revolution.bluetooth.domain

import android.bluetooth.BluetoothProfile

enum class ConnectionState(val id: Int) {
    CONNECTING(BluetoothProfile.STATE_CONNECTING),
    CONNECTED(BluetoothProfile.STATE_CONNECTED),
    DISCONNECTING(BluetoothProfile.STATE_DISCONNECTING),
    DISCONNECTED(BluetoothProfile.STATE_DISCONNECTED);

    companion object {
        fun parseConnectionId(id: Int): ConnectionState =
            values().find { it.id == id } ?: throw IllegalArgumentException("Invalid id $id")
    }
}
