package com.revolution.robotics.core.utils.dynamicPermissions

interface DynamicPermissionListener {
    fun onAllPermissionsGranted()
    fun onPermissionDenied(deniedPermissions: List<String>, showErrorMessage: Boolean)
}
