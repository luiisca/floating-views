package io.github.luiisca.floating.views

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.github.luiisca.floating.views.helpers.ConfigManager
import io.github.luiisca.floating.views.helpers.FloatServiceStateManager

class FloatingViewsService: Service() {
  private lateinit var floatingViewsController: FloatingViewsController
  override fun onBind(intent: Intent): IBinder {
    TODO("Return the communication channel to the service.")
  }

  override fun onCreate() {
    super.onCreate()

    floatingViewsController = FloatingViewsController(
      this,
      stopService = { stopSelf() },
    )

    // elevate service to foreground status to make it less likely to be terminated by the system under memory pressure
    floatingViewsController.initializeAsForegroundService()
    FloatServiceStateManager.setServiceRunning(true)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    val configId = intent?.getStringExtra("CONFIG_ID") ?: return START_NOT_STICKY
    val config = ConfigManager.getConfig(configId) ?: return START_NOT_STICKY
    // Creates and starts a new dynamic, interactive floating view.
    floatingViewsController.startDynamicFloatingView(config)

    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()

    // Removes all views added while the Service was alive
    floatingViewsController.stopAllDynamicFloatingViews()
    FloatServiceStateManager.setServiceRunning(false)
  }
}