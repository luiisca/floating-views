package com.floatingview.library.composables

import MainFloatyConfig
import android.graphics.Point
import android.view.WindowManager
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition.Segment
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun MainFloaty(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  modifier: Modifier = Modifier,
  config: MainFloatyConfig,
  content: @Composable BoxScope.() -> Unit,
) {
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current

  val screenSize = remember {
    mutableStateOf(
      IntSize(
        with(density) { configuration.screenWidthDp.dp.roundToPx()},
        with(density) { configuration.screenHeightDp.dp.roundToPx() }
      )
    )
  }
  val contentSize = remember { mutableStateOf(IntSize.Zero) }
  var point by remember { mutableStateOf(Point(layoutParams.x,layoutParams.y)) }

  // coerces `point` within updated screen boundaries
  LaunchedEffect(key1 = configuration, key2 = contentSize) {
    val oldScreenSize = screenSize.value
    val newScreenWidth = with(density) {configuration.screenWidthDp.dp.roundToPx()}
    val newScreenHeight = with(density) {configuration.screenHeightDp.dp.roundToPx()}
    screenSize.value = IntSize(
      newScreenWidth,
      newScreenHeight
    )

    if (config.isSnapToEdgeEnabled == true && oldScreenSize.width != 0 && oldScreenSize.height != 0) {
      val wasOnRightEdge = point.x + contentSize.value.width >= oldScreenSize.width
      if (wasOnRightEdge) {
        point = Point(
          newScreenWidth - contentSize.value.width,
          point.y.coerceIn(0, newScreenHeight - contentSize.value.height)
        )
      }
    }

    point = Point(
      point.x.coerceIn(0, newScreenWidth - contentSize.value.width),
      point.y.coerceIn(0, newScreenHeight - contentSize.value.height),
    )
  }

  Box(
    modifier = modifier
      .onSizeChanged { size ->
        contentSize.value = size
        layoutParams.width = size.width
        layoutParams.height = size.height
        windowManager.updateViewLayout(containerView, layoutParams)
      }
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = { offset ->
            config.onTap?.let { it(offset) }
          }
        )
      }
      .let { mod ->
        val transitionSpec: (@Composable Segment<Point>.() -> FiniteAnimationSpec<Int>)?
        var animateToEdge by remember { mutableStateOf(false) }

        var newChange by remember { mutableStateOf<PointerInputChange?>(null) }
        var newDragAmount by remember { mutableStateOf<Offset?>(null) }
        var newX by remember { mutableIntStateOf(0) }
        var newY by remember { mutableIntStateOf(0) }

        val transition = updateTransition(targetState = point, label = "point transition")
        if (config.enableAnimations == true) {
          if (animateToEdge) {
            transitionSpec = config.snapToEdgeTransitionSpec ?: {
              spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
              )
            }
          } else {
            transitionSpec = config.draggingTransitionSpec ?: {
              spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
              )
            }
          }

          val animatedX by transition.animateInt(
            transitionSpec = transitionSpec,
            label = "x"
          ) { it.x }
          val animatedY by transition.animateInt(
            transitionSpec = transitionSpec,
            label = "y"
          ) { it.y }

          LaunchedEffect(key1 = animatedX, key2 = animatedY) {
            config.onDrag?.invoke(
              newChange!!,
              newDragAmount!!,
              newX,
              newY,
              animatedX,
              animatedY,
            )
            windowManager.updateViewLayout(containerView, layoutParams.apply {
              x = animatedX
              y = animatedY
            })
          }
        }

        mod.pointerInput(Unit) {
          val decay = splineBasedDecay<Float>(this)
          val velocityTracker = VelocityTracker()

          detectDragGestures(
            onDragStart = { offset ->
              config.onDragStart?.let { it(offset) }
            },
            onDrag = { change, dragAmount ->
              animateToEdge = false

              if (config.enableAnimations == true) {
                newChange = change
                newDragAmount = dragAmount
                newX = (point.x + dragAmount.x.toInt()).coerceIn(
                  0,
                  screenSize.value.width - contentSize.value.width
                )
                newY = (point.y + dragAmount.y.toInt()).coerceIn(
                  0,
                  screenSize.value.height - contentSize.value.height
                )
                val newPoint = Point(newX, newY)
                point = newPoint
                if (config.isSnapToEdgeEnabled == true) {
                  velocityTracker.addPosition(change.uptimeMillis, change.position)
                }
              } else {
                val newLayoutX = (point.x + dragAmount.x.toInt()).coerceIn(
                  0,
                  screenSize.value.width - contentSize.value.width
                )
                val newLayoutY = (point.y + dragAmount.y.toInt()).coerceIn(
                  0,
                  screenSize.value.height - contentSize.value.height
                )
                windowManager.updateViewLayout(containerView, layoutParams.apply {
                  x = newLayoutX
                  y = newLayoutY
                })

                point = Point(newLayoutX, newLayoutY)

                config.onDrag?.invoke(
                  change,
                  dragAmount,
                  newX,
                  newY,
                  null,
                  null
                )
              }

            },
            onDragEnd = {
              if (config.isSnapToEdgeEnabled == true) {
                if (config.enableAnimations == true) {
                  val decayX = decay.calculateTargetValue(
                    initialValue = point.x.toFloat(),
                    initialVelocity = velocityTracker.calculateVelocity().x
                  )
                  val targetX = if (decayX > screenSize.value.width * 0.5f) {
                    screenSize.value.width - contentSize.value.width
                  } else {
                    0
                  }

                  point = Point(
                    targetX.coerceIn(0, screenSize.value.width - contentSize.value.width),
                    point.y.coerceIn(0, screenSize.value.height - contentSize.value.height)
                  )
                  animateToEdge = true
                } else {
                  val targetX = if (point.x > screenSize.value.width * 0.5) {
                    screenSize.value.width - contentSize.value.width
                  } else {
                    0
                  }
                  point = Point(
                    targetX,
                    layoutParams.y
                  )
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = targetX
                    y = layoutParams.y
                  })
                }
              }
              config.onDragEnd?.let { it() }
            }
          )
        }
      }
  ) {
    content()
  }
}