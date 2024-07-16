package com.floatingview.library.composables

import android.graphics.Point
import android.graphics.PointF
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
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current

  val screenSize = remember {
    mutableStateOf(
      IntSize(
        with(density) { configuration.screenWidthDp.dp.roundToPx() },
        with(density) { configuration.screenHeightDp.dp.roundToPx() }
      )
    )
  }
  val contentSize = remember { mutableStateOf(IntSize.Zero) }

  if (isInvisible == true) {
    LaunchedEffect(key1 = configuration, key2 = contentSize.value) {
      val newScreenWidth = with(density) { configuration.screenWidthDp.dp.roundToPx() }
      val newScreenHeight = with(density) { configuration.screenHeightDp.dp.roundToPx() }
      screenSize.value = IntSize(newScreenWidth, newScreenHeight)

      if (contentSize.value != IntSize.Zero) {
        val botPaddingPx = with(density) { 16.dp.toPx() }
        val newPoint = Point(
          (newScreenWidth / 2) - (contentSize.value.width / 2),
          newScreenHeight - contentSize.value.height - botPaddingPx.toInt()
        )

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
        contentSize.value = size
        layoutParams.width = size.width
        layoutParams.height = size.height
        windowManager.updateViewLayout(containerView, layoutParams)
      }
  ) {
    content()
  }
}

fun isWithinCloseArea(floatyCenterPointF: PointF, closeCenterPointF: PointF, closeThreshold: Float): Boolean {
  return distance(floatyCenterPointF, closeCenterPointF) <= closeThreshold
}

private fun distance(pointA: PointF, pointB: PointF): Float {
  return sqrt((pointA.x - pointB.x).pow(2) + (pointA.y - pointB.y).pow(2))
}