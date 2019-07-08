package com.revolution.bluetooth.threading

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun moveToUIThread(f: () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        withContext(Dispatchers.Main) {
            f.invoke()
        }
    }
}
