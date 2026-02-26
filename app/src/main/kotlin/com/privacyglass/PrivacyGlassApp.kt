package com.privacyglass

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService

class PrivacyGlassApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService<NotificationManager>()!!

        // Main overlay service channel
        NotificationChannel(
            CHANNEL_OVERLAY,
            "Privacy Glass Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while the privacy overlay is active"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }.also { nm.createNotificationChannel(it) }

        // Face detection alert channel
        NotificationChannel(
            CHANNEL_FACE_ALERT,
            "Face Detection Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when an extra face is detected"
        }.also { nm.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_OVERLAY = "privacy_glass_overlay"
        const val CHANNEL_FACE_ALERT = "privacy_glass_face_alert"
        const val NOTIF_ID_OVERLAY = 1001
    }
}
