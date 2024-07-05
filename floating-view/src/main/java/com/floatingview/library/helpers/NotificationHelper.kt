package com.floatingview.library.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.floatingview.library.R

class NotificationHelper(
    private val context: Context,
    private val channelId: String = "floaty_service",
    private val channelName: String = "floaty",
    val notificationId: Int = 101,
) {
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            NotificationManagerCompat.from(context).createNotificationChannel(channel)
        }
    }

    fun createDefaultNotification(icon: Int = R.drawable.round_bubble_chart_24,title: String = "Floaty is running"): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setOngoing(true)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
    }
}
