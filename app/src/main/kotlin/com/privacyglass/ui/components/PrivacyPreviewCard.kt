package com.privacyglass.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyglass.data.OverlayConfig

/**
 * A miniature live preview of what the privacy overlay looks like.
 * Redrawn instantly when config changes.
 */
@Composable
fun PrivacyPreviewCard(config: OverlayConfig, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1D27)),
        contentAlignment = Alignment.Center
    ) {
        // Simulated "screen content" text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓",
                color = Color(0xFF4B8EF1).copy(alpha = 0.6f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Screen Content",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓",
                color = Color(0xFF4B8EF1).copy(alpha = 0.6f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Overlay simulation drawn on top
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val blockAlpha = (config.intensity * 245).toInt().coerceIn(0, 255)
            val blockedColor = android.graphics.Color.argb(blockAlpha, 0, 0, 0)
            val clearColor   = android.graphics.Color.argb(0, 0, 0, 0)

            val halfSafe  = config.safeZoneWidth / 2f
            val safeLeft  = w * (0.5f - halfSafe)
            val safeRight = w * (0.5f + halfSafe)
            val fade      = w * 0.06f

            // Left zone
            val leftBrush = Brush.horizontalGradient(
                0f to Color(blockedColor),
                0.82f to Color(blockedColor),
                1f to Color(clearColor),
                startX = 0f,
                endX = safeLeft + fade
            )
            drawRect(brush = leftBrush, size = size.copy(width = safeLeft + fade))

            // Right zone
            val rightBrush = Brush.horizontalGradient(
                0f to Color(clearColor),
                0.18f to Color(blockedColor),
                1f to Color(blockedColor),
                startX = safeRight - fade,
                endX = w
            )
            drawRect(
                brush = rightBrush,
                topLeft = Offset(safeRight - fade, 0f),
                size = size.copy(width = w - (safeRight - fade))
            )

            // Stripes
            if (config.stripesEnabled) {
                val stripeAlpha = (config.intensity * 20).toInt().coerceIn(0, 255)
                val spacing = (4f - config.stripeDensity + 1f) * 3f
                var x = 0f
                while (x < w) {
                    drawLine(
                        color = Color(android.graphics.Color.argb(stripeAlpha, 0, 0, 0)),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.2f
                    )
                    x += spacing
                }
            }
        }

        // Label
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("PREVIEW", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}
