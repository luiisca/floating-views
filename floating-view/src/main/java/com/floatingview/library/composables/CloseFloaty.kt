package com.floatingview.library.composables

import android.graphics.Point
import android.graphics.PointF
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import com.floatingview.library.helpers.getScreenSizeWithoutInsets
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CloseFloaty(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  modifier: Modifier = Modifier,
  isInvisible: Boolean? = true,
  content: @Composable BoxScope.() -> Unit
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current

  var screenSize by remember { mutableStateOf(getScreenSizeWithoutInsets(context, density, configuration)) }
  var contentSize by remember { mutableStateOf(IntSize.Zero) }

  if (isInvisible == true) {
    LaunchedEffect(key1 = configuration, key2 = contentSize) {
      val oldScreenSize = screenSize
      val newScreenSize = getScreenSizeWithoutInsets(context, density, configuration)
      screenSize = newScreenSize

      if (contentSize != IntSize.Zero) {
        val botPaddingPx = with(density) { 16.dp.toPx() }
        val newPoint = Point(
          (newScreenSize.width / 2) - (contentSize.width / 2),
          newScreenSize.height - contentSize.height - botPaddingPx.toInt()
        )

        Log.d("CloseFloaty", "about to update view, newPoint: $newPoint")
        windowManager.updateViewLayout(containerView, layoutParams.apply {
          x = newPoint.x
          y = newPoint.y
        })

        containerView.visibility = View.VISIBLE
      }
    }
  }

  Box(
    modifier = modifier
      .onSizeChanged { size ->
        Log.d("❌", "closeFloaty size: $size")
        contentSize = size
        layoutParams.width = size.width
        layoutParams.height = size.height
        windowManager.updateViewLayout(containerView, layoutParams)
      }
  ) {
    content()
  }
}

fun isWithinCloseArea(floatyCenterPointF: PointF, closeCenterPointF: PointF, closingThreshold: Float): Boolean {
  return distance(floatyCenterPointF, closeCenterPointF) <= closingThreshold
}

private fun distance(pointA: PointF, pointB: PointF): Float {
  return sqrt((pointA.x - pointB.x).pow(2) + (pointA.y - pointB.y).pow(2))
}