package com.privacydisplay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.privacydisplay.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Camera permission launcher
    private val cameraPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkOverlayPermission()
        } else {
            Toast.makeText(this,
                "Camera permission needed for auto face-detection",
                Toast.LENGTH_LONG).show()
            // Still allow manual-only mode
            checkOverlayPermission()
        }
    }

    // Notification permission launcher (Android 13+)
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> requestCameraPermission() }

    // Overlay permission result
    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Settings.canDrawOverlays(this)) {
            startPrivacyService()
        } else {
            Toast.makeText(this,
                "Overlay permission is required to display the privacy filter over other apps",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updateUIState()
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
    }

    private fun setupUI() {
        // Main power toggle button
        binding.btnPowerToggle.setOnClickListener {
            if (PrivacyService.isRunning) {
                stopPrivacyService()
            } else {
                requestPermissionsAndStart()
            }
        }

        // Intensity slider
        binding.seekbarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val intensity = progress / 100f
                binding.tvIntensityValue.text = "${progress}%"
                PrivacyService.instance?.setIntensity(intensity)
                // Save preference
                getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit().putInt("intensity", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Gradient width slider
        binding.seekbarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 10..60 -> mapped from 0..100 slider
                val widthPct = (progress / 100f) * 0.5f + 0.1f // 0.1 to 0.6
                binding.tvWidthValue.text = "${progress}%"
                PrivacyService.instance?.setGradientWidth(widthPct)
                getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit().putInt("width", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Face detection toggle
        binding.switchFaceDetection.setOnCheckedChangeListener { _, isChecked ->
            PrivacyService.instance?.setFaceDetectionEnabled(isChecked)
            getSharedPreferences("prefs", MODE_PRIVATE)
                .edit().putBoolean("faceDetect", isChecked).apply()
        }

        // Load saved preferences
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        binding.seekbarIntensity.progress = prefs.getInt("intensity", 85)
        binding.seekbarWidth.progress = prefs.getInt("width", 60)
        binding.switchFaceDetection.isChecked = prefs.getBoolean("faceDetect", true)
        binding.tvIntensityValue.text = "${binding.seekbarIntensity.progress}%"
        binding.tvWidthValue.text = "${binding.seekbarWidth.progress}%"
    }

    private fun updateUIState() {
        val isOn = PrivacyService.isRunning
        if (isOn) {
            binding.btnPowerToggle.text = "● PRIVACY ON"
            binding.btnPowerToggle.setBackgroundColor(
                ContextCompat.getColor(this, R.color.privacy_active))
            binding.tvStatus.text = "Privacy filter is ACTIVE across all apps"
        } else {
            binding.btnPowerToggle.text = "○ PRIVACY OFF"
            binding.btnPowerToggle.setBackgroundColor(
                ContextCompat.getColor(this, R.color.privacy_inactive))
            binding.tvStatus.text = "Privacy filter is inactive. Tap to enable."
        }
        binding.tvFaceStatus.text = if (PrivacyService.extraFaceDetected)
            "⚠ Extra face detected — filter auto-activated" else ""
    }

    private fun requestPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this,
                "Please grant 'Display over other apps' permission",
                Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermLauncher.launch(intent)
        } else {
            startPrivacyService()
        }
    }

    private fun startPrivacyService() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val intent = Intent(this, PrivacyService::class.java).apply {
            putExtra("intensity", prefs.getInt("intensity", 85) / 100f)
            putExtra("width", (prefs.getInt("width", 60) / 100f) * 0.5f + 0.1f)
            putExtra("faceDetect", prefs.getBoolean("faceDetect", true))
        }
        ContextCompat.startForegroundService(this, intent)
        updateUIState()
    }

    private fun stopPrivacyService() {
        stopService(Intent(this, PrivacyService::class.java))
        updateUIState()
    }
}
