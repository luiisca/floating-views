package com.floatingview.library.composables

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition.Segment
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.floatingview.library.CloseBehavior
import com.floatingview.library.CloseFloatyConfig
import com.floatingview.library.MainFloatyConfig
import com.floatingview.library.helpers.getScreenSizeWithoutInsets
import kotlin.math.abs

enum class AnimationState {
  DRAGGING,
  SNAP_TO_EDGE,
  SNAP_TO_CLOSE,
  SNAP_TO_MAIN,
}

@Composable
fun MainFloaty(
  windowManager: WindowManager,
  containerView: ComposeView,
  closeContainerView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  closeLayoutParams: WindowManager.LayoutParams,
  modifier: Modifier = Modifier,
  enableAnimations: Boolean? = true,
  config: MainFloatyConfig,
  closeConfig: CloseFloatyConfig,
  content: @Composable BoxScope.() -> Unit,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current

  var screenSize by remember { mutableStateOf(getScreenSizeWithoutInsets(context, density, configuration)) }
  var contentSize by remember { mutableStateOf(IntSize.Zero) }
  var crrPoint by remember { mutableStateOf(Point(layoutParams.x,layoutParams.y)) }
  var animPoint by remember { mutableStateOf(Point(layoutParams.x,layoutParams.y)) }

  var isCloseCompVisible by remember { mutableStateOf(false) }
  val mountThreshold = remember { with(density) { closeConfig.mountThresholdDp?.dp?.toPx() ?: closeConfig.mountThresholdPx ?: 0f}}
  var initialClosePoint by remember { mutableStateOf<Point?>(null) }
  val closingThreshold = remember {with(density) { closeConfig.closingThresholdDp?.dp?.toPx() ?: closeConfig.closingThresholdPx ?: 0f}}
  val bottomPadding = remember { with(density) { closeConfig.bottomPaddingDp?.dp?.toPx() ?: closeConfig.bottomPaddingPx ?: 0f}}

  var withinCloseArea by remember { mutableStateOf(false) }

  LaunchedEffect(key1 = configuration, key2 = contentSize) {
    val oldScreenSize = screenSize
    val newScreenSize = getScreenSizeWithoutInsets(context, density, configuration)
    screenSize = newScreenSize

    // snap to edge
    if (config.isSnapToEdgeEnabled == true && oldScreenSize.width != 0 && oldScreenSize.height != 0) {
      val wasOnRightEdge = crrPoint.x + contentSize.width >= oldScreenSize.width
      val wasOnBottomEdge = crrPoint.y + contentSize.width >= oldScreenSize.height

      var newPoint: Point? = null
      if (wasOnRightEdge) {
        newPoint = Point(
          newScreenSize.width - contentSize.width,
          crrPoint.y.coerceIn(0, newScreenSize.height - contentSize.height)
        )
      }
      if (wasOnBottomEdge) {
        newPoint = Point(
          crrPoint.x.coerceIn(0, newScreenSize.width - contentSize.width),
          newScreenSize.height - contentSize.height
        )
      }

      if (newPoint != null) {
        if (enableAnimations == true) {
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
        contentSize = size
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
        if (enableAnimations == true) {
          transitionSpec = when (animationState) {
            AnimationState.SNAP_TO_EDGE -> config.snapToEdgeTransitionSpec
            AnimationState.SNAP_TO_CLOSE -> closeConfig.snapToCloseTransitionSpec
            AnimationState.SNAP_TO_MAIN -> closeConfig.snapToMainTransitionSpec
            AnimationState.DRAGGING -> config.draggingTransitionSpec
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
            when (animationState) {
              AnimationState.SNAP_TO_MAIN -> {
//                windowManager.updateViewLayout(closeContainerView, closeLayoutParams.apply {
//                  x = animatedX
//                  y = animatedY
//                })
              }

              else -> {
                windowManager.updateViewLayout(containerView, layoutParams.apply {
                  x = animatedX
                  y = animatedY
                })
              }
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
              newX = (crrPoint.x + dragAmount.x.toInt()).coerceIn(
                0,
                screenSize.width - contentSize.width
              )
              newY = (crrPoint.y + dragAmount.y.toInt()).coerceIn(
                0,
                screenSize.height - contentSize.height
              )
              crrPoint = Point(newX, newY)

              // CLOSE LOGIC
              if (closeConfig.enabled == true) {
                if (!isCloseCompVisible && (dragAmount.x > mountThreshold || dragAmount.y > mountThreshold)) {
                  windowManager.addView(closeContainerView, closeLayoutParams)
                  isCloseCompVisible = true
                }

                if (initialClosePoint == null && isCloseCompVisible) {
                  // ensures correct initialClosePoint is saved as soon as it's available
                  // to be used by onDragEnd to snap close float back to initial position after being within close area
                  initialClosePoint = if (
                    (closeConfig.startPointDp != null || closeConfig.startPointPx != null) ||
                    (closeLayoutParams.x > 0 || closeLayoutParams.y > 0)
                  ) {
                    Point(
                      closeLayoutParams.x,
                      closeLayoutParams.y
                    )
                  } else {
                    null
                  }
                }
                // bottom back
              }

              val floatyCenterPointF = PointF(
                (newX + contentSize.width / 2).toFloat(),
                (newY + contentSize.height / 2).toFloat()
              )

              val closeCenterPointF =
                if (closeConfig.startPointDp == null && closeConfig.startPointPx == null) {
                  PointF(
                    (screenSize.width / 2).toFloat(),
                    (screenSize.height - bottomPadding.toInt() - closeContainerView.height / 2).toFloat()
                      .coerceIn(
                        (closeContainerView.height / 2).toFloat(),
                        (screenSize.height - closeContainerView.height / 2).toFloat()
                      )
                  )
                } else {
                  with(density) {
                    PointF(
                      ((closeConfig.startPointDp?.x?.dp?.toPx() ?: closeConfig.startPointPx?.x
                      ?: 0f) + closeContainerView.width / 2)
                        .coerceIn(
                          (closeContainerView.width / 2).toFloat(),
                          (screenSize.width - closeContainerView.width / 2).toFloat()
                        ),
                      ((closeConfig.startPointDp?.y?.dp?.toPx() ?: closeConfig.startPointPx?.y
                      ?: 0f) + closeContainerView.height / 2)
                        .coerceIn(
                          (closeContainerView.height / 2).toFloat(),
                          (screenSize.height - closeContainerView.height / 2).toFloat()
                        ),
                    )
                  }
                }

              val wasWithinCloseArea = withinCloseArea
              withinCloseArea = isCloseCompVisible && isWithinCloseArea(
                floatyCenterPointF,
                closeCenterPointF,
                closingThreshold + closeContainerView.width / 2
              )
              val isWithinCloseArea = withinCloseArea

              when (closeConfig.closeBehavior) {
                CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT -> {
                  if (isWithinCloseArea) {
                    if (!wasWithinCloseArea) {
                      val newPoint = Point(
                        (closeCenterPointF.x.toInt() - contentSize.width / 2)
                          .coerceIn(0, screenSize.width - contentSize.width),
                        (closeCenterPointF.y.toInt() - contentSize.height / 2)
                          .coerceIn(0, screenSize.height - contentSize.height)
                      )

                      if (enableAnimations == true) {
                        animationState = AnimationState.SNAP_TO_CLOSE

                        animPoint = newPoint
                      } else {
                        windowManager.updateViewLayout(containerView, layoutParams.apply {
                          x = newPoint.x
                          y = newPoint.y
                        })
                      }
                    }

                    return@detectDragGestures
                  }
                }

                CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT -> {
                  if (isWithinCloseArea) {
                    val newPoint = Point(
                      (floatyCenterPointF.x.toInt() - closeContainerView.width / 2)
                        .coerceIn(0, screenSize.width - closeContainerView.width),
                      (floatyCenterPointF.y.toInt() - closeContainerView.height / 2)
                        .coerceIn(0, screenSize.height - closeContainerView.height),
                    )

                    if (enableAnimations == true) {
                      animationState = AnimationState.SNAP_TO_MAIN
                      animPoint = newPoint
                    } else {
                      windowManager.updateViewLayout(closeContainerView, closeLayoutParams.apply {
                        x = newPoint.x
                        y = newPoint.y
                      })
                    }
                  } else {
                    if (wasWithinCloseArea && initialClosePoint != null && isCloseCompVisible) {
                      if (enableAnimations == true) {
                        animationState = AnimationState.SNAP_TO_MAIN
                        animPoint = initialClosePoint!!
                      } else {
                        windowManager.updateViewLayout(
                          closeContainerView,
                          closeLayoutParams.apply {
                            x = initialClosePoint!!.x
                            y = initialClosePoint!!.y
                          })
                      }
                    }
                  }
                }

                else -> {}
              }
              //

              // DRAGGING MAIN AND CLOSE FLOATS LOGIC
//              var newClosePoint: Point? = null
//              if (closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT) {
//                val newXDistanceToInitialClosePoint = abs(initialClosePoint!!.x - newX)
//                newClosePoint = Point(
//                  newX
//                )
//              }
              if (enableAnimations == true) {
                animationState = AnimationState.DRAGGING
                newChange = change
                newDragAmount = dragAmount
                animPoint = Point(newX, newY)
                // if close config is snap close to main then update extra closeAnimPoint to
                // ensure it gets animated
              } else {
                windowManager.updateViewLayout(containerView, layoutParams.apply {
                  x = newX
                  y = newY
                })
              }
              //

              // SNAP TO EDGE LOGIC
              if (enableAnimations == true && config.isSnapToEdgeEnabled == true) {
                velocityTracker.addPosition(change.uptimeMillis, change.position)
              }
              //

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
                  animationState = AnimationState.DRAGGING

                  windowManager.removeView(containerView)
                  if (initialClosePoint != null && !isCloseCompVisible) {
                    windowManager.updateViewLayout(
                      closeContainerView,
                      closeLayoutParams.apply {
                        x = initialClosePoint!!.x
                        y = initialClosePoint!!.y
                      })
                  }
                }
                // remove bottom back
              }

              if (config.isSnapToEdgeEnabled == true) {
                if (enableAnimations == true) {
                  val decayX = decay.calculateTargetValue(
                    initialValue = crrPoint.x.toFloat(),
                    initialVelocity = velocityTracker.calculateVelocity().x
                  )
                  val targetX = if (decayX > screenSize.width * 0.5f) {
                    screenSize.width - contentSize.width
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
                  val targetX = if (crrPoint.x > screenSize.width * 0.5) {
                    screenSize.width - contentSize.width
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