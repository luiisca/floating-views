package com.floatingview.library.composables

import android.graphics.Point
import android.graphics.PointF
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
import com.floatingview.library.CloseFloatyConfig
import com.floatingview.library.MainFloatyConfig
import com.floatingview.library.helpers.toPx

enum class AnimationState {
  DRAGGING,
  SNAP_TO_EDGE,
  SNAP_TO_CLOSE
}

@Composable
fun MainFloaty(
  windowManager: WindowManager,
  containerView: ComposeView,
  closeContainerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  closeLayoutParams: WindowManager.LayoutParams,
  modifier: Modifier = Modifier,
  config: MainFloatyConfig,
  closeConfig: CloseFloatyConfig,
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
  var crrPoint by remember { mutableStateOf(Point(layoutParams.x,layoutParams.y)) }
  var animPoint by remember { mutableStateOf(Point(layoutParams.x,layoutParams.y)) }

  var isCloseCompVisible by remember { mutableStateOf(false) }
  val openThreshold = remember { with(density) { closeConfig.openThresholdDp?.dp?.toPx() ?: closeConfig.openThresholdPx ?: 0f}}
  val closeThreshold = remember {with(density) { closeConfig.closeThresholdDp?.dp?.toPx() ?: closeConfig.closeThresholdPx ?: 0f}}
  var closeCenterPointF = remember { with(density) {PointF(
    (closeConfig.startPointDp?.x?.dp?.toPx() ?: closeConfig.startPointPx?.x ?: 0f) + closeContainerView.width / 2,
    (closeConfig.startPointDp?.y?.dp?.toPx() ?: closeConfig.startPointPx?.y ?: 0f) + closeContainerView.height / 2,
  )}}
  var withinCloseArea by remember { mutableStateOf(false) }

  // coerces `point` within updated screen boundaries
  LaunchedEffect(key1 = configuration, key2 = contentSize) {
    val oldScreenSize = screenSize.value
    val newScreenWidth = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val newScreenHeight = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    screenSize.value = IntSize(
      newScreenWidth,
      newScreenHeight
    )
    if (closeConfig.startPointDp == null && closeConfig.startPointPx == null) {
      val botPaddingPx = with(density) { 16.dp.toPx() }
      closeCenterPointF = PointF(
        (screenSize.value.width / 2).toFloat(),
        screenSize.value.height - botPaddingPx - closeContainerView.height / 2,
      )
    }

    if (config.isSnapToEdgeEnabled == true && oldScreenSize.width != 0 && oldScreenSize.height != 0) {
      val wasOnRightEdge = crrPoint.x + contentSize.value.width >= oldScreenSize.width
      val wasOnBottomEdge = crrPoint.y + contentSize.value.width >= oldScreenSize.height
      if (wasOnRightEdge) {
        val newPoint = Point(
          newScreenWidth - contentSize.value.width,
          crrPoint.y.coerceIn(0, newScreenHeight - contentSize.value.height)
        )

        if (config.enableAnimations == true) {
          animPoint = newPoint
        } else {
          windowManager.updateViewLayout(containerView, layoutParams.apply {
            x = newPoint.x
            y = newPoint.y
          })
        }
      }
      if (wasOnBottomEdge) {
        val newPoint = Point(
          crrPoint.x.coerceIn(0, newScreenWidth - contentSize.value.width),
          newScreenHeight - contentSize.value.height
        )

        if (config.enableAnimations == true) {
          animPoint = newPoint
        } else {
          windowManager.updateViewLayout(containerView, layoutParams.apply {
            x = newPoint.x
            y = newPoint.y
          })
        }
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
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = { offset ->
            config.onTap?.let { it(offset) }
          }
        )
      }
      .let { mod ->
        val transitionSpec: Segment<Point>.() -> FiniteAnimationSpec<Int>
        var animationState by remember { mutableStateOf(AnimationState.DRAGGING) }

        var newChange by remember { mutableStateOf<PointerInputChange?>(null) }
        var newDragAmount by remember { mutableStateOf<Offset?>(null) }
        var newX by remember { mutableIntStateOf(0) }
        var newY by remember { mutableIntStateOf(0) }

        val transition = updateTransition(targetState = animPoint, label = "point transition")
        if (config.enableAnimations == true) {
          transitionSpec = when(animationState) {
            AnimationState.SNAP_TO_EDGE -> config.snapToEdgeTransitionSpec ?: {
              spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
              )
            }
            AnimationState.SNAP_TO_CLOSE -> closeConfig.snapToCloseTransitionSpec ?: {
              spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessLow
              )
            }
            AnimationState.DRAGGING -> config.draggingTransitionSpec ?: {
              spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
              )
            }
          }

          val animatedX by transition.animateInt(
            transitionSpec = { transitionSpec() },
            label = "x"
          ) { it.x }
          val animatedY by transition.animateInt(
            transitionSpec = { transitionSpec() },
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
              if (closeConfig.enabled == true &&
                !isCloseCompVisible &&
                (dragAmount.x > openThreshold || dragAmount.y > openThreshold)
              ) {
                windowManager.addView(closeContainerView, closeLayoutParams)
                isCloseCompVisible = true
                // bottom back
              }

              newX = (crrPoint.x + dragAmount.x.toInt()).coerceIn(
                0,
                screenSize.value.width - contentSize.value.width
              )
              newY = (crrPoint.y + dragAmount.y.toInt()).coerceIn(
                0,
                screenSize.value.height - contentSize.value.height
              )
              crrPoint = Point(newX, newY)

              // close floaty calculations
              val floatyCenterPointF = PointF(
                (newX + contentSize.value.width / 2).toFloat(),
                (newY + contentSize.value.height / 2).toFloat()
              )

              val wasWithinCloseArea = withinCloseArea
              withinCloseArea = isCloseCompVisible && isWithinCloseArea(
                          floatyCenterPointF,
                          closeCenterPointF,
                          closeThreshold + closeContainerView.width / 2
                      )
              val isWithinCloseArea = withinCloseArea
              //

              if (config.enableAnimations == true) {
                animationState = AnimationState.DRAGGING

                newChange = change
                newDragAmount = dragAmount

                if (isWithinCloseArea && closeConfig.isSnapToCloseEnabled == true) {
                  if (!wasWithinCloseArea) {
                    withinCloseArea = true
                    animationState = AnimationState.SNAP_TO_CLOSE

                    val newPoint = Point(
                      (closeCenterPointF.x.toInt() - contentSize.value.width / 2)
                        .coerceIn(0, screenSize.value.width - contentSize.value.width),
                      (closeCenterPointF.y.toInt() - contentSize.value.height / 2)
                        .coerceIn(0, screenSize.value.height - contentSize.value.height)
                    )
                    animPoint = newPoint
                  }
                } else {
                  animationState = AnimationState.DRAGGING

                  animPoint = Point(newX, newY)
                }

                if (config.isSnapToEdgeEnabled == true) {
                  velocityTracker.addPosition(change.uptimeMillis, change.position)
                }
              } else {
                if (isWithinCloseArea && closeConfig.isSnapToCloseEnabled == true) {
                  val newLayoutX = (closeCenterPointF.x.toInt() - contentSize.value.width / 2)
                      .coerceIn(0, screenSize.value.width - contentSize.value.width)
                  val newLayoutY = (closeCenterPointF.y.toInt() - contentSize.value.height / 2)
                    .coerceIn(0, screenSize.value.height - contentSize.value.height)
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = newLayoutX
                    y = newLayoutY
                  })
                } else {
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = newX
                    y = newY
                  })
                }
              }

              config.onDrag?.invoke(
                change,
                dragAmount,
                newX,
                newY,
                null,
                null
              )
            },
            onDragEnd = {
              if (closeConfig.enabled == true && isCloseCompVisible) {
                windowManager.removeView(closeContainerView)
                isCloseCompVisible = false

                if (withinCloseArea) {
                  windowManager.removeView(containerView)
                  animationState = AnimationState.DRAGGING
                }
                // remove bottom back
              }

              if (config.isSnapToEdgeEnabled == true) {
                if (config.enableAnimations == true) {
                  val decayX = decay.calculateTargetValue(
                    initialValue = crrPoint.x.toFloat(),
                    initialVelocity = velocityTracker.calculateVelocity().x
                  )
                  val targetX = if (decayX > screenSize.value.width * 0.5f) {
                    screenSize.value.width - contentSize.value.width
                  } else {
                    0
                  }

                  val newPoint = Point(
                    targetX,
                    crrPoint.y
                  )
                  animPoint = newPoint
                  crrPoint = newPoint

                  animationState = AnimationState.SNAP_TO_EDGE
                } else {
                  val targetX = if (crrPoint.x > screenSize.value.width * 0.5) {
                    screenSize.value.width - contentSize.value.width
                  } else {
                    0
                  }
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = targetX
                    y = layoutParams.y
                  })
                  crrPoint = Point(
                    targetX,
                    layoutParams.y
                  )
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