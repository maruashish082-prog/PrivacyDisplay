package com.privacydisplay

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View

/**
 * Simulates a physical privacy screen filter.
 *
 * Effect: The center of the screen is fully visible.
 * The left and right edges (and top/bottom) are darkened with a gradient,
 * mimicking how a narrow-viewing-angle privacy filter appears from the side.
 *
 * When viewed head-on: content is clear.
 * From the side: screen appears dark (because the dark gradient dominates).
 */
class PrivacyOverlayView(context: Context) : View(context) {

    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val topBottomPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 0.0 = transparent (off), 1.0 = fully opaque dark (max privacy) */
    var intensity: Float = 0.85f
        set(value) {
            field = value.coerceIn(0f, 1f)
            rebuildShaders()
            invalidate()
        }

    /**
     * How far the gradient reaches inward from each edge (as fraction of screen width).
     * 0.1 = narrow gradient, 0.5 = gradient covers half the screen from each side.
     */
    var gradientWidth: Float = 0.40f
        set(value) {
            field = value.coerceIn(0.05f, 0.65f)
            rebuildShaders()
            invalidate()
        }

    private var leftShader: LinearGradient? = null
    private var rightShader: LinearGradient? = null
    private var topShader: LinearGradient? = null
    private var bottomShader: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShaders()
    }

    private fun rebuildShaders() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val edgeReach = w * gradientWidth
        val vEdgeReach = h * (gradientWidth * 0.4f) // top/bottom less aggressive

        // Alpha value for the darkest edge point (0-255)
        val maxAlpha = (intensity * 245).toInt().coerceIn(0, 255)
        val edgeColor = (maxAlpha shl 24) or 0x000000     // semi-transparent black
        val clearColor = 0x00000000                         // fully transparent

        // Left: black (left edge) → transparent (inward)
        leftShader = LinearGradient(
            0f, 0f, edgeReach, 0f,
            intArrayOf(edgeColor, edgeColor, clearColor),
            floatArrayOf(0f, 0.15f, 1f),
            Shader.TileMode.CLAMP
        )

        // Right: transparent (inward) → black (right edge)
        rightShader = LinearGradient(
            w - edgeReach, 0f, w, 0f,
            intArrayOf(clearColor, edgeColor, edgeColor),
            floatArrayOf(0f, 0.85f, 1f),
            Shader.TileMode.CLAMP
        )

        // Top: black (top edge) → transparent
        topShader = LinearGradient(
            0f, 0f, 0f, vEdgeReach,
            intArrayOf(edgeColor, clearColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        // Bottom: transparent → black (bottom edge)
        bottomShader = LinearGradient(
            0f, h - vEdgeReach, 0f, h,
            intArrayOf(clearColor, edgeColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Draw left gradient
        leftShader?.let {
            edgePaint.shader = it
            canvas.drawRect(0f, 0f, w * gradientWidth, h, edgePaint)
        }

        // Draw right gradient
        rightShader?.let {
            edgePaint.shader = it
            canvas.drawRect(w - w * gradientWidth, 0f, w, h, edgePaint)
        }

        // Draw top gradient
        topShader?.let {
            topBottomPaint.shader = it
            canvas.drawRect(0f, 0f, w, h * (gradientWidth * 0.4f), topBottomPaint)
        }

        // Draw bottom gradient
        bottomShader?.let {
            topBottomPaint.shader = it
            canvas.drawRect(0f, h - h * (gradientWidth * 0.4f), w, h, topBottomPaint)
        }
    }
}
