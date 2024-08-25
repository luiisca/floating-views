package io.github.luiisca.floating.views.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import io.github.luiisca.floating.views.helpers.getScreenSizeWithoutInsets

@Composable
fun AdaptiveSizeWrapper(
  updateLayoutParams: (contentSize: IntSize, screenSize: IntSize) -> Unit,
  content: @Composable () -> Unit
) {
  var contentSize by remember { mutableStateOf<IntSize?>(null) }
  val context = LocalContext.current
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current
  val layoutDirection = LocalLayoutDirection.current
  val windowInsetsAsPaddingValues = WindowInsets.systemBars.asPaddingValues()
  var screenSize by remember {
    mutableStateOf(
      getScreenSizeWithoutInsets(
        context,
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
    val newScreenSize = getScreenSizeWithoutInsets(
      context,
    )
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
