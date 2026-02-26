package com.privacyglass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.privacyglass.ui.screens.HomeScreen
import com.privacyglass.ui.screens.PermissionScreen
import com.privacyglass.ui.screens.SettingsScreen
import com.privacyglass.ui.theme.PrivacyGlassTheme
import com.privacyglass.util.PermissionManager
import com.privacyglass.viewmodel.PrivacyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PrivacyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PrivacyGlassTheme {
                PrivacyGlassApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionState()
    }
}

@Composable
private fun PrivacyGlassApp(viewModel: PrivacyViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh permissions whenever lifecycle resumes
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissionState()
        }
    }

    // Simple manual nav stack (avoid full NavHost for simplicity & compile safety)
    var screen by remember { mutableStateOf(Screen.AUTO) }

    // Determine starting screen
    val resolvedScreen = when {
        screen == Screen.PERMISSIONS -> Screen.PERMISSIONS
        screen == Screen.SETTINGS    -> Screen.SETTINGS
        !uiState.hasOverlayPermission || !uiState.hasNotificationPermission -> Screen.PERMISSIONS
        else -> Screen.HOME
    }

    when (resolvedScreen) {
        Screen.PERMISSIONS -> PermissionScreen(
            onAllGranted = { screen = Screen.HOME }
        )
        Screen.HOME -> HomeScreen(
            uiState = uiState,
            viewModel = viewModel,
            onNavigateToSettings = { screen = Screen.SETTINGS }
        )
        Screen.SETTINGS -> SettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { screen = Screen.HOME }
        )
        Screen.AUTO -> { /* resolved above */ }
    }
}

private enum class Screen { AUTO, PERMISSIONS, HOME, SETTINGS }
