package com.privacydisplay

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

/**
 * Quick Settings tile — drag it into your Quick Settings panel.
 * Tap to toggle the privacy filter on/off without opening the app.
 */
class PrivacyTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (PrivacyService.isRunning) {
            // Send toggle action to running service
            val intent = Intent(this, PrivacyService::class.java).apply {
                action = PrivacyService.ACTION_TOGGLE
            }
            ContextCompat.startForegroundService(this, intent)
        } else {
            // Open app for full permission flow
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (PrivacyService.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (PrivacyService.isRunning) "Privacy ON" else "Privacy OFF"
            updateTile()
        }
    }
}
