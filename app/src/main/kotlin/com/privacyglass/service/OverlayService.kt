package com.privacyglass.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.privacyglass.MainActivity
import com.privacyglass.PrivacyGlassApp
import com.privacyglass.R
import com.privacyglass.data.OverlayConfig
import com.privacyglass.data.PreferencesManager
import com.privacyglass.overlay.FaceDetectionAnalyzer
import com.privacyglass.overlay.PrivacyOverlayView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OverlayService : LifecycleService() {

    companion object {
        var isRunning = false
            private set
        var extraFaceDetected = false
            private set

        const val ACTION_STOP   = "com.privacyglass.STOP"
        const val ACTION_TOGGLE = "com.privacyglass.TOGGLE"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager

    private var overlayView: PrivacyOverlayView? = null
    private var overlayAdded = false
    private var manuallyEnabled = true

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var configJob: Job? = null

    private var currentConfig = OverlayConfig()

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager(applicationContext)

        startForeground(PrivacyGlassApp.NOTIF_ID_OVERLAY, buildNotification())
        observeConfig()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP   -> stopSelf()
            ACTION_TOGGLE -> toggleOverlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        configJob?.cancel()
        stopCamera()
        removeOverlay()
        isRunning = false
        extraFaceDetected = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }

    // ── Config observer ───────────────────────────────────────────────────────

    private fun observeConfig() {
        configJob = lifecycleScope.launch {
            prefs.configFlow
                .distinctUntilChanged()
                .collect { config ->
                    currentConfig = config
                    overlayView?.applyConfig(config)
                    if (config.faceDetectionEnabled) startCamera() else stopCamera()
                    updateNotification()
                }
        }
    }

    // ── Overlay management ────────────────────────────────────────────────────

    private fun addOverlay() {
        if (overlayAdded) return

        val view = PrivacyOverlayView(this).also { overlayView = it }
        view.applyConfig(currentConfig)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(view, params)
            overlayAdded = true
        } catch (e: Exception) {
            // Window token invalid (race condition) — ignore
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        overlayAdded = false
    }

    fun toggleOverlay() {
        manuallyEnabled = !overlayAdded
        if (overlayAdded) removeOverlay() else addOverlay()
        updateNotification()
    }

    // ── Camera / Face detection ───────────────────────────────────────────────

    private fun startCamera() {
        if (cameraExecutor != null) return // already running
        cameraExecutor = Executors.newSingleThreadExecutor()

        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({
                runCatching {
                    cameraProvider = future.get()
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(cameraExecutor!!, FaceDetectionAnalyzer { count ->
                                onFaceCountChanged(count)
                            })
                        }
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        cameraProvider = null
        extraFaceDetected = false
    }

    private fun onFaceCountChanged(count: Int) {
        val shouldActivate = count > 1
        extraFaceDetected = shouldActivate
        ContextCompat.getMainExecutor(this).execute {
            if (shouldActivate && !overlayAdded) {
                addOverlay()
            } else if (!shouldActivate && overlayAdded && !manuallyEnabled) {
                removeOverlay()
            }
            updateNotification()
        }
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification() = run {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleIntent = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = buildString {
            append(if (overlayAdded) "Filter ON" else "Filter OFF")
            if (extraFaceDetected) append(" • ⚠ Extra face")
        }

        NotificationCompat.Builder(this, PrivacyGlassApp.CHANNEL_OVERLAY)
            .setContentTitle("Privacy Glass")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_privacy)
            .setContentIntent(openIntent)
            .addAction(0, if (overlayAdded) "Turn OFF" else "Turn ON", toggleIntent)
            .addAction(0, "Stop Service", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(PrivacyGlassApp.NOTIF_ID_OVERLAY, buildNotification())
    }
}
