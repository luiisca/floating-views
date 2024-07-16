package com.floaty.app

import FloatView
import android.app.Service
import android.content.Intent
import android.graphics.Point
import android.graphics.PointF
import android.os.IBinder
import com.floatingview.library.CloseFloatyConfig
import com.floatingview.library.FloatiesBuilder
import com.floatingview.library.MainFloatyConfig

class FloatyService : Service() {
  private lateinit var floatiesBuilder: FloatiesBuilder
  override fun onBind(intent: Intent): IBinder {
    TODO("Return the communication channel to the service.")
  }

  override fun onCreate() {
    super.onCreate()

    floatiesBuilder = FloatiesBuilder(
      this,
      mainFloatyConfig = MainFloatyConfig(
        composable = {FloatView()},
        enableAnimations = false,
        isSnapToEdgeEnabled = false,
      ),
      closeFloatyConfig = CloseFloatyConfig(
        startPointDp = PointF(100f, 200f)
      )
    )

    // can be omitted for service.startForeground with custom notification or just pass custom properties
    floatiesBuilder.startForegroundWithDefaultNotification()
    floatiesBuilder.setup(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    floatiesBuilder.addFloaty()

    return START_STICKY
  }
}