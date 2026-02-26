package com.privacyglass.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyglass.ui.components.PrivacyPreviewCard
import com.privacyglass.ui.theme.*
import com.privacyglass.viewmodel.PrivacyUiState
import com.privacyglass.viewmodel.PrivacyViewModel

@Composable
fun HomeScreen(
    uiState: PrivacyUiState,
    viewModel: PrivacyViewModel,
    onNavigateToSettings: () -> Unit
) {
    // Disclosure dialog
    if (uiState.showDisclosureDialog) {
        DisclosureDialog(
            onAccept  = viewModel::onDisclosureAccepted,
            onDismiss = viewModel::onDisclosureDismissed
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Privacy Glass", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Screen privacy filter", fontSize = 13.sp, color = TextSecondary)
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Face detection warning ────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.extraFaceDetected,
            enter = slideInVertically() + fadeIn(),
            exit  = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = WarningOrange.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Extra face detected — privacy filter auto-activated",
                        color = WarningOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Power button ──────────────────────────────────────────────────────
        PowerButton(
            isOn = uiState.serviceRunning,
            onClick = viewModel::requestToggle
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (uiState.serviceRunning)
                "Privacy filter active across all apps"
            else
                "Tap to activate the privacy filter",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── Live preview ──────────────────────────────────────────────────────
        SectionLabel("Live Preview")
        Spacer(Modifier.height(8.dp))
        PrivacyPreviewCard(config = uiState.config, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))

        // ── Quick stats ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Visible Zone",
                value = "${(uiState.config.safeZoneWidth * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Darkness",
                value = "${(uiState.config.intensity * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Stripes",
                value = if (uiState.config.stripesEnabled) "ON" else "OFF",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Quick tiles tip ───────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCardAlt),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Quick Settings Tile", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pull down twice → pencil icon → drag 'Privacy Glass' tile for instant toggle",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PowerButton(isOn: Boolean, onClick: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue  = if (isOn) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val bgColor = if (isOn) SuccessGreen else TextSecondary.copy(alpha = 0.3f)
    val iconColor = if (isOn) Color.White else TextSecondary

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(bgColor.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isOn) "Turn Off" else "Turn On",
                    tint = iconColor,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = PrimaryBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DisclosureDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        title = {
            Text("About Privacy Glass", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "Privacy Glass draws a semi-transparent overlay on top of all other apps using Android's " +
                "'Display over other apps' permission.\n\n" +
                "• The filter is visible to you and anyone looking at your screen head-on\n" +
                "• People viewing from the side see a mostly dark screen\n" +
                "• The app uses your front camera only for face detection (optional)\n" +
                "• No screen content is captured or transmitted\n" +
                "• A foreground service runs while the filter is active\n\n" +
                "You can stop the filter at any time via the notification or this app.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("I Understand") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
