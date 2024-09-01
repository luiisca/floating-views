package io.github.luiisca.floating.views.helpers

import android.content.Context
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import android.util.DisplayMetrics

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
      IntSize(
        displayMetrics.widthPixels,
        displayMetrics.heightPixels
      )
    }
  }
}