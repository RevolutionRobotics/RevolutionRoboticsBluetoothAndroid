package org.revolutionrobotics.robotcontroller.bluetooth

import android.view.MotionEvent
import android.view.View

class ButtonPressListener(
    private val onChanged: (Boolean)->Unit
) : View.OnTouchListener {


    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                onChanged(true)
                true
            }
            MotionEvent.ACTION_UP -> {
                onChanged(false)
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                onChanged(false)
                true
            }
            else -> false
        }
    }
}