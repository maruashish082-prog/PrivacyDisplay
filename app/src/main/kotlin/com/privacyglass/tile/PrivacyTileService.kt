package com.privacyglass.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.privacyglass.MainActivity
import com.privacyglass.service.OverlayService
import com.privacyglass.util.PermissionManager

/**
 * Quick Settings tile.
 * - If service is running: toggles overlay ON/OFF
 * - If service is not running: opens app for permission flow
 *
 * To add: pull down twice → tap pencil → drag "Privacy Glass" tile up.
 */
class PrivacyTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (!PermissionManager.hasAllRequiredPermissions(this)) {
            // Open main app for permission setup
            startActivityAndCollapse(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            return
        }

        if (OverlayService.isRunning) {
            // Toggle the overlay
            ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_TOGGLE
                }
            )
        } else {
            // Start service
            ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java)
            )
        }

        // Brief delay then refresh tile state
        qsTile?.let {
            it.state = Tile.STATE_ACTIVE
            it.updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    private fun refreshTile() {
        qsTile?.apply {
            state = when {
                !PermissionManager.hasAllRequiredPermissions(context) -> Tile.STATE_UNAVAILABLE
                OverlayService.isRunning -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            label = "Privacy Glass"
            updateTile()
        }
    }
}
