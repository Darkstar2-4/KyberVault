package com.kybervault

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Background service that detects when user swipes the app from recents.
 * onTaskRemoved is called ONLY on deliberate task removal, not on app switching.
 */
class WipeService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        val prefs = getSharedPreferences("kybervault_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("wipe_on_exit", true)) {
            (application as? KyberVaultApp)?.let { app ->
                app.keyVault.destroyAll()
                app.keyVault.deleteTempState(this)
            }
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
