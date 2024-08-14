package com.sample.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sample.app.ui.ExpandedView
import com.sample.app.ui.FloatView
import io.github.luiisca.floating.views.CloseBehavior
import io.github.luiisca.floating.views.CloseFloatConfig
import io.github.luiisca.floating.views.ExpandedFloatConfig
import io.github.luiisca.floating.views.FloatingViewsBuilder
import io.github.luiisca.floating.views.MainFloatConfig

class Service : Service() {
  private lateinit var floatingViewsBuilder: FloatingViewsBuilder
  override fun onBind(intent: Intent): IBinder {
    TODO("Return the communication channel to the service.")
  }

  override fun onCreate() {
    super.onCreate()

    floatingViewsBuilder = FloatingViewsBuilder(
      this,
      mainFloatConfig = MainFloatConfig(
        composable = { FloatView() },
      ),
      closeFloatConfig = CloseFloatConfig(
        closeBehavior = CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT,
      ),
      expandedFloatConfig = ExpandedFloatConfig(
        composable = {close -> ExpandedView(close) },
      )
    )

    floatingViewsBuilder.startForegroundWithDefaultNotification()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    floatingViewsBuilder.addFloat()

    return START_STICKY
  }
}