package com.privacyglass.overlay

import android.content.Context
import android.graphics.*
import android.view.View
import com.privacyglass.data.OverlayConfig
import kotlin.math.cos
import kotlin.math.PI

/**
 * Production-grade privacy overlay that simulates micro-louver privacy glass.
 *
 * Rendering layers (back to front):
 *   1. Full-screen tint — very subtle, reduces brightness at all angles
 *   2. Left & right blocked zones — near-opaque black with sharp privacy cutoff
 *   3. Vertical stripe texture — simulates louver lines
 *   4. Vignette — top & bottom darkening for realistic depth
 *
 * Performance optimisations:
 *   - Hardware accelerated (set in WindowManager params)
 *   - Shaders rebuilt only when config or size changes (not every frame)
 *   - willNotDraw = false only when active
 *   - All drawing uses pre-allocated Paint objects
 *   - No object allocation inside onDraw
 */
class PrivacyOverlayView(context: Context) : View(context) {

    // ── State ─────────────────────────────────────────────────────────────────
    private var config = OverlayConfig()

    // ── Pre-allocated Paints ──────────────────────────────────────────────────
    private val zonePaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val tintPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // ── Cached shader references ──────────────────────────────────────────────
    private var leftShader:     LinearGradient? = null
    private var rightShader:    LinearGradient? = null
    private var vignetteShader: RadialGradient? = null

    // ── Pre-allocated reusable Rect ───────────────────────────────────────────
    private val drawRect = RectF()

    init {
        // Critical: allow onDraw to be called
        setWillNotDraw(false)
        // Hardware layer for smooth compositing
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Apply a new config. Triggers shader rebuild + redraw. */
    fun applyConfig(newConfig: OverlayConfig) {
        config = newConfig
        rebuildShaders()
        invalidate()
    }

    // ── Size change ───────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShaders()
    }

    // ── Shader construction ───────────────────────────────────────────────────

    private fun rebuildShaders() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val alpha = (config.intensity * 250).toInt().coerceIn(0, 255)
        val blockedColor = (alpha shl 24) // pure black with computed alpha
        val clearColor   = 0x00000000

        // Safe zone: the visible center strip
        val halfSafe  = config.safeZoneWidth / 2f
        val safeLeft  = w * (0.5f - halfSafe)
        val safeRight = w * (0.5f + halfSafe)
        val fade      = w * 0.06f // 6% fade width — short for sharp cutoff

        // Left zone: solid black → fades to clear at safe zone boundary
        leftShader = LinearGradient(
            0f, 0f, safeLeft + fade, 0f,
            intArrayOf(blockedColor, blockedColor, clearColor),
            floatArrayOf(0f, 0.82f, 1f),
            Shader.TileMode.CLAMP
        )

        // Right zone: clear at safe zone boundary → fades to solid black
        rightShader = LinearGradient(
            safeRight - fade, 0f, w, 0f,
            intArrayOf(clearColor, blockedColor, blockedColor),
            floatArrayOf(0f, 0.18f, 1f),
            Shader.TileMode.CLAMP
        )

        // Radial vignette centered on screen
        if (config.vignetteEnabled) {
            val vigAlpha = (config.vignetteIntensity * 180).toInt().coerceIn(0, 200)
            val vigColor = (vigAlpha shl 24)
            vignetteShader = RadialGradient(
                w / 2f, h / 2f,
                maxOf(w, h) * 0.65f,
                intArrayOf(clearColor, vigColor),
                floatArrayOf(0.4f, 1f),
                Shader.TileMode.CLAMP
            )
        } else {
            vignetteShader = null
        }

        // Subtle global tint
        tintPaint.color = Color.argb((config.intensity * 30).toInt(), 0, 0, 0)

        // Stripes
        stripePaint.color = Color.argb((config.intensity * 22).toInt(), 0, 0, 0)
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Global tint
        canvas.drawRect(0f, 0f, w, h, tintPaint)

        // 2. Vertical louver stripes across the whole screen
        if (config.stripesEnabled) {
            val spacing = (4f - config.stripeDensity + 1f) * 3f  // density 1→4 = spacing 12→3px
            var x = 0f
            while (x < w) {
                canvas.drawLine(x, 0f, x, h, stripePaint)
                x += spacing
            }
        }

        // 3. Left blocked zone
        leftShader?.let {
            zonePaint.shader = it
            val safeLeft = w * (0.5f - config.safeZoneWidth / 2f) + w * 0.06f
            drawRect.set(0f, 0f, safeLeft, h)
            canvas.drawRect(drawRect, zonePaint)
        }

        // 4. Right blocked zone
        rightShader?.let {
            zonePaint.shader = it
            val safeRight = w * (0.5f + config.safeZoneWidth / 2f) - w * 0.06f
            drawRect.set(safeRight, 0f, w, h)
            canvas.drawRect(drawRect, zonePaint)
        }

        // 5. Radial vignette (top/bottom edge darkening)
        vignetteShader?.let {
            vignettePaint.shader = it
            canvas.drawRect(0f, 0f, w, h, vignettePaint)
        }
    }
}
