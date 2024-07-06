package com.floaty.app

import CloseFloatyConfig
import FloatView
import FloatiesBuilder
import MainFloatyConfig
import android.app.Service
import android.content.Intent
import android.os.IBinder

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
        composable = {FloatView()}
      ),
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