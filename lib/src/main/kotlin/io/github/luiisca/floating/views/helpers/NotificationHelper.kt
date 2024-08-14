package io.github.luiisca.floating.views.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(
    private val context: Context,
    private val channelId: String = "floating_views_service",
    private val channelName: String = "Floating views",
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

    fun createDefaultNotification(icon: Int, title: String): Notification {
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
