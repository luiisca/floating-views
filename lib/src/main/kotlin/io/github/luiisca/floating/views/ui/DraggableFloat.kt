package io.github.luiisca.floating.views.ui

import android.content.res.Configuration
import android.graphics.Point
import android.graphics.PointF
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition.Segment
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.luiisca.floating.views.CloseBehavior
import io.github.luiisca.floating.views.FloatingViewsConfig
import io.github.luiisca.floating.views.helpers.getScreenSizeWithoutInsets
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AnimationState {
  DRAGGING,
  SNAP_TO_EDGE,
  SNAP_TO_CLOSE,
}
enum class CloseAnimationState {
  DRAGGING,
  SNAP_TO_MAIN,
}
enum class InterruptMovState {
  DRAGGING,
  CLOSE_DRAGGING,
}

@Composable
fun DraggableFloat(
  modifier: Modifier = Modifier,
  windowManager: WindowManager,
  containerView: ComposeView,
  closeView: ComposeView,
  layoutParams: WindowManager.LayoutParams,
  closeLayoutParams: WindowManager.LayoutParams,
  config: FloatingViewsConfig,
  updateSize: (size: IntSize) -> Unit,
  onKey: (event: KeyEvent) -> Boolean,
  onDestroy: (() -> Unit)? = null,
  onTap: ((Offset) -> Unit)? = null,
  onDragStart: ((offset: Offset) -> Unit)? = null,
  onDrag: ((
    change: PointerInputChange,
    dragAmount: Offset,
    newPoint: Point,
    newAnimatedPoint: Point?) -> Unit)? = null,
  onDragEnd: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val focusRequester = remember {FocusRequester()}

  val context = LocalContext.current
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current
  val orientation by remember(configuration) { derivedStateOf {
    when (configuration.orientation) {
      Configuration.ORIENTATION_PORTRAIT -> "portrait"
      Configuration.ORIENTATION_LANDSCAPE -> "landscape"
      else -> "undefined"
    }
  }}
  var screenSize by remember {
    mutableStateOf(
      getScreenSizeWithoutInsets(
        context
      )
    )
  }
  var contentSize by remember { mutableStateOf(IntSize.Zero) }

  LaunchedEffect(key1 = Unit) {
    focusRequester.requestFocus()
  }

  Box(
    modifier = modifier
      .zIndex(10f)
      .layout { measurable, constraints ->
        val newConstraints = constraints.copy(maxWidth = Int.MAX_VALUE, maxHeight = Int.MAX_VALUE) // Remove the 880px cap
        val placeable = measurable.measure(newConstraints)
        layout(placeable.width, placeable.height) {
          placeable.placeRelative(0, 0)
        }
      }
      .onSizeChanged { size ->
        contentSize = size
        updateSize(size)
      }
      .systemGestureExclusion()
      .onKeyEvent { event ->
        onKey(event)
      }
      .focusRequester(focusRequester)
      .focusable()
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = { offset ->
            onTap?.let { it(offset) }
          }
        )
      }
      .let { mod ->
        var dragAmountState by remember { mutableStateOf<Offset?>(null) }

        var initialPoint by remember { mutableStateOf(Point(layoutParams.x, layoutParams.y))}
        var crrPoint by remember { mutableStateOf(Point(layoutParams.x, layoutParams.y)) }
        var accDrag by remember { mutableStateOf(PointF(0f,0f)) }
        var constrainedCrrPoint by remember { mutableStateOf(Point(layoutParams.x, layoutParams.y)) }
        var animPoint by remember { mutableStateOf(Point(layoutParams.x, layoutParams.y)) }

        var isCloseVisible by remember { mutableStateOf(false) }
        var initialClosePoint by remember { mutableStateOf<Point?>(null) }
        var closeContentSize by remember { mutableStateOf<IntSize?>(null) }
        var closeCrrPoint by remember { mutableStateOf<Point?>(null) }
        var closeAnimPoint by remember { mutableStateOf<Point?>(null) }

        var closeCenterPoint by remember { mutableStateOf<Point?>(null) }
        val mountThreshold = remember {
          with(density) {
            config.close.mountThresholdDp?.dp?.toPx() ?: config.close.mountThresholdPx ?: 1f.dp.toPx()
          }
        }
        val closingThreshold = remember {
          with(density) {
            config.close.closingThresholdDp?.dp?.toPx() ?: config.close.closingThresholdPx
            ?: 100f.dp.toPx()
          }
        }

        val transitionSpec: Segment<Point>.() -> FiniteAnimationSpec<Int>
        val closeTransitionSpec: Segment<Point>.() -> FiniteAnimationSpec<Int>
        var animationState by remember { mutableStateOf(AnimationState.DRAGGING) }
        var closeAnimationState by remember { mutableStateOf(CloseAnimationState.DRAGGING) }

        var withinCloseArea by remember { mutableStateOf(false) }
        var newChange by remember { mutableStateOf<PointerInputChange?>(null) }
        var newDragAmount by remember { mutableStateOf<Offset?>(null) }

        var interruptMovState by remember { mutableStateOf<InterruptMovState?>(null) }

        // snap main float to some edge when screen orientation or contentSize changes
        LaunchedEffect(orientation, contentSize) {
          val oldScreenSize = screenSize
          screenSize = getScreenSizeWithoutInsets(
            context
          )

          // snap to edge
          if (oldScreenSize != screenSize) {
            if (config.main.isSnapToEdgeEnabled
              && oldScreenSize.width != 0 && oldScreenSize.height != 0
            ) {
              val wasOnRightEdge = crrPoint.x + contentSize.width >= oldScreenSize.width
              val wasOnBottomEdge = crrPoint.y + contentSize.height >= oldScreenSize.height

              // adjust main float position to new screen size
              if (wasOnRightEdge) {
                crrPoint = Point(
                  screenSize.width - contentSize.width,
                  crrPoint.y.coerceIn(0, coerceInMax(screenSize.height - contentSize.height))
                )
              }
              if (wasOnBottomEdge) {
                crrPoint = Point(
                  crrPoint.x.coerceIn(0, coerceInMax(screenSize.width - contentSize.width)),
                  screenSize.height - contentSize.height
                )
              }

              if (config.enableAnimations) {
                animationState = AnimationState.SNAP_TO_EDGE
                animPoint = crrPoint
              } else {
                windowManager.updateViewLayout(containerView, layoutParams.apply {
                  x = crrPoint.x
                  y = crrPoint.y
                })
              }
              initialPoint = crrPoint
            }

            // adapt close position to new screenSize and constrainedCrrPoint
            if (config.close.enabled
              && isCloseVisible
              && contentSize != IntSize.Zero
              && dragAmountState != null
              && closeContentSize != null
            ) {
              // adapt initialClosePoint to new screen orientation
              initialClosePoint = getCloseInitialPoint(
                density = density,
                config,
                contentSize = closeContentSize!!,
                screenSize = screenSize
              )
              closeCrrPoint = initialClosePoint

              if (config.close.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT) {
                closeCrrPoint = followFloat(
                  isFollowerVisible = isCloseVisible,
                  followerInitialPoint = initialClosePoint!!,
                  followerCrrPoint = closeCrrPoint!!,
                  followerContentSize = closeContentSize!!,
                  targetCrrPoint = constrainedCrrPoint,
                  targetContentSize = contentSize,
                  dragAmount = dragAmountState!!,
                  screenSize = screenSize,
                  config,
                )
              }

              if (config.enableAnimations) {
                closeAnimationState = CloseAnimationState.DRAGGING
                closeAnimPoint = closeCrrPoint
              } else {
                windowManager.updateViewLayout(closeView, closeLayoutParams.apply {
                  x = closeCrrPoint!!.x
                  y = closeCrrPoint!!.y
                })
              }
            }
          }
        }

        if (config.enableAnimations) {
          val transition = updateTransition(targetState = animPoint, label = "point transition")
          transitionSpec = when (animationState) {
            AnimationState.SNAP_TO_EDGE -> config.main.snapToEdgeTransitionSpec
            AnimationState.SNAP_TO_CLOSE -> config.main.snapToCloseTransitionSpec
            AnimationState.DRAGGING -> config.main.draggingTransitionSpec
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
            windowManager.updateViewLayout(containerView, layoutParams.apply {
              x = animatedX
              y = animatedY
            })
            onDrag?.invoke(
              newChange!!,
              newDragAmount!!,
              constrainedCrrPoint,
              Point(
                animatedX,
                animatedY
              )
            )
          }

          if (config.close.enabled && closeAnimPoint != null) {
            val closeTransition =
              updateTransition(targetState = closeAnimPoint!!, label = "close point transition")
            closeTransitionSpec = when (closeAnimationState) {
              CloseAnimationState.DRAGGING -> config.close.draggingTransitionSpec
              CloseAnimationState.SNAP_TO_MAIN -> config.close.snapToMainTransitionSpec
            }

            val closeAnimatedX by closeTransition.animateInt(
              transitionSpec = { closeTransitionSpec() },
              label = "close x"
            ) { it.x }
            val closeAnimatedY by closeTransition.animateInt(
              transitionSpec = { closeTransitionSpec() },
              label = "close y"
            ) { it.y }

            LaunchedEffect(key1 = closeAnimatedX, key2 = closeAnimatedY) {
              windowManager.updateViewLayout(closeView, closeLayoutParams.apply {
                x = closeAnimatedX
                y = closeAnimatedY
              })
              closeView.visibility = View.VISIBLE
              isCloseVisible = true
            }
          }
        }

        mod.pointerInput(Unit) {
          val decay = splineBasedDecay<Float>(this)
          val velocityTracker = VelocityTracker()

          detectDragGestures(
            onDragStart = { offset ->
              onDragStart?.invoke(offset)
            },
            onDrag = { change, dragAmount ->
              dragAmountState = dragAmount
              accDrag = PointF(
                accDrag.x + dragAmount.x,
                accDrag.y + dragAmount.y,
              )
              crrPoint = Point(
                initialPoint.x + accDrag.x.roundToInt(),
                initialPoint.y + accDrag.y.roundToInt()
              )
              constrainedCrrPoint = Point(
                crrPoint.x.coerceIn(0, coerceInMax(screenSize.width - contentSize.width)),
                crrPoint.y.coerceIn(0, coerceInMax(screenSize.height - contentSize.height))
              )

              // MOUNT CLOSE LOGIC
              if (config.close.enabled) {
                if (
                  (abs(dragAmount.x) > mountThreshold || abs(dragAmount.y) > mountThreshold)
                  && !isCloseVisible
                  && (closeView.width > 0 || closeView.height > 0)
                ) {
                  if (closeContentSize == null) {
                    closeContentSize = IntSize(closeView.width, closeView.height)
                  }

                  initialClosePoint = getCloseInitialPoint(
                    density = density,
                    config,
                    contentSize = closeContentSize!!,
                    screenSize = screenSize
                  )

                  closeCrrPoint = initialClosePoint

                  if (config.close.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT) {
                    closeCrrPoint = followFloat(
                      isFollowerVisible = isCloseVisible,
                      followerInitialPoint = initialClosePoint!!,
                      followerCrrPoint = closeCrrPoint!!,
                      followerContentSize = closeContentSize!!,
                      targetCrrPoint = constrainedCrrPoint,
                      targetContentSize = contentSize,
                      dragAmount = dragAmount,
                      screenSize = screenSize,
                      config,
                    )
                  }

                  closeCenterPoint = Point(
                    closeCrrPoint!!.x + closeContentSize!!.width / 2,
                    closeCrrPoint!!.y + closeContentSize!!.height / 2
                  )

                  if (config.enableAnimations) {
                    closeAnimationState = CloseAnimationState.DRAGGING
                    closeAnimPoint = closeCrrPoint

                    // set close visible value immediately instead of waiting for animated value,
                    // since here close float does not move.
                    if (config.close.closeBehavior == CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT) {
                      closeView.visibility = View.VISIBLE
                      isCloseVisible = true
                    }
                  } else {
                    windowManager.updateViewLayout(closeView, closeLayoutParams.apply {
                      x = closeCrrPoint!!.x
                      y = closeCrrPoint!!.y
                    })
                    closeView.visibility = View.VISIBLE
                    isCloseVisible = true
                  }
                }
              }
              //

              // CLOSING LOGIC
              if (config.close.enabled) {
                if (closeCenterPoint != null) {
                  val wasWithinCloseArea = withinCloseArea

                  val centerPointF = PointF(
                    (crrPoint.x + contentSize.width / 2).toFloat(),
                    (crrPoint.y + contentSize.height / 2).toFloat()
                  )
                  withinCloseArea = isCloseVisible && isWithinCloseArea(
                    centerPointF,
                    PointF(
                      closeCenterPoint!!.x.toFloat(),
                      closeCenterPoint!!.y.toFloat(),
                    ),
                    closingThreshold + closeView.width / 2
                  )

                  when (config.close.closeBehavior) {
                    CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT -> {
                      if (withinCloseArea) {
                        interruptMovState = InterruptMovState.DRAGGING

                        if (!wasWithinCloseArea) {
                          val newPoint = Point(
                            (closeCenterPoint!!.x - contentSize.width / 2)
                              .coerceIn(0, coerceInMax(screenSize.width - contentSize.width)),
                            (closeCenterPoint!!.y - contentSize.height / 2)
                              .coerceIn(0, coerceInMax(screenSize.height - contentSize.height))
                          )

                          if (config.enableAnimations) {
                            animationState = AnimationState.SNAP_TO_CLOSE

                            animPoint = newPoint
                          } else {
                            windowManager.updateViewLayout(containerView, layoutParams.apply {
                              x = newPoint.x
                              y = newPoint.y
                            })
                          }
                        }
                      } else {
                        interruptMovState = null
                      }
                    }

                    CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT -> {
                      if (withinCloseArea) {
                        interruptMovState = InterruptMovState.CLOSE_DRAGGING

                        val newSnapPoint = Point(
                          (centerPointF.x.toInt() - closeView.width / 2)
                            .coerceIn(0, coerceInMax(screenSize.width - closeView.width)),
                          (centerPointF.y.toInt() - closeView.height / 2)
                            .coerceIn(
                              0,
                              coerceInMax(screenSize.height - closeView.height)
                            ),
                        )

                        if (config.enableAnimations) {
                          closeAnimationState = CloseAnimationState.SNAP_TO_MAIN
                          closeAnimPoint = newSnapPoint
                        } else {
                          windowManager.updateViewLayout(
                            closeView,
                            closeLayoutParams.apply {
                              x = newSnapPoint.x
                              y = newSnapPoint.y
                            })
                        }
                      } else {
                        if (wasWithinCloseArea) {
                          interruptMovState = null
                        }
                      }
                    }

                    null -> TODO()
                  }
                }
              }
              //

              // DRAGGING CLOSE FLOAT LOGIC
              if (config.close.enabled
                && isCloseVisible
                && interruptMovState != InterruptMovState.CLOSE_DRAGGING
                && config.close.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT
                && initialClosePoint != null
                && closeCrrPoint != null
                && closeContentSize != null
              ) {
                closeCrrPoint = followFloat(
                  isFollowerVisible = isCloseVisible,
                  followerInitialPoint = initialClosePoint!!,
                  followerCrrPoint = closeCrrPoint!!,
                  followerContentSize = closeContentSize!!,
                  targetCrrPoint = constrainedCrrPoint,
                  targetContentSize = contentSize,
                  dragAmount = dragAmount,
                  screenSize = screenSize,
                  config,
                )

                closeCenterPoint = Point(
                  closeCrrPoint!!.x + closeContentSize!!.width / 2,
                  closeCrrPoint!!.y + closeContentSize!!.height / 2
                )

                if (config.enableAnimations) {
                  closeAnimationState = CloseAnimationState.DRAGGING
                  closeAnimPoint = closeCrrPoint
                } else {
                  windowManager.updateViewLayout(closeView, closeLayoutParams.apply {
                    x = closeCrrPoint!!.x
                    y = closeCrrPoint!!.y
                  })
                }
              }
              //

              // DRAGGING MAIN FLOAT LOGIC
              if (interruptMovState != InterruptMovState.DRAGGING) {
                if (config.enableAnimations) {
                  animationState = AnimationState.DRAGGING
                  animPoint = crrPoint

                  newChange = change
                  newDragAmount = dragAmount
                } else {
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = crrPoint.x
                    y = crrPoint.y
                  })
                }
              }
              //

              // SNAP TO EDGE LOGIC
              if (config.enableAnimations && config.main.isSnapToEdgeEnabled) {
                velocityTracker.addPosition(change.uptimeMillis, change.position)
              }
              //

              onDrag?.invoke(
                change,
                dragAmount,
                constrainedCrrPoint,
                null,
              )
            },
            onDragEnd = {
              onDragEnd?.invoke()
              accDrag = PointF(0f,0f)

              if (config.close.enabled) {
                isCloseVisible = false
                closeView.visibility = View.INVISIBLE

                if (withinCloseArea) {
                  onDestroy?.let { it() }

                  return@detectDragGestures
                }
              }

              if (config.main.isSnapToEdgeEnabled) {
                var newPoint = Point(0, constrainedCrrPoint.y)

                if (config.enableAnimations) {
                  animationState = AnimationState.SNAP_TO_EDGE

                  val decayCenterX = decay.calculateTargetValue(
                    initialValue = (crrPoint.x + contentSize.width / 2).toFloat(),
                    initialVelocity = velocityTracker.calculateVelocity().x
                  )
                  if (decayCenterX >= screenSize.width * 0.5f) {
                    newPoint = Point(
                      screenSize.width - contentSize.width,
                      newPoint.y
                    )
                  }

                  animPoint = newPoint
                } else {
                  if (crrPoint.x + contentSize.width / 2 >= screenSize.width * 0.5) {
                    newPoint = Point(
                      screenSize.width - contentSize.width,
                      newPoint.y
                    )
                  }
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = newPoint.x
                    y = newPoint.y
                  })
                }

                initialPoint = newPoint

                constrainedCrrPoint = newPoint
                crrPoint = newPoint
              }
            }
          )
        }
      }
  ) {
    content()
  }
}

