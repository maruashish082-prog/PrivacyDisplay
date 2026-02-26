package com.privacydisplay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PrivacyService : LifecycleService() {

    companion object {
        var isRunning = false
        var extraFaceDetected = false
        var instance: PrivacyService? = null
        const val CHANNEL_ID = "privacy_display_channel"
        const val NOTIF_ID = 1001
        const val ACTION_TOGGLE = "com.privacydisplay.TOGGLE"
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: PrivacyOverlayView? = null
    private var overlayVisible = false
    private var manuallyEnabled = true

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var safeZone = 0.20f
    private var intensity = 0.97f
    private var faceDetectionEnabled = true

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_TOGGLE) { toggleOverlay(); return START_STICKY }

        safeZone = intent?.getFloatExtra("safeZone", 0.20f) ?: 0.20f
        intensity = intent?.getFloatExtra("intensity", 0.97f) ?: 0.97f
        faceDetectionEnabled = intent?.getBooleanExtra("faceDetect", true) ?: true

        startForeground(NOTIF_ID, buildNotification())
        addOverlay()
        if (faceDetectionEnabled) startFaceDetection()
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addOverlay() {
        if (overlayView != null) return
        val view = PrivacyOverlayView(this).apply {
            this.safeZoneWidth = this@PrivacyService.safeZone
            this.intensity = this@PrivacyService.intensity
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(view, params)
        overlayView = view
        overlayVisible = true
    }

    private fun removeOverlay() {
        overlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        overlayView = null
        overlayVisible = false
    }

    fun toggleOverlay() {
        manuallyEnabled = !overlayVisible
        if (overlayVisible) removeOverlay() else addOverlay()
        updateNotification()
    }

    fun setSafeZone(value: Float) {
        safeZone = value
        overlayView?.safeZoneWidth = value
    }

    fun setIntensity(value: Float) {
        intensity = value
        overlayView?.intensity = value
    }

    fun setFaceDetectionEnabled(enabled: Boolean) {
        faceDetectionEnabled = enabled
        if (enabled) startFaceDetection() else stopFaceDetection()
    }

    private fun startFaceDetection() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                cameraProvider = future.get()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(cameraExecutor!!, FaceDetectionAnalyzer { count ->
                    onFaceCountChanged(count)
                })
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(this))
    }

    private fun onFaceCountChanged(faceCount: Int) {
        val shouldActivate = faceCount > 1
        extraFaceDetected = shouldActivate
        ContextCompat.getMainExecutor(this).execute {
            if (shouldActivate && !overlayVisible) {
                addOverlay()
                Toast.makeText(this, "⚠ Extra face detected — privacy activated", Toast.LENGTH_SHORT).show()
            } else if (!shouldActivate && overlayVisible && !manuallyEnabled) {
                removeOverlay()
            }
            updateNotification()
        }
    }

    private fun stopFaceDetection() {
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        extraFaceDetected = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Privacy Display", NotificationManager.IMPORTANCE_LOW)
            .apply { setShowBadge(false) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val togglePi = PendingIntent.getService(this, 0,
            Intent(this, PrivacyService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val openPi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Privacy Display Active")
            .setContentText(if (overlayVisible) "Privacy glass ON" else "Privacy glass OFF")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openPi)
            .addAction(0, if (overlayVisible) "Turn OFF" else "Turn ON", togglePi)
            .setOngoing(true).setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFaceDetection()
        removeOverlay()
        instance = null
        isRunning = false
        extraFaceDetected = false
    }

    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }
}
