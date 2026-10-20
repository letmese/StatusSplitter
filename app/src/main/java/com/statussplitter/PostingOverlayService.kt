package com.statussplitter

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Legacy component retained for upgrade compatibility; overlay automation retired. */
class PostingOverlayService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
