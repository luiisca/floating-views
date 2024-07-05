package com.floatingview.library.composables

import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView

@Composable
fun MainFloaty(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  modifier: Modifier = Modifier,
  onMove: (Int, Int) -> Unit,
  onTap: () -> Unit,
  content: @Composable BoxScope.() -> Unit
) {
  var newLayoutPointX by remember { mutableIntStateOf(layoutParams.x) }
  var newLayoutPointY by remember { mutableIntStateOf(layoutParams.y) }

  Box(
    modifier = modifier
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = {
            onTap()
          }
        )
      }
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { offset ->
            // display close bubble without doing any calculation since detectDragGestures already does
          },
          onDrag = { change, _ ->
            newLayoutPointX += (change.position.x - change.previousPosition.x).toInt()
            newLayoutPointY += (change.position.y - change.previousPosition.y)
              .toInt()

            windowManager.updateViewLayout(containerView, layoutParams.apply {
              x = newLayoutPointX
              y = newLayoutPointY
            })

            onMove(newLayoutPointX, newLayoutPointY)
          },
          onDragEnd = {
          }
        )
      }
  ) {
    content()
  }
}
