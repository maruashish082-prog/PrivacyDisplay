package com.privacyglass.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyglass.ui.components.PrivacyPreviewCard
import com.privacyglass.ui.components.SliderRow
import com.privacyglass.ui.components.SwitchRow
import com.privacyglass.ui.theme.*
import com.privacyglass.viewmodel.PrivacyUiState
import com.privacyglass.viewmodel.PrivacyViewModel

@Composable
fun SettingsScreen(
    uiState: PrivacyUiState,
    viewModel: PrivacyViewModel,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {

            // ── Live preview ──────────────────────────────────────────────────
            SectionHeader("Live Preview")
            PrivacyPreviewCard(config = uiState.config, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))

            // ── Privacy effect ────────────────────────────────────────────────
            SectionHeader("Privacy Effect")
            SettingsCard {
                SliderRow(
                    label = "Side Darkness",
                    value = uiState.config.intensity,
                    onValueChange = viewModel::setIntensity,
                    description = "How opaque the side zones are. Higher = more private."
                )
                Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                SliderRow(
                    label = "Visible Zone Width",
                    value = uiState.config.safeZoneWidth,
                    onValueChange = viewModel::setSafeZoneWidth,
                    valueRange = 0.05f..0.50f,
                    displayValue = "${(uiState.config.safeZoneWidth * 100).toInt()}%",
                    description = "Width of the center strip that remains visible. Smaller = narrower viewing angle."
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Stripe texture ────────────────────────────────────────────────
            SectionHeader("Louver Texture")
            SettingsCard {
                SwitchRow(
                    label = "Enable Stripes",
                    checked = uiState.config.stripesEnabled,
                    onCheckedChange = viewModel::setStripesEnabled,
                    description = "Adds vertical lines simulating real micro-louver glass"
                )
                AnimatedContent(uiState.config.stripesEnabled) { enabled ->
                    if (enabled) {
                        Column {
                            Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                            SliderRow(
                                label = "Stripe Density",
                                value = uiState.config.stripeDensity,
                                onValueChange = viewModel::setStripeDensity,
                                valueRange = 1f..4f,
                                displayValue = when {
                                    uiState.config.stripeDensity <= 1.5f -> "Sparse"
                                    uiState.config.stripeDensity <= 2.5f -> "Medium"
                                    uiState.config.stripeDensity <= 3.5f -> "Dense"
                                    else -> "Very Dense"
                                },
                                description = "How closely spaced the louver lines are"
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Vignette ──────────────────────────────────────────────────────
            SectionHeader("Vignette")
            SettingsCard {
                SwitchRow(
                    label = "Edge Vignette",
                    checked = uiState.config.vignetteEnabled,
                    onCheckedChange = viewModel::setVignetteEnabled,
                    description = "Darkens top and bottom edges for a realistic depth effect"
                )
                AnimatedContent(uiState.config.vignetteEnabled) { enabled ->
                    if (enabled) {
                        Column {
                            Divider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))
                            SliderRow(
                                label = "Vignette Strength",
                                value = uiState.config.vignetteIntensity,
                                onValueChange = viewModel::setVignetteIntensity,
                                description = "How strong the top/bottom darkening is"
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Face detection ────────────────────────────────────────────────
            SectionHeader("Auto Detection")
            SettingsCard {
                SwitchRow(
                    label = "Face Detection",
                    checked = uiState.config.faceDetectionEnabled,
                    onCheckedChange = viewModel::setFaceDetectionEnabled,
                    description = "Automatically activates the filter when a 2nd face is detected via front camera"
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── System ────────────────────────────────────────────────────────
            SectionHeader("System")
            SettingsCard {
                SwitchRow(
                    label = "Start on Boot",
                    checked = uiState.config.startOnBoot,
                    onCheckedChange = viewModel::setStartOnBoot,
                    description = "Automatically start Privacy Glass when your device turns on"
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── App info ──────────────────────────────────────────────────────
            Text(
                "Privacy Glass v1.0.0\nNo data is collected or transmitted. All processing is on-device.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun AnimatedContent(condition: Boolean, content: @Composable (Boolean) -> Unit) {
    androidx.compose.animation.AnimatedContent(
        targetState = condition,
        label = "animated_content"
    ) { state ->
        content(state)
    }
}
