package com.sample.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sample.app.ui.ExpandedView
import com.sample.app.ui.FloatView
import io.github.luiisca.floating.views.CloseBehavior
import io.github.luiisca.floating.views.CloseFloatConfig
import io.github.luiisca.floating.views.ExpandedFloatConfig
import io.github.luiisca.floating.views.FloatingViewsController
import io.github.luiisca.floating.views.MainFloatConfig
import io.github.luiisca.floating.views.helpers.FloatServiceStateManager

class Service : Service() {
  private lateinit var floatingViewsController: FloatingViewsController
  override fun onBind(intent: Intent): IBinder {
    TODO("Return the communication channel to the service.")
  }

  override fun onCreate() {
    super.onCreate()

    floatingViewsController = FloatingViewsController(
      this,
      stopService = { stopSelf() },
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

    floatingViewsController.startForegroundService()
    FloatServiceStateManager.setServiceRunning(true)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    floatingViewsController.initializeNewFloatSystem()

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    floatingViewsController.shutdownAllFloatSystems()
    FloatServiceStateManager.setServiceRunning(false)
  }
}