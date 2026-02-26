package com.privacyglass.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.privacyglass.data.OverlayConfig
import com.privacyglass.data.PreferencesManager
import com.privacyglass.service.OverlayService
import com.privacyglass.util.PermissionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val config: OverlayConfig = OverlayConfig(),
    val serviceRunning: Boolean = false,
    val extraFaceDetected: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val disclosureShown: Boolean = false,
    val showDisclosureDialog: Boolean = false,
)

class PrivacyViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesManager(app)
    private val context = app.applicationContext

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        // Observe preferences
        viewModelScope.launch {
            prefs.configFlow.collect { config ->
                _uiState.update { it.copy(config = config) }
            }
        }
        viewModelScope.launch {
            prefs.disclosureShownFlow.collect { shown ->
                _uiState.update { it.copy(disclosureShown = shown) }
            }
        }
        refreshPermissionState()
    }

    // ── Permission ────────────────────────────────────────────────────────────

    fun refreshPermissionState() {
        _uiState.update {
            it.copy(
                hasOverlayPermission      = PermissionManager.hasOverlayPermission(context),
                hasCameraPermission       = PermissionManager.hasCameraPermission(context),
                hasNotificationPermission = PermissionManager.hasNotificationPermission(context),
                serviceRunning            = OverlayService.isRunning,
                extraFaceDetected         = OverlayService.extraFaceDetected,
            )
        }
    }

    // ── Service control ───────────────────────────────────────────────────────

    fun startService() {
        if (!PermissionManager.hasAllRequiredPermissions(context)) return
        ContextCompat.startForegroundService(
            context, Intent(context, OverlayService::class.java)
        )
        _uiState.update { it.copy(serviceRunning = true) }
    }

    fun stopService() {
        context.stopService(Intent(context, OverlayService::class.java))
        _uiState.update { it.copy(serviceRunning = false) }
    }

    fun toggleService() {
        if (_uiState.value.serviceRunning) stopService() else startService()
    }

    fun toggleOverlay() {
        if (!OverlayService.isRunning) { startService(); return }
        ContextCompat.startForegroundService(
            context,
            Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_TOGGLE
            }
        )
    }

    // ── Disclosure dialog ─────────────────────────────────────────────────────

    fun requestToggle() {
        if (!_uiState.value.disclosureShown) {
            _uiState.update { it.copy(showDisclosureDialog = true) }
        } else {
            toggleService()
        }
    }

    fun onDisclosureAccepted() {
        viewModelScope.launch {
            prefs.setDisclosureShown(true)
            _uiState.update { it.copy(showDisclosureDialog = false) }
            toggleService()
        }
    }

    fun onDisclosureDismissed() {
        _uiState.update { it.copy(showDisclosureDialog = false) }
    }

    // ── Config updates ────────────────────────────────────────────────────────

    fun setIntensity(value: Float) = viewModelScope.launch {
        prefs.setIntensity(value)
    }

    fun setSafeZoneWidth(value: Float) = viewModelScope.launch {
        prefs.setSafeZoneWidth(value)
    }

    fun setStripesEnabled(value: Boolean) = viewModelScope.launch {
        prefs.setStripesEnabled(value)
    }

    fun setStripeDensity(value: Float) = viewModelScope.launch {
        prefs.setStripeDensity(value)
    }

    fun setVignetteEnabled(value: Boolean) = viewModelScope.launch {
        prefs.setVignetteEnabled(value)
    }

    fun setVignetteIntensity(value: Float) = viewModelScope.launch {
        prefs.setVignetteIntensity(value)
    }

    fun setFaceDetectionEnabled(value: Boolean) = viewModelScope.launch {
        prefs.setFaceDetectionEnabled(value)
    }

    fun setStartOnBoot(value: Boolean) = viewModelScope.launch {
        prefs.setStartOnBoot(value)
    }
}
