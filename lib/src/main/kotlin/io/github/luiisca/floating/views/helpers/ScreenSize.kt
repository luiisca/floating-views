package io.github.luiisca.floating.views.helpers

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat

fun getScreenSizeWithoutInsets(context: Context): IntSize {
  val windowManager = ContextCompat.getSystemService(context, WindowManager::class.java)
    ?: return IntSize.Zero

  return when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
      val windowMetrics = windowManager.maximumWindowMetrics
      val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
        WindowInsets.Type.systemBars()
      )
      IntSize(
        windowMetrics.bounds.width() - (insets.left + insets.right),
        windowMetrics.bounds.height() - (insets.top + insets.bottom)
      )
    }
    else -> {
      val displayMetrics = DisplayMetrics()
      windowManager.defaultDisplay.getMetrics(displayMetrics)
      val statusBarHeight = getStatusBarHeight(context)
      IntSize(
        displayMetrics.widthPixels,
        displayMetrics.heightPixels - statusBarHeight
      )
    }
  }
}

private fun getStatusBarHeight(context: Context): Int {
  val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
  return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
}