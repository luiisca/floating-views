package com.floatingview.library.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.floatingview.library.canDrawOverlays
import com.floatingview.library.helper.NotificationHelper
import com.floatingview.library.sez

abstract class FloatingBubbleService : Service() {

    open fun startNotificationForeground() {
        val noti = NotificationHelper(this)
        noti.createNotificationChannel()
        startForeground(noti.notificationId, noti.defaultNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        Log.d("✅FloatingBubbleService", "onCreate")
        super.onCreate()

        if (canDrawOverlays().not()) {
            throw SecurityException(
                    "Permission Denied: \"display over other app\" permission IS NOT granted!"
            )
        }

        // setup some screen vlaues based on sdk version
        sez.with(this.applicationContext)

        startNotificationForeground()

        // confusing OOP stuff here but yeah it will be called from instances of classes that expand
        // this one where setup fn exists. hate it though.
        // TODO: unite FloatingBubbleService and ExpandableBubbleService into one
        setup()
    }

    abstract fun setup()

    abstract fun removeAll()

    override fun onDestroy() {
        removeAll()
        super.onDestroy()
    }
}
