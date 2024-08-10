package com.floatingview.library.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.floatingview.library.helpers.getScreenSizeWithoutInsets

@Composable
fun AdaptiveSizeWrapper(
  updateLayoutParams: (contentSize: IntSize, screenSize: IntSize) -> Unit,
  content: @Composable () -> Unit
) {
  var contentSize by remember { mutableStateOf<IntSize?>(null) }
  val context = LocalContext.current
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current
  var screenSize by remember {
    mutableStateOf(
      getScreenSizeWithoutInsets(
        context,
        density,
        configuration
      )
    )
  }
  fun tryUpdateLayoutParams() {
    if (contentSize != null) {
      updateLayoutParams(contentSize!!, screenSize)
    }
  }
  LaunchedEffect(key1 = configuration) {
    val oldScreenSize = screenSize
    val newScreenSize = getScreenSizeWithoutInsets(context, density, configuration)
    screenSize = newScreenSize

    if (oldScreenSize.width != newScreenSize.width ||
      oldScreenSize.height != newScreenSize.height
    ) {
      tryUpdateLayoutParams()
    }
  }

  Box(
    modifier = Modifier
      .onSizeChanged {
        contentSize = it
        tryUpdateLayoutParams()
      }
      .wrapContentHeight()
  ) {
    content()
  }
}
