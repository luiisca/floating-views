package com.floatingview.library.composables

import MainFloatyConfig
import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import android.graphics.Point
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.core.Transition.Segment
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@Composable
fun MainFloatyNew(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  modifier: Modifier = Modifier,
  config: MainFloatyConfig,
  content: @Composable BoxScope.() -> Unit
) {
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current

  val (screenSize, contentSize) = rememberScreenAndContentSize(density, configuration)
  var point by remember { mutableStateOf(Point(0, 0)) }



  Box(
    modifier = modifier
      .onSizeChanged { size ->
        contentSize.value = size

        layoutParams.width = size.width
        layoutParams.height = size.height
        windowManager.updateViewLayout(containerView, layoutParams)
      }
      .let { mod ->
        if (config.isDraggingAnimationEnabled == true) {
          mod.animatableDrag(
            windowManager,
            containerView,
            layoutParams,
            point,
            screenSize,
            contentSize,
            config
          ) { newPoint ->
            point = newPoint
          }
        } else {
          Log.d("✅MainFloaty", "direct drag about to run!H")
          mod.directDrag(
            windowManager,
            containerView,
            layoutParams,
            screenSize,
            contentSize,
            config
          ) { newPoint ->
            point = newPoint
          }
        }
      }
//      .let { mod ->
//        if (config.isSnapToEdgeEnabled == true) {
//          if (config.isSnapToEdgeAnimationEnabled == true) {
//            mod.animatableEdgeSnap(
//              windowManager,
//              containerView,
//              layoutParams,
//              point,
//              screenSize,
//              contentSize,
//              config
//            ) { newPoint ->
//              point = newPoint
//            }
//          } else {
//            mod.directEdgeSnap(
//              windowManager,
//              containerView,
//              layoutParams,
//              point,
//              screenSize,
//              contentSize,
//            ) { newPoint ->
//              point = newPoint
//            }
//          }
//        } else {
//          mod
//        }
//      }
      .pointerInput(Unit) {
        detectTapGestures { offset ->
          config.onTap?.invoke(offset)
        }
      }
      .coercePoint(
        windowManager,
        containerView,
        layoutParams,
        point,
        configuration,
        density,
        contentSize,
        screenSize,
        config
      )
  ) {
    content()
  }
}

@Composable
private fun rememberScreenAndContentSize(
  density: Density,
  configuration: Configuration
): Pair<MutableState<IntSize>, MutableState<IntSize>> {
  val screenSize = remember {
    mutableStateOf(
      IntSize(
        with(density) { configuration.screenWidthDp.dp.roundToPx()},
        with(density) { configuration.screenHeightDp.dp.roundToPx() }
      )
    )
  }
  val contentSize = remember { mutableStateOf(IntSize.Zero) }
  return screenSize to contentSize
}

private fun Modifier.animatableDrag(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  point: Point,
  screenSize: State<IntSize>,
  contentSize: State<IntSize>,
  config: MainFloatyConfig,
  onPointChange: (Point) -> Unit
) = composed {
  Log.d("✅animatableDrag", "animatableDrag!")
  var newPoint by remember { mutableStateOf(Point(point)) }
  val transition = updateTransition(newPoint, label = "drag")
  val defaultSpring: @Composable Segment<Point>.() -> FiniteAnimationSpec<Int> = remember {{
    spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessHigh
    )
  }}
  val animatedX by transition.animateInt(
    transitionSpec = config.draggingTransitionSpec ?:  defaultSpring,
    label = "x"
  ) { it.x }
  val animatedY by transition.animateInt(
    transitionSpec = config.draggingTransitionSpec ?:  defaultSpring,
    label = "y"
  ) { it.y }

  var newChange by remember { mutableStateOf<PointerInputChange?>(null) }
  var newDragAmount by remember {mutableStateOf<Offset?>(null)}
  var newX by remember { mutableIntStateOf(0) }
  var newY by remember { mutableIntStateOf(0) }

  LaunchedEffect(key1 = animatedX, key2 = animatedY) {
    windowManager.updateViewLayout(containerView, layoutParams.apply {
      x = animatedX
      y = animatedY
    })
    config.onDrag?.invoke(
      newChange!!,
      newDragAmount!!,
      newX,
      newY,
      animatedX,
      animatedY,
    )
  }

  return@composed this.pointerInput(Unit) {
    this.detectDragGestures(
      onDragStart = { config.onDragStart?.invoke(it) },
      onDrag = { change, dragAmount ->
        newChange = change
        newDragAmount = dragAmount
        newX = (point.x + dragAmount.x.toInt()).coerceIn(0, screenSize.value.width - contentSize.value.width)
        newY = (point.y + dragAmount.y.toInt()).coerceIn(0, screenSize.value.height - contentSize.value.height)

        newPoint = Point(newX, newY)
        onPointChange(Point(newX, newY))
      },
      onDragEnd = { config.onDragEnd?.invoke() }
    )
  }
}