private fun followFloat(
  isFollowerVisible: Boolean = false,
  followerInitialPoint: Point,
  followerCrrPoint: Point,
  followerContentSize: IntSize,
  targetCrrPoint: Point,
  targetContentSize: IntSize,
  dragAmount: Offset,
  screenSize: IntSize,
  config: FloatingViewsConfig,
): Point {
  val followerInitialCenter = PointF(
    followerInitialPoint.x + followerContentSize.width / 2f,
    followerInitialPoint.y + followerContentSize.height / 2f
  )
  val targetCrrCenter = PointF(
    targetCrrPoint.x + targetContentSize.width / 2f,
    targetCrrPoint.y + targetContentSize.height / 2f
  )

  val centersDistance = Point(
    (followerInitialCenter.x - targetCrrCenter.x).toInt(),
    (followerInitialCenter.y - targetCrrCenter.y).toInt()
  )

  val pointModifierY = ((if (!isFollowerVisible) abs(centersDistance.y) else abs(dragAmount.y.toInt())) * config.close.followRate).toInt()
  val followerPoint = if (!isFollowerVisible) followerInitialPoint else followerCrrPoint

  val newX = (followerInitialPoint.x - (centersDistance.x * config.close.followRate)).toInt()

  val newY = if (dragAmount.y > 0f) {
    followerPoint.y - pointModifierY
  } else if (dragAmount.y < 0f) {
    if (!isFollowerVisible) {
      followerPoint.y - pointModifierY
    } else {
      followerPoint.y + pointModifierY
    }
  } else {
    followerPoint.y
  }

  return Point(
    newX,
    newY.coerceIn(0, coerceInMax((screenSize.height - followerContentSize.height - (followerContentSize.height * config.close.followRate)).toInt()))
  )
}

