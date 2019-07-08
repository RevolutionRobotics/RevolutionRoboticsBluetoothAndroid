package com.revolution.robotics.core.utils.dynamicPermissions

import android.app.Activity
import android.content.pm.PackageManager
import android.util.SparseArray
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.core.util.set

class DynamicPermissionHandler {

    private var requestCode = 0
    private var requests = SparseArray<PermissionRequest>()

    fun hasPermissions(permissions: List<String>, activity: Activity): Boolean {
        val permissionList = permissions.toMutableList()
        permissionList.removeAll {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
        return permissionList.isEmpty()
    }

    fun permissions(permissions: List<String>) =
        PermissionRequest(this, permissions.toMutableList())

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        activity: Activity
    ) {
        requests[requestCode]?.let { currentRequest ->
            val permissionsDenied = mutableListOf<String>()
            permissions.forEachIndexed { index, permission ->
                if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                    permissionsDenied.add(permission)
                }
            }

            if (permissionsDenied.isEmpty()) {
                currentRequest.listener?.onAllPermissionsGranted()
            } else {
                currentRequest.listener?.onPermissionDenied(
                    permissionsDenied.toList(),
                    permissions.any { !shouldShowRequestPermissionRationale(activity, it) })
            }
        }
    }

    class PermissionRequest(
        var handler: DynamicPermissionHandler?,
        private val permissions: MutableList<String>
    ) {

        var listener: DynamicPermissionListener? = null

        fun listener(listener: DynamicPermissionListener): PermissionRequest {
            this.listener = listener
            return this
        }

        fun request(activity: Activity) {
            val currentRequestCode = handler?.requestCode ?: 0
            handler?.let { handler ->
                handler.requests[currentRequestCode] = this
                handler.requestCode++
            }

            listener?.let { listener ->
                permissions.removeAll {
                    ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
                }

                if (permissions.isEmpty()) {
                    listener.onAllPermissionsGranted()
                } else {
                    ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), currentRequestCode)
                }
            }
        }
    }
}
