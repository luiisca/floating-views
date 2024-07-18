package com.floatingview.library.helpers

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun getScreenSizeWithoutInsets(context: Context, density: Density, configuration: Configuration): IntSize {
  val screenWidth = configuration.screenWidthDp.dp

  val screenHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val metrics: WindowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
    val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
      WindowInsets.Type.navigationBars() or WindowInsets.Type.statusBars()
    )
    with(density) { (metrics.bounds.height() - insets.top - insets.bottom).toDp() }
  } else {
    configuration.screenHeightDp.dp
  }

  return IntSize(
    with(density) { screenWidth.roundToPx() },
    with(density) { screenHeight.roundToPx() }
  )
}
