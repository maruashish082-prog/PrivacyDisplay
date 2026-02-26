package com.privacyglass.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_glass_prefs")

class PreferencesManager(private val context: Context) {

    // ── Keys ─────────────────────────────────────────────────────────────────
    private object Keys {
        val INTENSITY          = floatPreferencesKey("intensity")
        val SAFE_ZONE_WIDTH    = floatPreferencesKey("safe_zone_width")
        val STRIPES_ENABLED    = booleanPreferencesKey("stripes_enabled")
        val STRIPE_DENSITY     = floatPreferencesKey("stripe_density")
        val VIGNETTE_ENABLED   = booleanPreferencesKey("vignette_enabled")
        val VIGNETTE_INTENSITY = floatPreferencesKey("vignette_intensity")
        val FACE_DETECTION     = booleanPreferencesKey("face_detection")
        val START_ON_BOOT      = booleanPreferencesKey("start_on_boot")
        val DISCLOSURE_SHOWN   = booleanPreferencesKey("disclosure_shown")
    }

    // ── Reactive Flow ─────────────────────────────────────────────────────────
    val configFlow: Flow<OverlayConfig> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences())
            else throw e
        }
        .map { prefs ->
            OverlayConfig(
                intensity          = prefs[Keys.INTENSITY]          ?: 0.95f,
                safeZoneWidth      = prefs[Keys.SAFE_ZONE_WIDTH]    ?: 0.18f,
                stripesEnabled     = prefs[Keys.STRIPES_ENABLED]    ?: true,
                stripeDensity      = prefs[Keys.STRIPE_DENSITY]     ?: 2f,
                vignetteEnabled    = prefs[Keys.VIGNETTE_ENABLED]   ?: true,
                vignetteIntensity  = prefs[Keys.VIGNETTE_INTENSITY] ?: 0.35f,
                faceDetectionEnabled = prefs[Keys.FACE_DETECTION]   ?: true,
                startOnBoot        = prefs[Keys.START_ON_BOOT]      ?: false,
            )
        }

    val disclosureShownFlow: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.DISCLOSURE_SHOWN] ?: false }

    // ── Suspend Setters ───────────────────────────────────────────────────────
    suspend fun setIntensity(value: Float) = context.dataStore.edit {
        it[Keys.INTENSITY] = value.coerceIn(0f, 1f)
    }

    suspend fun setSafeZoneWidth(value: Float) = context.dataStore.edit {
        it[Keys.SAFE_ZONE_WIDTH] = value.coerceIn(0.05f, 0.50f)
    }

    suspend fun setStripesEnabled(value: Boolean) = context.dataStore.edit {
        it[Keys.STRIPES_ENABLED] = value
    }

    suspend fun setStripeDensity(value: Float) = context.dataStore.edit {
        it[Keys.STRIPE_DENSITY] = value.coerceIn(1f, 4f)
    }

    suspend fun setVignetteEnabled(value: Boolean) = context.dataStore.edit {
        it[Keys.VIGNETTE_ENABLED] = value
    }

    suspend fun setVignetteIntensity(value: Float) = context.dataStore.edit {
        it[Keys.VIGNETTE_INTENSITY] = value.coerceIn(0f, 1f)
    }

    suspend fun setFaceDetectionEnabled(value: Boolean) = context.dataStore.edit {
        it[Keys.FACE_DETECTION] = value
    }

    suspend fun setStartOnBoot(value: Boolean) = context.dataStore.edit {
        it[Keys.START_ON_BOOT] = value
    }

    suspend fun setDisclosureShown(value: Boolean) = context.dataStore.edit {
        it[Keys.DISCLOSURE_SHOWN] = value
    }

    suspend fun saveConfig(config: OverlayConfig) = context.dataStore.edit { prefs ->
        prefs[Keys.INTENSITY]          = config.intensity
        prefs[Keys.SAFE_ZONE_WIDTH]    = config.safeZoneWidth
        prefs[Keys.STRIPES_ENABLED]    = config.stripesEnabled
        prefs[Keys.STRIPE_DENSITY]     = config.stripeDensity
        prefs[Keys.VIGNETTE_ENABLED]   = config.vignetteEnabled
        prefs[Keys.VIGNETTE_INTENSITY] = config.vignetteIntensity
        prefs[Keys.FACE_DETECTION]     = config.faceDetectionEnabled
        prefs[Keys.START_ON_BOOT]      = config.startOnBoot
    }
}
