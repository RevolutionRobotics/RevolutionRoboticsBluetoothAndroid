package com.revolution.bluetooth.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RoboticsEventSerializer {

    companion object {
        private const val DEFAULT_DELAY = 100.toLong()
    }

    private val events = mutableListOf<() -> Boolean>()
    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)

    fun registerEvent(f: () -> Boolean) {
        synchronized(RoboticsEventSerializer::class.java) {
            events.add(f)
            if (events.size == 1) {
                startSerializationThread()
            }
        }
    }

    fun clear() {
        synchronized(RoboticsEventSerializer::class.java) {
            events.clear()
        }
    }

    private fun startSerializationThread() {
        ioScope.launch {
            delay(DEFAULT_DELAY)
            while (events.isNotEmpty()) {
                synchronized(RoboticsEventSerializer::class.java) {
                    if (events.first().invoke()) {
                        events.removeAt(0)
                    }
                }
                delay(DEFAULT_DELAY)
            }
        }
    }
}
