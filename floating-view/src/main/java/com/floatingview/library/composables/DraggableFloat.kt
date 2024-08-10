package com.floatingview.library.composables

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.floatingview.library.CloseBehavior
import com.floatingview.library.CloseFloatyConfig
import com.floatingview.library.DraggableType
import com.floatingview.library.ExpandedFloatyConfig
import com.floatingview.library.MainFloatyConfig
import com.floatingview.library.helpers.getScreenSizeWithoutInsets
import kotlin.math.abs

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
  type: DraggableType,
  windowManager: WindowManager,
  containerView: ComposeView,
  closeContainerView: ComposeView,
  closeLayoutParams: WindowManager.LayoutParams,
  layoutParams: WindowManager.LayoutParams,
  modifier: Modifier = Modifier,
  enableAnimations: Boolean? = true,
  mainConfig: MainFloatyConfig,
  expandedConfig: ExpandedFloatyConfig,
  closeConfig: CloseFloatyConfig,
  openExpandedView: (() -> Unit)? = null,
  onClose: ((openMainAfter: Boolean?) -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val focusRequester = remember {FocusRequester()}

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

  var contentSize by remember { mutableStateOf(IntSize.Zero) }

  LaunchedEffect(key1 = type) {
    if (type == DraggableType.EXPANDED) {
      focusRequester.requestFocus()
    }
  }

  Box(
    propagateMinConstraints = true,
    modifier = modifier
      // TODO: test removing it
      .zIndex(10f)
      .onSizeChanged { size ->
        contentSize = size
        windowManager.updateViewLayout(containerView, layoutParams)
      }
      .wrapContentWidth(Alignment.Start, unbounded = true)
      .wrapContentHeight(Alignment.Top, unbounded = true)
      .systemGestureExclusion()
      .onKeyEvent { event ->
        if (event.key == Key.Back) {
          onClose?.let { it(true) }

          true
        } else {
          false
        }
      }
      .focusRequester(focusRequester)
      .focusable()
      .pointerInput(Unit) {
        detectTapGestures(
          onTap = { offset ->
            when (type) {
              DraggableType.MAIN -> {
                if (expandedConfig.enabled) {
                  openExpandedView?.let { it() }
                }
                mainConfig.onTap?.let { it(offset) }
              }

              DraggableType.EXPANDED -> {
                expandedConfig.onTap?.let { it(offset) }
              }
            }
          }
        )
      }
      .let { mod ->
        var dragAmountState by remember { mutableStateOf<Offset?>(null) }

        var crrPoint by remember { mutableStateOf(Point(layoutParams.x, layoutParams.y)) }
        var animPoint by remember { mutableStateOf(Point(layoutParams.x, layoutParams.y)) }

        var isCloseMounted by remember { mutableStateOf(false) }
        var isCloseVisible by remember { mutableStateOf(false) }
        var initialClosePoint by remember { mutableStateOf<Point?>(null) }
        var closeContentSize by remember { mutableStateOf<IntSize?>(null) }
        var closeCrrPoint by remember { mutableStateOf<Point?>(null) }
        var closeAnimPoint by remember { mutableStateOf<Point?>(null) }

        var closeCenterPoint by remember { mutableStateOf<Point?>(null) }
        val mountThreshold = remember {
          with(density) {
            closeConfig.mountThresholdDp?.dp?.toPx() ?: closeConfig.mountThresholdPx ?: 1f.dp.toPx()
          }
        }
        val closingThreshold = remember {
          with(density) {
            closeConfig.closingThresholdDp?.dp?.toPx() ?: closeConfig.closingThresholdPx
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
        LaunchedEffect(key1 = configuration, key2 = contentSize) {
          val oldScreenSize = screenSize
          val newScreenSize = getScreenSizeWithoutInsets(context, density, configuration)
          screenSize = newScreenSize

          // snap to edge
          if (mainConfig.isSnapToEdgeEnabled
            && oldScreenSize.width != 0 && oldScreenSize.height != 0
          ) {
            val wasOnRightEdge = crrPoint.x + contentSize.width >= oldScreenSize.width
            val wasOnBottomEdge = crrPoint.y + contentSize.height >= oldScreenSize.height

//          adjust main float position to new screen size
            if (wasOnRightEdge) {
              crrPoint = Point(
                newScreenSize.width - contentSize.width,
                crrPoint.y.coerceIn(0, coerceInMax(newScreenSize.height - contentSize.height))
              )
            }
            if (wasOnBottomEdge) {
              crrPoint = Point(
                crrPoint.x.coerceIn(0, coerceInMax(newScreenSize.width - contentSize.width)),
                newScreenSize.height - contentSize.height
              )
            }

            if (enableAnimations == true) {
              animationState = AnimationState.SNAP_TO_EDGE
              animPoint = crrPoint
            } else {
              windowManager.updateViewLayout(containerView, layoutParams.apply {
                x = crrPoint.x
                y = crrPoint.y
              })
            }
          }
        }

        // update close point when orientation changes and close float is visible to adapt it to
        // new screenSize and crrPoint
        if (closeConfig.enabled
          && isCloseVisible
          && contentSize != IntSize.Zero
          && dragAmountState != null
          && closeContentSize != null
        ) {
          LaunchedEffect(key1 = configuration) {
            val oldScreenSize = screenSize
            val newScreenSize = getScreenSizeWithoutInsets(context, density, configuration)
            screenSize = newScreenSize

            if (oldScreenSize.width != newScreenSize.width ||
              oldScreenSize.height != newScreenSize.height
            ) {
              // adapt initialClosePoint to new screen orientation
              initialClosePoint = getCloseInitialPoint(
                density = density,
                closeConfig = closeConfig,
                contentSize = closeContentSize!!,
                screenSize = screenSize
              )
              closeCrrPoint = initialClosePoint

              if (closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT) {
                closeCrrPoint = followFloat(
                  isFollowerVisible = isCloseVisible,
                  followerInitialPoint = initialClosePoint!!,
                  followerCrrPoint = closeCrrPoint!!,
                  followerContentSize = closeContentSize!!,
                  targetCrrPoint = crrPoint,
                  targetContentSize = contentSize,
                  dragAmount = dragAmountState!!,
                  screenSize = screenSize,
                  closeConfig = closeConfig
                )
              }

              if (enableAnimations == true) {
                closeAnimationState = CloseAnimationState.DRAGGING
                closeAnimPoint = closeCrrPoint
              } else {
                windowManager.updateViewLayout(closeContainerView, closeLayoutParams.apply {
                  x = closeCrrPoint!!.x
                  y = closeCrrPoint!!.y
                })
              }
            }
          }
        }

        if (enableAnimations == true) {
          val transition = updateTransition(targetState = animPoint, label = "point transition")
          transitionSpec = when (animationState) {
            AnimationState.SNAP_TO_EDGE -> mainConfig.snapToEdgeTransitionSpec
            AnimationState.SNAP_TO_CLOSE -> mainConfig.snapToCloseTransitionSpec
            AnimationState.DRAGGING -> mainConfig.draggingTransitionSpec
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
            (when (type) {
              DraggableType.MAIN -> mainConfig
              DraggableType.EXPANDED -> expandedConfig
            }).onDrag?.invoke(
              newChange!!,
              newDragAmount!!,
              crrPoint,
              Point(
                animatedX,
                animatedY
              )
            )
          }

          if (closeConfig.enabled && closeAnimPoint != null && isCloseMounted) {
            val closeTransition =
              updateTransition(targetState = closeAnimPoint!!, label = "close point transition")
            closeTransitionSpec = when (closeAnimationState) {
              CloseAnimationState.DRAGGING -> closeConfig.draggingTransitionSpec
              CloseAnimationState.SNAP_TO_MAIN -> closeConfig.snapToMainTransitionSpec
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

              windowManager.updateViewLayout(closeContainerView, closeLayoutParams.apply {
                x = closeAnimatedX
                y = closeAnimatedY
              })
              closeContainerView.visibility = View.VISIBLE
              isCloseVisible = true
            }
          }
        }

        mod.pointerInput(Unit) {
          val decay = splineBasedDecay<Float>(this)
          val velocityTracker = VelocityTracker()

          detectDragGestures(
            onDragStart = { offset ->
              (when (type) {
                DraggableType.MAIN -> mainConfig
                DraggableType.EXPANDED -> expandedConfig
              }).onDragStart?.let { it(offset) }
            },
            onDrag = { change, dragAmount ->
              dragAmountState = dragAmount
              crrPoint = Point(
                (crrPoint.x + dragAmount.x.toInt()).coerceIn(
                  0,
                  coerceInMax(screenSize.width - contentSize.width)
                ),
                (crrPoint.y + dragAmount.y.toInt()).coerceIn(
                  0,
                  coerceInMax(screenSize.height - contentSize.height)
                )
              )

              // MOUNT CLOSE LOGIC
              if (closeConfig.enabled) {
                if (!isCloseMounted && (abs(dragAmount.x) > mountThreshold || abs(dragAmount.y) > mountThreshold)) {
                  closeContainerView.visibility = View.INVISIBLE
                  windowManager.addView(closeContainerView, closeLayoutParams)
                  isCloseMounted = true
                }
                if (isCloseMounted
                  && !isCloseVisible
                  && (closeContainerView.width > 0 || closeContainerView.height > 0)
                ) {

                  if (closeContentSize == null) {
                    closeContentSize = IntSize(closeContainerView.width, closeContainerView.height)
                  }

                  initialClosePoint = getCloseInitialPoint(
                    density = density,
                    closeConfig = closeConfig,
                    contentSize = closeContentSize!!,
                    screenSize = screenSize
                  )

                  closeCrrPoint = initialClosePoint

                  if (closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT) {
                    closeCrrPoint = followFloat(
                      isFollowerVisible = isCloseVisible,
                      followerInitialPoint = initialClosePoint!!,
                      followerCrrPoint = closeCrrPoint!!,
                      followerContentSize = closeContentSize!!,
                      targetCrrPoint = crrPoint,
                      targetContentSize = contentSize,
                      dragAmount = dragAmount,
                      screenSize = screenSize,
                      closeConfig = closeConfig
                    )
                  }

                  closeCenterPoint = Point(
                    closeCrrPoint!!.x + closeContentSize!!.width / 2,
                    closeCrrPoint!!.y + closeContentSize!!.height / 2
                  )

                  if (enableAnimations == true) {
                    closeAnimationState = CloseAnimationState.DRAGGING
                    closeAnimPoint = closeCrrPoint
                  } else {
                    windowManager.updateViewLayout(closeContainerView, closeLayoutParams.apply {
                      x = closeCrrPoint!!.x
                      y = closeCrrPoint!!.y
                    })
                    closeContainerView.visibility = View.VISIBLE
                    isCloseVisible = true
                  }
                }
              }
              //

              // CLOSING LOGIC
              if (closeConfig.enabled) {
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
                    closingThreshold + closeContainerView.width / 2
                  )
                  val isWithinCloseArea = withinCloseArea

                  when (closeConfig.closeBehavior) {
                    CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT -> {
                      if (isWithinCloseArea) {
                        interruptMovState = InterruptMovState.DRAGGING

                        if (!wasWithinCloseArea) {
                          val newPoint = Point(
                            (closeCenterPoint!!.x - contentSize.width / 2)
                              .coerceIn(0, coerceInMax(screenSize.width - contentSize.width)),
                            (closeCenterPoint!!.y - contentSize.height / 2)
                              .coerceIn(0, coerceInMax(screenSize.height - contentSize.height))
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
                      } else {
                        interruptMovState = null
                      }
                    }

                    CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT -> {
                      if (isWithinCloseArea) {
                        interruptMovState = InterruptMovState.CLOSE_DRAGGING

                        val newSnapPoint = Point(
                          (centerPointF.x.toInt() - closeContainerView.width / 2)
                            .coerceIn(0, coerceInMax(screenSize.width - closeContainerView.width)),
                          (centerPointF.y.toInt() - closeContainerView.height / 2)
                            .coerceIn(
                              0,
                              coerceInMax(screenSize.height - closeContainerView.height)
                            ),
                        )

                        if (enableAnimations == true) {
                          closeAnimationState = CloseAnimationState.SNAP_TO_MAIN
                          closeAnimPoint = newSnapPoint
                        } else {
                          windowManager.updateViewLayout(
                            closeContainerView,
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

                // bottom back
              }
              //

              // DRAGGING CLOSE FLOAT LOGIC
              if (closeConfig.enabled
                && isCloseVisible
                && interruptMovState != InterruptMovState.CLOSE_DRAGGING
                && closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT
                && initialClosePoint != null
                && closeCrrPoint != null
                && closeContentSize != null
              ) {
                closeCrrPoint = followFloat(
                  isFollowerVisible = isCloseVisible,
                  followerInitialPoint = initialClosePoint!!,
                  followerCrrPoint = closeCrrPoint!!,
                  followerContentSize = closeContentSize!!,
                  targetCrrPoint = crrPoint,
                  targetContentSize = contentSize,
                  dragAmount = dragAmount,
                  screenSize = screenSize,
                  closeConfig = closeConfig
                )

                closeCenterPoint = Point(
                  closeCrrPoint!!.x + closeContentSize!!.width / 2,
                  closeCrrPoint!!.y + closeContentSize!!.height / 2
                )

                if (enableAnimations == true) {
                  closeAnimationState = CloseAnimationState.DRAGGING
                  closeAnimPoint = closeCrrPoint
                } else {
                  windowManager.updateViewLayout(closeContainerView, closeLayoutParams.apply {
                    x = closeCrrPoint!!.x
                    y = closeCrrPoint!!.y
                  })
                }
              }
              //

              // DRAGGING MAIN FLOAT LOGIC
              if (interruptMovState != InterruptMovState.DRAGGING) {
                if (enableAnimations == true) {
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
              if (enableAnimations == true && mainConfig.isSnapToEdgeEnabled) {
                velocityTracker.addPosition(change.uptimeMillis, change.position)
              }
              //

              (when (type) {
                DraggableType.MAIN -> mainConfig
                DraggableType.EXPANDED -> expandedConfig
              }).onDrag?.invoke(
                change,
                dragAmount,
                crrPoint,
                null,
              )
            },
            onDragEnd = {
              if (closeConfig.enabled && isCloseMounted && isCloseVisible) {
                windowManager.removeView(closeContainerView)
                isCloseMounted = false
                isCloseVisible = false

                if (withinCloseArea) {
                  onClose?.let { it(false) }
                }
              }

              if (mainConfig.isSnapToEdgeEnabled) {
                if (enableAnimations == true) {
                  animationState = AnimationState.SNAP_TO_EDGE

                  val decayCenterX = decay.calculateTargetValue(
                    initialValue = (crrPoint.x + contentSize.width / 2).toFloat(),
                    initialVelocity = velocityTracker.calculateVelocity().x
                  )
                  val newPoint = Point(
                    if (decayCenterX >= screenSize.width * 0.5f) {
                      screenSize.width - contentSize.width
                    } else {
                      0
                    },
                    crrPoint.y
                  )

                  animPoint = newPoint
                  crrPoint = newPoint
                } else {
                  val newPoint = Point(
                    if (crrPoint.x + contentSize.width / 2 >= screenSize.width * 0.5) {
                      screenSize.width - contentSize.width
                    } else {
                      0
                    },
                    layoutParams.y
                  )
                  windowManager.updateViewLayout(containerView, layoutParams.apply {
                    x = newPoint.x
                    y = newPoint.y
                  })
                  crrPoint = newPoint
                }
              }
              (when (type) {
                DraggableType.MAIN -> mainConfig
                DraggableType.EXPANDED -> expandedConfig
              }).onDragEnd?.let { it() }
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
  closeConfig: CloseFloatyConfig
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

  val pointModifierY = ((if (!isFollowerVisible) abs(centersDistance.y) else abs(dragAmount.y.toInt())) * closeConfig.followRate).toInt()
  val followerPoint = if (!isFollowerVisible) followerInitialPoint else followerCrrPoint

  val newX = (followerInitialPoint.x - (centersDistance.x * closeConfig.followRate)).toInt()

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
    newY.coerceIn(0, coerceInMax((screenSize.height - followerContentSize.height - (followerContentSize.height * closeConfig.followRate)).toInt()))
  )
}

private fun getCloseInitialPoint(
  density: Density,
  closeConfig: CloseFloatyConfig,
  contentSize: IntSize,
  screenSize: IntSize,
): Point {
  val hasCustomPos = closeConfig.startPointDp != null || closeConfig.startPointPx != null
  val bottomPadding = with(density) {
      closeConfig.bottomPaddingDp?.dp?.toPx() ?: closeConfig.bottomPaddingPx ?: 16.dp.toPx()
  }

  val closeInitialPoint = if (hasCustomPos) with(density) {
    val customStartX = (closeConfig.startPointDp?.x?.dp?.roundToPx() ?: closeConfig.startPointPx?.x
    ?: 0).toInt()
    val customStartY = (closeConfig.startPointDp?.y?.dp?.roundToPx() ?: closeConfig.startPointPx?.y
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