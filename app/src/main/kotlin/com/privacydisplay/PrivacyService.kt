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
import android.util.DisplayMetrics
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

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var intensity = 0.85f
    private var gradientWidth = 0.40f
    private var faceDetectionEnabled = true

    // Whether overlay is manually toggled on (independent of face detection)
    private var manuallyEnabled = true

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_TOGGLE) {
            toggleOverlay()
            return START_STICKY
        }

        // Read config from intent extras
        intensity = intent?.getFloatExtra("intensity", 0.85f) ?: 0.85f
        gradientWidth = intent?.getFloatExtra("width", 0.40f) ?: 0.40f
        faceDetectionEnabled = intent?.getBooleanExtra("faceDetect", true) ?: true

        startForeground(NOTIF_ID, buildNotification())
        addOverlay()
        if (faceDetectionEnabled) startFaceDetection()

        return START_STICKY
    }

    // ─── Overlay Management ──────────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun addOverlay() {
        if (overlayView != null) return

        val view = PrivacyOverlayView(this).apply {
            intensity = this@PrivacyService.intensity
            gradientWidth = this@PrivacyService.gradientWidth
        }

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_FOCUSABLE | NOT_TOUCHABLE so touches pass through
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(view, params)
        overlayView = view
        overlayVisible = true
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        overlayVisible = false
    }

    fun toggleOverlay() {
        manuallyEnabled = !overlayVisible
        if (overlayVisible) {
            removeOverlay()
        } else {
            addOverlay()
        }
        updateNotification()
    }

    fun setIntensity(value: Float) {
        intensity = value
        overlayView?.intensity = value
    }

    fun setGradientWidth(value: Float) {
        gradientWidth = value
        overlayView?.gradientWidth = value
    }

    fun setFaceDetectionEnabled(enabled: Boolean) {
        faceDetectionEnabled = enabled
        if (enabled) startFaceDetection() else stopFaceDetection()
    }

    // ─── Face Detection ──────────────────────────────────────────────────────

    private fun startFaceDetection() {
        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                bindFaceDetection(provider)
            } catch (e: Exception) {
                // Camera unavailable (e.g. no front camera)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindFaceDetection(provider: ProcessCameraProvider) {
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            cameraExecutor!!,
            FaceDetectionAnalyzer { faceCount -> onFaceCountChanged(faceCount) }
        )

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                imageAnalysis
            )
        } catch (e: Exception) {
            // Front camera not available
        }
    }

    private fun onFaceCountChanged(faceCount: Int) {
        val shouldActivate = faceCount > 1
        extraFaceDetected = shouldActivate

        ContextCompat.getMainExecutor(this).execute {
            if (shouldActivate && !overlayVisible) {
                addOverlay()
                Toast.makeText(this, "⚠ Extra face detected — privacy filter activated", Toast.LENGTH_SHORT).show()
            } else if (!shouldActivate && overlayVisible && !manuallyEnabled) {
                // Only auto-remove if not manually enabled
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

    // ─── Notification ────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Privacy Display",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Privacy filter status"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val toggleIntent = Intent(this, PrivacyService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val togglePi = PendingIntent.getService(
            this, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val filterLabel = if (overlayVisible) "Filter: ON" else "Filter: OFF"
        val faceLabel = if (extraFaceDetected) " | ⚠ Extra face!" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Privacy Display Active")
            .setContentText("$filterLabel$faceLabel  •  Tap to open")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openPi)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (overlayVisible) "Turn OFF" else "Turn ON",
                togglePi
            )
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        stopFaceDetection()
        removeOverlay()
        instance = null
        isRunning = false
        extraFaceDetected = false
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
