package com.floaty.app

import FloatView
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.floatingview.library.CloseBehavior
import com.floatingview.library.CloseFloatyConfig
import com.floatingview.library.ExpandedFloatyConfig
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
//      enableAnimations = false,
      mainFloatyConfig = MainFloatyConfig(
        composable = {FloatView()},
//        isSnapToEdgeEnabled = false,
//        startPointDp = PointF(100f, 200f)
      ),
      closeFloatyConfig = CloseFloatyConfig(
//        viewFactory = { context ->
//          View(context).apply {
//            layoutParams = ViewGroup.LayoutParams(100, 100)
//            setBackgroundResource(com.floatingview.library.R.drawable.round_bubble_chart_24)
//          }
//        },
//        startPointDp = PointF(240f, 600f),
//        mountThresholdDp = 50f,
//        closingThresholdDp = 30f,
        closeBehavior = CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT,
      ),
      expandedFloatyConfig = ExpandedFloatyConfig(
        composable = {hide -> ExtendedView(hide)},
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