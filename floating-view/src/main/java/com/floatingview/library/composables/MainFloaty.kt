package com.floatingview.library.composables

import MainFloatyConfig
import android.graphics.Point
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.Transition.Segment
import androidx.compose.animation.core.animate
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
  var animateToEdge by remember { mutableStateOf(false) }

  val pointTransition = updateTransition(targetState = point, label = "point transition")
  val defaultDraggingSpring: @Composable Segment<Point>.() -> FiniteAnimationSpec<Int> = remember {{
    spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessHigh
    )
  }}
  val defaultToEdgeSpring: @Composable Segment<Point>.() -> FiniteAnimationSpec<Int> = remember {{
    spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    )
  }}
  val customSpring = if (animateToEdge) {
    if (config.snapToEdgeTransitionSpec != null) {
      config.snapToEdgeTransitionSpec!!
    } else {
      defaultToEdgeSpring
    }
  } else {
    if (config.draggingTransitionSpec !== null) {
      config.draggingTransitionSpec!!
    } else {
      defaultDraggingSpring
    }
  }

  val newLayoutPointX by pointTransition.animateInt(
    label = "point x transition",
    transitionSpec = customSpring
  ) { state: Point -> state.x }
  val newLayoutPointY by pointTransition.animateInt(
    label = "point y transition",
    transitionSpec = customSpring
  ) {state: Point -> state.y }

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

  LaunchedEffect(key1 = newLayoutPointX, key2 = newLayoutPointY) {
    windowManager.updateViewLayout(containerView, layoutParams.apply {
      x = newLayoutPointX
      y = newLayoutPointY
    })
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
        var newDragAmount by remember {mutableStateOf<Offset?>(null)}
        var newX by remember { mutableIntStateOf(0) }
        var newY by remember { mutableIntStateOf(0) }

        if (
          config.isDraggingAnimationEnabled == true ||
          (config.isSnapToEdgeEnabled == true && config.isSnapToEdgeAnimationEnabled == true)
          ) {
          val transition = updateTransition(targetState = point, label = "point transition")
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
            if (config.isDraggingAnimationEnabled == true) {
              config.onDrag?.invoke(
                newChange!!,
                newDragAmount!!,
                newX,
                newY,
                animatedX,
                animatedY,
              )
            }
            if ((config.isDraggingAnimationEnabled == true && !animateToEdge) ||
              (animateToEdge && config.isSnapToEdgeEnabled == true && config.isSnapToEdgeAnimationEnabled == true)
              ) {
              windowManager.updateViewLayout(containerView, layoutParams.apply {
                x = animatedX
                y = animatedY
              })
            }
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

              Log.d("✅onDrag 1", "onDrag 1")
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

              point = Point(newX, newY)

              if (config.isDraggingAnimationEnabled != true) {
                windowManager.updateViewLayout(containerView, layoutParams.apply {
                  x = newX
                  y = newY
                })
                config.onDrag?.invoke(
                  change,
                  dragAmount,
                  newX,
                  newY,
                  null,
                  null
                )
              }

              if (config.isSnapToEdgeEnabled == true && config.isSnapToEdgeAnimationEnabled == true) {
                velocityTracker.addPosition(change.uptimeMillis, change.position)
              }
            },
            onDragEnd = {
              if (config.isSnapToEdgeEnabled == true) {
                if (config.isSnapToEdgeAnimationEnabled == true) {
                  animateToEdge = true

                  val decayX = decay.calculateTargetValue(
                    initialValue = point.x.toFloat(),
                    initialVelocity = velocityTracker.calculateVelocity().x
                  )
                  val targetX = if (decayX > screenSize.value.width * 0.5) {
                    screenSize.value.width - size.width
                  } else {
                    0
                  }
                  point = Point(
                    targetX.coerceIn(0, screenSize.value.width  - size.width),
                    point.y.coerceIn(0, screenSize.value.height - size.height)
                  )
                } else {
                  point = Point(
                    0,
                    layoutParams.y
                  )
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = 0
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