private fun Modifier.directDrag(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  screenSize: State<IntSize>,
  contentSize: State<IntSize>,
  config: MainFloatyConfig,
  onPointChange: (Point) -> Unit
) = this.pointerInput(Unit) {
  Log.d("✅directDrag!", "direct drag run!")
  detectDragGestures(
    onDragStart = {
      Log.d("✅directDrag! - onDragStart", "drag Start")
      config.onDragStart?.invoke(it)
    },
    onDrag = { change, dragAmount ->
      Log.d("✅directDrag! - onDrag", "onDrag")
      val newX = (layoutParams.x + dragAmount.x.toInt()).coerceIn(0, screenSize.value.width - contentSize.value.width)
      val newY = (layoutParams.y + dragAmount.y.toInt()).coerceIn(0, screenSize.value.height - contentSize.value.height)
      windowManager.updateViewLayout(containerView, layoutParams.apply {
        x = newX
        y = newY
      })

      onPointChange(Point(newX, newY))
      config.onDrag?.invoke(
        change,
        dragAmount,
        newX,
        newY,
        null,
        null
      )
    },
    onDragEnd = { config.onDragEnd?.invoke() }
  )
}

private fun Modifier.animatableEdgeSnap(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  point: Point,
  screenSize: State<IntSize>,
  contentSize: State<IntSize>,
  config: MainFloatyConfig,
  onPointChange: (Point) -> Unit,
) = composed {
  var newPoint by remember { mutableStateOf(Point(point)) }
  val transition = updateTransition(newPoint, label = "drag")
  val defaultSpring: @Composable Segment<Point>.() -> FiniteAnimationSpec<Int> = remember {{
    spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    )
  }}
  val animatedX by transition.animateInt(
    transitionSpec = config.snapToEdgeTransitionSpec ?:  defaultSpring,
    label = "x"
  ) { it.x }
  val animatedY by transition.animateInt(
    transitionSpec = config.snapToEdgeTransitionSpec ?:  defaultSpring,
    label = "y"
  ) { it.y }

  LaunchedEffect(key1 = animatedX, key2 = animatedY) {
    windowManager.updateViewLayout(containerView, layoutParams.apply {
      x = animatedX
      y = animatedY
    })
  }

  val decay = rememberSplineBasedDecay<Float>()
  val velocityTracker = remember { VelocityTracker() }

  this.pointerInput(Unit) {
    detectDragGestures(
      onDragEnd = {
        val velocity = velocityTracker.calculateVelocity()
        val targetX = decay.calculateTargetValue(point.x.toFloat(), velocity.x)
        val finalX = if (targetX > screenSize.value.width * 0.5f) {
          screenSize.value.width - contentSize.value.width
        } else {
          0
        }
        newPoint = Point(finalX, point.y)
        onPointChange(Point(finalX, point.y))
      },
      onDrag = { change, _ ->
        velocityTracker.addPosition(change.uptimeMillis, change.position)
      }
    )
  }
}

private fun Modifier.directEdgeSnap(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  point: Point,
  screenSize: MutableState<IntSize>,
  contentSize: MutableState<IntSize>,
  onPointChange: (Point) -> Unit
) = this.pointerInput(Unit) {
  detectDragGestures(
    onDragEnd = {
      val finalX = if (point.x >= screenSize.value.width * 0.5) {
        screenSize.value.width - contentSize.value.width
      } else {
        0
      }
      windowManager.updateViewLayout(containerView, layoutParams.apply {
        x = finalX
        y = point.y
      })
      onPointChange(Point(finalX, point.y))
    },
    onDrag = { change, _ ->
    }
  )
}

private fun Modifier.coercePoint(
  windowManager: WindowManager,
  containerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  point: Point,
  configuration: Configuration,
  density: Density,
  contentSize: MutableState<IntSize>,
  screenSize: MutableState<IntSize>,
  config: MainFloatyConfig,
) = composed {
  var newPoint by remember { mutableStateOf(Point(point)) }
  val transition = updateTransition(newPoint, label = "drag")
  val defaultSpring: @Composable Segment<Point>.() -> FiniteAnimationSpec<Int> = remember {{
    spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessHigh
    )
  }}
  val animatedX by transition.animateInt(
    transitionSpec = config.snapToEdgeTransitionSpec ?:  defaultSpring,
    label = "x"
  ) { it.x }
  val animatedY by transition.animateInt(
    transitionSpec = config.snapToEdgeTransitionSpec ?:  defaultSpring,
    label = "y"
  ) { it.y }

  LaunchedEffect(key1 = animatedX, key2 = animatedY) {
    Log.d("✅coercePoint", "coerced!")
    windowManager.updateViewLayout(containerView, layoutParams.apply {
      x = animatedX
      y = animatedY
    })
  }

  LaunchedEffect(key1 = configuration, key2 = contentSize) {
    val oldScreenWidth = screenSize.value.width
    val oldScreenHeight = screenSize.value.height
    val newScreenWidth = with(density) {configuration.screenWidthDp.dp.roundToPx()}
    val newScreenHeight = with(density) {configuration.screenHeightDp.dp.roundToPx()}

    screenSize.value = IntSize(
      newScreenWidth,
      newScreenHeight
    )

    if (config.isSnapToEdgeEnabled == true && oldScreenWidth != 0 && oldScreenHeight != 0) {
      val wasOnRightEdge = point.x + contentSize.value.width >= oldScreenWidth
      if (wasOnRightEdge) {
        newPoint = Point(
          newScreenWidth - contentSize.value.width,
          point.y.coerceIn(0, newScreenHeight - contentSize.value.height)
        )
      }
    }

    newPoint = Point(
      point.x.coerceIn(0, newScreenWidth - contentSize.value.width),
      point.y.coerceIn(0, newScreenHeight - contentSize.value.height),
    )
  }

  this
}
