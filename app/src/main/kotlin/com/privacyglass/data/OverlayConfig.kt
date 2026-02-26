package com.privacyglass.data

/**
 * Immutable snapshot of all overlay configuration values.
 * Passed between service, view, and ViewModel.
 */
data class OverlayConfig(
    /** 0.0 = transparent, 1.0 = nearly opaque side zones */
    val intensity: Float = 0.95f,

    /** Fraction of screen width that stays visible (center safe zone). 0.1–0.5 */
    val safeZoneWidth: Float = 0.18f,

    /** Whether to draw vertical stripe (louver) texture */
    val stripesEnabled: Boolean = true,

    /** Density of stripes: 1 = sparse, 4 = dense */
    val stripeDensity: Float = 2f,

    /** Whether to apply subtle vignette top/bottom */
    val vignetteEnabled: Boolean = true,

    /** Vignette strength 0.0–1.0 */
    val vignetteIntensity: Float = 0.35f,

    /** Whether face detection auto-triggers the overlay */
    val faceDetectionEnabled: Boolean = true,

    /** Start on boot */
    val startOnBoot: Boolean = false,

    /** Active state — does not persist but is runtime state */
    val isActive: Boolean = false
)
