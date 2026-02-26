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

    private val cameraPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> checkOverlayPermission() }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> requestCameraPermission() }

    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) startPrivacyService()
        else Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_LONG).show()
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
        binding.btnPowerToggle.setOnClickListener {
            if (PrivacyService.isRunning) stopPrivacyService()
            else requestPermissionsAndStart()
        }

        // Safe zone width: how narrow the visible center strip is
        binding.seekbarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // progress 0-100 → safeZone 0.08 (very narrow) to 0.45 (wider)
                val safeZone = 0.08f + (progress / 100f) * 0.37f
                binding.tvIntensityValue.text = "${progress}%"
                PrivacyService.instance?.setSafeZone(safeZone)
                getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt("safeZone", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Darkness intensity
        binding.seekbarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val intensity = 0.80f + (progress / 100f) * 0.19f  // 0.80 to 0.99
                binding.tvWidthValue.text = "${progress}%"
                PrivacyService.instance?.setIntensity(intensity)
                getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt("darkness", progress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.switchFaceDetection.setOnCheckedChangeListener { _, isChecked ->
            PrivacyService.instance?.setFaceDetectionEnabled(isChecked)
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("faceDetect", isChecked).apply()
        }

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        binding.seekbarIntensity.progress = prefs.getInt("safeZone", 25)
        binding.seekbarWidth.progress = prefs.getInt("darkness", 90)
        binding.switchFaceDetection.isChecked = prefs.getBoolean("faceDetect", true)
        binding.tvIntensityValue.text = "${binding.seekbarIntensity.progress}%"
        binding.tvWidthValue.text = "${binding.seekbarWidth.progress}%"
    }

    private fun updateUIState() {
        val isOn = PrivacyService.isRunning
        if (isOn) {
            binding.btnPowerToggle.text = "● PRIVACY ON"
            binding.btnPowerToggle.setBackgroundColor(ContextCompat.getColor(this, R.color.privacy_active))
            binding.tvStatus.text = "Privacy glass active — screen hidden from side angles"
        } else {
            binding.btnPowerToggle.text = "○ PRIVACY OFF"
            binding.btnPowerToggle.setBackgroundColor(ContextCompat.getColor(this, R.color.privacy_inactive))
            binding.tvStatus.text = "Privacy filter is inactive. Tap to enable."
        }
        binding.tvFaceStatus.text = if (PrivacyService.extraFaceDetected)
            "⚠ Extra face detected — filter auto-activated" else ""
    }

    private fun requestPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
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
            Toast.makeText(this, "Please grant 'Display over other apps' permission", Toast.LENGTH_LONG).show()
            overlayPermLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        } else {
            startPrivacyService()
        }
    }

    private fun startPrivacyService() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val safeZone = 0.08f + (prefs.getInt("safeZone", 25) / 100f) * 0.37f
        val intensity = 0.80f + (prefs.getInt("darkness", 90) / 100f) * 0.19f
        val intent = Intent(this, PrivacyService::class.java).apply {
            putExtra("safeZone", safeZone)
            putExtra("intensity", intensity)
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
