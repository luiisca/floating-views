package com.example.cupcake

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat

class FloatService : Service() {
    private lateinit var windowManager: WindowManager
    private val floatViewLifecycleOwner = FloatViewLifecycleOwner()

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        floatViewLifecycleOwner.onCreate()

        Log.d("❌FloatService", "onCreate")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // Start foreground service with a notification
        val channelId = "float_service_channel"
        val channel = NotificationChannel(
            channelId,
            "Floating Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Float Service")
            .setContentText("Float service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("❌FloatService", "onStartCommand")

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        try {
            var chatHead = ImageView(this)
//            chatHead.setImageResource(R.drawable.floating2)
//            windowManager.addView(chatHead, params)
            val floatView = composeFloatView()
            windowManager.addView(floatView, params)
        } catch (e: Exception) {
            Log.e("❌FloatService", "ERROR: ", e)

            stopSelf()
        }

        return START_STICKY
    }

    private fun composeFloatView(): View {
        val floatView = ComposeView(this).apply {
            this.setContent {
                FloatView()
            }
        }

        floatViewLifecycleOwner.attachToDecorView(floatView)

        return floatView
    }
}