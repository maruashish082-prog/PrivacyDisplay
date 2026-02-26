package com.privacyglass.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.privacyglass.ui.theme.*
import com.privacyglass.util.PermissionManager

@Composable
fun PermissionScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current

    var hasOverlay  by remember { mutableStateOf(PermissionManager.hasOverlayPermission(context)) }
    var hasNotif    by remember { mutableStateOf(PermissionManager.hasNotificationPermission(context)) }
    var hasCamera   by remember { mutableStateOf(PermissionManager.hasCameraPermission(context)) }

    // Camera permission launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    // Notification permission launcher
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotif = granted }

    // Overlay permission launcher (opens Settings)
    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasOverlay = PermissionManager.hasOverlayPermission(context) }

    // When all required permissions are granted, proceed
    LaunchedEffect(hasOverlay, hasNotif) {
        if (hasOverlay && hasNotif) onAllGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Permissions Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "Privacy Glass needs the following permissions to protect your screen",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // Permission rows
        PermissionRow(
            icon = Icons.Default.Layers,
            title = "Display Over Other Apps",
            description = "Required to show the privacy filter on top of all apps",
            granted = hasOverlay,
            required = true,
            onRequest = {
                overlayLauncher.launch(PermissionManager.overlayPermissionIntent(context))
            }
        )
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Column {
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    description = "Shows a persistent control in your notification shade",
                    granted = hasNotif,
                    required = true,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        PermissionRow(
            icon = Icons.Default.CameraFront,
            title = "Camera",
            description = "Optional: auto-activate when another face is detected nearby",
            granted = hasCamera,
            required = false,
            onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
        )

        Spacer(Modifier.weight(1f))

        // Continue button (only when required permissions granted)
        Button(
            onClick = onAllGranted,
            enabled = hasOverlay && hasNotif,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    required: Boolean,
    onRequest: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (granted) SuccessGreen.copy(alpha = 0.15f)
                        else PrimaryBlue.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (granted) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (granted) SuccessGreen else PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (!required) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Optional",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(BgCardAlt)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(description, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
            if (!granted) {
                TextButton(onClick = onRequest) {
                    Text("Grant", color = PrimaryBlue, fontSize = 13.sp)
                }
            }
        }
    }
}
