package com.example.cupcake

import FloatView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import kotlin.random.Random

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
        floatViewLifecycleOwner.onStart()

        Log.d("❌FloatService", "onStartCommand")

        val params = WindowManager.LayoutParams().apply {
            this.width = WindowManager.LayoutParams.WRAP_CONTENT
            this.height = WindowManager.LayoutParams.WRAP_CONTENT
            this.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            this.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            this.format = PixelFormat.TRANSLUCENT
            this.x = Random.nextInt(0, 1000)
            this.y = Random.nextInt(0, 1000)
        }
        try {
            val floatView = composeFloatView()
//            Log.d("✅FloatView-hashcode", floatView!!.hashCode().toString())
            windowManager.addView(floatView!!, params)
        } catch (e: Exception) {
            Log.e("❌FloatService", "ERROR: ", e)

            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        floatViewLifecycleOwner.onPause()
        floatViewLifecycleOwner.onStop()
        floatViewLifecycleOwner.onDestroy()
    }

    private fun composeFloatView(): View {
        val floatView = ComposeView(this).apply {
            this.setContent {
                FloatView(windowManager, this)
            }
        }

        floatViewLifecycleOwner.attachToDecorView(floatView)

        return floatView
    }
}