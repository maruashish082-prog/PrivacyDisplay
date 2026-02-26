package com.privacydisplay

import android.content.Context
import android.graphics.*
import android.view.View

/**
 * Simulates a real privacy glass / micro-louver filter.
 *
 * How real privacy glass works:
 * - Tiny vertical louvers (like micro-blinds) block side-angle light
 * - Head-on view: screen is fully clear
 * - Side view (>30° angle): screen appears completely black
 *
 * This overlay replicates that by:
 * 1. Covering ~80% of screen width from each side with near-opaque black
 * 2. Only a narrow center strip stays clear (the "safe zone")
 * 3. Sharp transition with a very short gradient - not a gentle fade
 * 4. A subtle dark tint over the entire screen to reduce brightness at angles
 * 5. A micro-louver texture pattern drawn over the screen
 */
class PrivacyOverlayView(context: Context) : View(context) {

    // How much of the center is "safe" / visible (0.15 = only 15% center is clear)
    var safeZoneWidth: Float = 0.20f
        set(value) { field = value.coerceIn(0.05f, 0.50f); rebuildShaders(); invalidate() }

    // Max darkness of the blocked zones (0.0-1.0)
    var intensity: Float = 0.97f
        set(value) { field = value.coerceIn(0f, 1f); rebuildShaders(); invalidate() }

    // How wide the transition gradient is (keep small for sharp cutoff like real glass)
    var gradientWidth: Float = 0.08f

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val louverPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Left blocked zone shader
    private var leftShader: LinearGradient? = null
    // Right blocked zone shader
    private var rightShader: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShaders()
    }

    private fun rebuildShaders() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val blockAlpha = (intensity * 252).toInt().coerceIn(0, 255)
        val blockedColor = (blockAlpha shl 24) or 0x000000
        val clearColor = 0x00000000

        // Center safe zone boundaries
        val safeLeft = w * (0.5f - safeZoneWidth / 2f)
        val safeRight = w * (0.5f + safeZoneWidth / 2f)
        val fadeWidth = w * gradientWidth

        // Left shader: black from left edge → fades to clear at safe zone start
        leftShader = LinearGradient(
            0f, 0f,
            safeLeft + fadeWidth, 0f,
            intArrayOf(blockedColor, blockedColor, clearColor),
            floatArrayOf(0f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )

        // Right shader: clear at safe zone end → fades to black at right edge
        rightShader = LinearGradient(
            safeRight - fadeWidth, 0f,
            w, 0f,
            intArrayOf(clearColor, blockedColor, blockedColor),
            floatArrayOf(0f, 0.15f, 1f),
            Shader.TileMode.CLAMP
        )

        // Subtle full-screen tint (simulates reduced brightness at any angle)
        tintPaint.color = Color.argb((intensity * 40).toInt(), 0, 0, 0)

        // Louver lines paint (very subtle vertical lines like micro-louvers)
        louverPaint.apply {
            color = Color.argb((intensity * 18).toInt(), 0, 0, 0)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Draw subtle full-screen tint first
        canvas.drawRect(0f, 0f, w, h, tintPaint)

        // 2. Draw micro-louver texture lines across entire screen
        val louverSpacing = 3f
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h, louverPaint)
            x += louverSpacing
        }

        // 3. Draw LEFT blocked zone
        leftShader?.let {
            overlayPaint.shader = it
            val safeLeft = w * (0.5f - safeZoneWidth / 2f) + w * gradientWidth
            canvas.drawRect(0f, 0f, safeLeft, h, overlayPaint)
        }

        // 4. Draw RIGHT blocked zone
        rightShader?.let {
            overlayPaint.shader = it
            val safeRight = w * (0.5f + safeZoneWidth / 2f) - w * gradientWidth
            canvas.drawRect(safeRight, 0f, w, h, overlayPaint)
        }
    }
}