private fun getCloseInitialPoint(
  density: Density,
  config: FloatingViewsConfig,
  contentSize: IntSize,
  screenSize: IntSize,
): Point {
  val hasCustomPos = config.close.startPointDp != null || config.close.startPointPx != null
  val bottomPadding = with(density) {
      config.close.bottomPaddingDp?.dp?.toPx() ?: config.close.bottomPaddingPx ?: 16.dp.toPx()
  }

  val closeInitialPoint = if (hasCustomPos) with(density) {
    val customStartX = (config.close.startPointDp?.x?.dp?.roundToPx() ?: config.close.startPointPx?.x
    ?: 0).toInt()
    val customStartY = (config.close.startPointDp?.y?.dp?.roundToPx() ?: config.close.startPointPx?.y
    ?: 0).toInt()
    Point(
      customStartX.coerceIn(0, coerceInMax(screenSize.width - contentSize.width)),
      customStartY.coerceIn(0, coerceInMax(screenSize.height - contentSize.height))
    )
  } else {
    val defaultStartX = (screenSize.width / 2) - (contentSize.width / 2)
    val defaultStartY = screenSize.height - contentSize.height - bottomPadding.toInt()
    Point(
      defaultStartX.coerceAtLeast(0),
      defaultStartY.coerceAtLeast(0)
    )
  }

  return closeInitialPoint
}

private fun coerceInMax(max: Int): Int {
  return if (max < 0) {
    0
  } else {
    max
  }
}