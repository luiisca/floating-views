package io.github.luiisca.floating.views.ui

import android.graphics.PointF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CloseFloat(
  modifier: Modifier = Modifier,
  updateSize: (newSize: IntSize) -> Unit,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier
      .layout { measurable, constraints ->
        val newConstraints = constraints.copy(maxWidth = Int.MAX_VALUE, maxHeight = Int.MAX_VALUE) // Remove the 880px cap
        val placeable = measurable.measure(newConstraints)
        layout(placeable.width, placeable.height) {
          placeable.placeRelative(0, 0)
        }
      }
      .onSizeChanged { size ->
        updateSize(size)
      }
  ) {
    content()
  }
}

fun isWithinCloseArea(floatCenterPointF: PointF, closeCenterPointF: PointF, closingThreshold: Float): Boolean {
  return distanceBetweenTwoPoints(floatCenterPointF, closeCenterPointF) <= closingThreshold
}

fun distanceBetweenTwoPoints(pointA: PointF, pointB: PointF): Float {
  return sqrt((pointA.x - pointB.x).pow(2) + (pointA.y - pointB.y).pow(2))
}
