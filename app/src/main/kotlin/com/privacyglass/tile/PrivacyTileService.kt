package com.privacyglass.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.privacyglass.MainActivity
import com.privacyglass.service.OverlayService
import com.privacyglass.util.PermissionManager

class PrivacyTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        if (!PermissionManager.hasAllRequiredPermissions(applicationContext)) {
            startActivityAndCollapse(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            return
        }

        if (OverlayService.isRunning) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_TOGGLE
                }
            )
        } else {
            ContextCompat.startForegroundService(
                this,
                Intent(this, OverlayService::class.java)
            )
        }

        qsTile?.let {
            it.state = Tile.STATE_ACTIVE
            it.updateTile()
        }
    }

    private fun refreshTile() {
        qsTile?.apply {
            state = when {
                !PermissionManager.hasAllRequiredPermissions(applicationContext) -> Tile.STATE_UNAVAILABLE
                OverlayService.isRunning -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            label = "Privacy Glass"
            updateTile()
        }
    }
}
