package com.sample.app

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.sample.app.ui.ExpandedView
import com.sample.app.ui.FloatView
import io.github.luiisca.floating.views.CloseBehavior
import io.github.luiisca.floating.views.MainFloatConfig
import io.github.luiisca.floating.views.CloseFloatConfig
import io.github.luiisca.floating.views.ExpandedFloatConfig
import io.github.luiisca.floating.views.FloatingViewsController
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
      // pass `stopSelf` to stop the service after closing the last dynamic floating view.
      stopService = { stopSelf() },
      mainFloatConfig = MainFloatConfig(
        composable = { FloatView() },
        // Add other main float configurations here
      ),
      closeFloatConfig = CloseFloatConfig(
        closeBehavior = CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT,
        // Add other close float configurations here
      ),
      expandedFloatConfig = ExpandedFloatConfig(
        composable = {close -> ExpandedView(close) },
        // Add other expanded float configurations here
      )
    )

    // elevate service to foreground status to make it less likely to be terminated by the system under memory pressure
    floatingViewsController.initializeAsForegroundService()

    // Optional: React to service running state changes
    FloatServiceStateManager.setServiceRunning(true)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    // Creates and starts a new dynamic, interactive floating view.
    floatingViewsController.startDynamicFloatingView()

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    // Removes all views added while the Service was alive
    floatingViewsController.stopAllDynamicFloatingViews()

    // Optional: React to service running state changes
    FloatServiceStateManager.setServiceRunning(false)
  }
}