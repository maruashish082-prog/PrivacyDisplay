package com.privacyglass.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Centralised permission checks and intent builders.
 * Keep all permission logic here — never scatter it across the UI layer.
 */
object PermissionManager {

    /** Returns true if the app can draw over other apps */
    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /** Returns true if camera permission is granted */
    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    /** Returns true if POST_NOTIFICATIONS is granted (Android 13+) */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required below Android 13
        }
    }

    /** Returns all currently missing permissions */
    fun getMissingPermissions(context: Context): List<PermissionType> {
        val missing = mutableListOf<PermissionType>()
        if (!hasOverlayPermission(context))      missing += PermissionType.OVERLAY
        if (!hasNotificationPermission(context)) missing += PermissionType.NOTIFICATION
        if (!hasCameraPermission(context))       missing += PermissionType.CAMERA
        return missing
    }

    /** Returns true if all required permissions (overlay + notification) are granted */
    fun hasAllRequiredPermissions(context: Context): Boolean =
        hasOverlayPermission(context) && hasNotificationPermission(context)

    /** Intent to open overlay permission settings for this app */
    fun overlayPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    /** Intent to open app-level settings */
    fun appSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
}

enum class PermissionType(val title: String, val description: String) {
    OVERLAY(
        "Display Over Other Apps",
        "Required to show the privacy filter on top of all apps"
    ),
    NOTIFICATION(
        "Notifications",
        "Required to show a persistent control in your notification shade"
    ),
    CAMERA(
        "Camera",
        "Optional — enables automatic activation when another face is detected"
    )
}
