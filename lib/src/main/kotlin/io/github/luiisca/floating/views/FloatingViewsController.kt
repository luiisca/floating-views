package io.github.luiisca.floating.views

import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.luiisca.floating.views.ui.AdaptiveSizeWrapper
import io.github.luiisca.floating.views.ui.CloseFloat
import io.github.luiisca.floating.views.ui.DraggableFloat
import io.github.luiisca.floating.views.ui.FullscreenOverlayFloat
import io.github.luiisca.floating.views.helpers.NotificationHelper
import io.github.luiisca.floating.views.helpers.toPx

enum class CloseBehavior {
    MAIN_SNAPS_TO_CLOSE_FLOAT,
    CLOSE_SNAPS_TO_MAIN_FLOAT,
}
enum class DraggableType {
    MAIN,
    EXPANDED
}

/**
 * @property startPointDp Initial position of the floating view in density-independent pixels (dp).
 *
 * When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used
 * @property startPointPx Initial position of the floating view in pixels (px).
 *
 * When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used
 * @property draggingTransitionSpec Animation specification for dragging.
 *
 * Applied when `enableAnimations == true`.
 *
 * Default:
 *
 *                                  spring(
 *                                      dampingRatio = Spring.DampingRatioNoBouncy,
 *                                      stiffness = Spring.StiffnessHigh
 *                                  )
 * @property snapToEdgeTransitionSpec Animation specification for snapping to screen edge.
 *
 * Applied when `enableAnimations == true && isSnapToEdgeEnabled`.
 *
 * Default:
 *
 *                                    spring(
 *                                      dampingRatio = Spring.DampingRatioMediumBouncy,
 *                                      stiffness = Spring.StiffnessMedium
 *                                    )
 * @property snapToCloseTransitionSpec Animation specification for snapping to close float.
 *
 * Applied when `enableAnimations == true &&
 *                                     closeConfig.closeBehavior == CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT`.
 *
 * Default:
 *
 *                                     spring(
 *                                       dampingRatio = Spring.DampingRatioMediumBouncy,
 *                                       stiffness = Spring.StiffnessLow
 *                                     )
 * @property isSnapToEdgeEnabled If true, the floating view snaps to the nearest screen edge on `dragEnd`.
 * @property onTap Callback triggered when the floating view is tapped.
 * @property onDragStart Callback triggered when dragging of the floating view begins.
 * @property onDrag Callback triggered during dragging of the floating view.
 * @property onDragEnd Callback triggered when dragging of the floating view ends.
 */
sealed interface FloatConfig {
    var startPointDp: PointF?
    var startPointPx: PointF?
    var draggingTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)
    var snapToEdgeTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)
    var snapToCloseTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)
    var isSnapToEdgeEnabled: Boolean
    var onTap: ((Offset) -> Unit)?
    var onDragStart: ((offset: Offset) -> Unit)?
    var onDrag: ((
        change: PointerInputChange,
        dragAmount: Offset,
        newPoint: Point,
        newAnimatedPoint: Point?) -> Unit)?
    var onDragEnd: (() -> Unit)?
}

/**
* @property viewFactory A function that creates an Android View to be displayed in the floating view.
* This is an alternative to using a Composable.
 * @property composable Jetpack Compose function defining the content of the floating view.
*/
data class MainFloatConfig(
    val composable: (@Composable () -> Unit)? = null,
    val viewFactory: ((Context) -> View)? = null,
    override var startPointDp: PointF? = null,
    override var startPointPx: PointF? = null,
    override var draggingTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
    },
    override var snapToEdgeTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    },
    override var snapToCloseTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )},
    override var isSnapToEdgeEnabled: Boolean = true,
    override var onTap: ((Offset) -> Unit)? = null,
    override var onDragStart: ((offset: Offset) -> Unit)? = null,
    override var onDrag: ((
        change: PointerInputChange,
        dragAmount: Offset,
        newPoint: Point,
        newAnimatedPoint: Point?) -> Unit)? = null,
    override var onDragEnd: (() -> Unit)? = null,
): FloatConfig

/**
 * @property enabled Determines if the expanded floating view is active.
 * @property tapOutsideToClose Determines if tapping outside of expanded view should close it.
 * @property composable Jetpack Compose function defining the content of the expanded view.
 *
 * Call close to remove expanded view (and overlay view if enabled) and add main view to windowManager again
 * @property viewFactory A function that creates an Android View to be displayed in the floating view.
 * This is an alternative to using a Composable.
 *
 * Call close to remove expanded view (and overlay view if enabled) and add main view to windowManager again
 * @property dimAmount This is the amount of dimming to apply behind expanded view. Range is from 1.0 for completely opaque to 0.0 for no dim.
 */
data class ExpandedFloatConfig(
    val enabled: Boolean = true,
    val tapOutsideToClose: Boolean = true,
    val dimAmount: Float = 0.5f,
    val composable: (@Composable (close: () -> Unit) -> Unit)? = null,
    val viewFactory: ((context: Context, close:() -> Unit) -> View)? = null,
    override var startPointDp: PointF? = null,
    override var startPointPx: PointF? = null,
    override var draggingTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
    },
    override var snapToEdgeTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    },
    override var snapToCloseTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )},
    override var isSnapToEdgeEnabled: Boolean = true,
    override var onTap: ((Offset) -> Unit)? = null,
    override var onDragStart: ((offset: Offset) -> Unit)? = null,
    override var onDrag: ((
        change: PointerInputChange,
        dragAmount: Offset,
        newPoint: Point,
        newAnimatedPoint: Point?) -> Unit)? = null,
    override var onDragEnd: (() -> Unit)? = null,
): FloatConfig


/**
 * Configuration class for the close floating view.
 *
 * @property enabled Determines if the close floating view is active.
 * @property composable Jetpack Compose function defining the content of the close floating view.
 * @property viewFactory A function that creates an Android View to be displayed in the close floating view.
 * This is an alternative to using a Composable.
 * @property startPointDp The initial position of the close floating view in density-independent pixels (dp).
 *
 * When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used
 * @property startPointPx The initial position of the close floating view in pixels (px).
 *
 * When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used
 * @property mountThresholdDp The drag distance required to show the close float, in density-independent pixels (dp).
 *                            A larger value requires more dragging before the close float becomes visible.
 *
 * When neither `mountThresholdDp` nor `mountThresholdPx` are provided `1.dp` is used
 * @property mountThresholdPx The drag distance required to show the close float, in pixels (px).
 *                            A larger value requires more dragging before the close float becomes visible.
 *
 * When neither `mountThresholdDp` nor `mountThresholdPx` are provided `1.dp` is used
 * @property closingThresholdDp The distance (in density-independent pixels) between the main float
 * and the close float that triggers the `closeBehavior`.
 *
 * When neither `closingThresholdDp` nor `closingThresholdPx` are provided `100.dp` is used
 * @property closingThresholdPx The distance (in pixels) between the main float and the close float
 * that triggers the `closeBehavior`.
 *
 * When neither `closingThresholdDp` nor `closingThresholdPx` are provided `100.dp` is used
 * @property bottomPaddingDp The bottom padding for the close float in density-independent pixels (dp).
 *
 * When neither `bottomPaddingDp` nor `bottomPaddingPx` are provided `16.dp` is used
 * @property bottomPaddingPx The bottom padding for the close float in pixels (px).
 *
 * When neither `bottomPaddingDp` nor `bottomPaddingPx` are provided `16.dp` is used
 * @property draggingTransitionSpec Defines the animation for dragging the close float.
 *
 * Used when `enableAnimations == true && closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.
 *
 * Default:
 *
 *                                  spring(
 *                                      dampingRatio = Spring.DampingRatioNoBouncy,
 *                                      stiffness = Spring.StiffnessHigh
 *                                  )
 * @property snapToMainTransitionSpec Defines the animation for the close float snapping to the main float.
 *
 * Used when `enableAnimations == true && closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.
 *
 * Default:
 *
 *                                    spring(
 *                                        dampingRatio = Spring.DampingRatioLowBouncy,
 *                                        stiffness = Spring.StiffnessLow
 *                                    )
 *
 * @property closeBehavior Determines how the close float interacts with the main float.
 *
 * * `CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`: The close float follows the main float,
 *                           moving based on `followRate`. It snaps to the main float when their distance
 *                           exceeds `closingThresholdDp` or `closingThresholdPx`.
 * * `CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT`: The main float snaps to the close float
 *                           when their distance exceeds `closingThresholdDp` or `closingThresholdPx`.
 *
 *
 *                      Default: `CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT`
 *
 * @property followRate Controls the movement of the close float when following the main float.
 *
 * Only used when `closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.
 *
 *                      Default: 0.1f
 */
data class CloseFloatConfig(
    val enabled: Boolean = true,
    val composable: (@Composable () -> Unit)? = null,
    val viewFactory: ((Context) -> View)? = null,
    var startPointDp: PointF? = null,
    var startPointPx: PointF? = null,
    val mountThresholdDp: Float? = null,
    val mountThresholdPx: Float? = null,
    val closingThresholdDp: Float? = null,
    val closingThresholdPx: Float? = null,
    val bottomPaddingDp: Float? = null,
    val bottomPaddingPx: Float? = null,
    var draggingTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        )
    },
    var snapToMainTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>) = {
      spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
      )
    },
    var closeBehavior: CloseBehavior? = CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT,
    var followRate: Float = 0.1f,
)

class FloatingViewsController(
    private val context: Context,
    /**
     * Called after the last main float view is "dropped" within the close area
     */
    private val stopService: () -> Unit,
    private val enableAnimations: Boolean = true,
    private val mainFloatConfig: MainFloatConfig = MainFloatConfig(),
    private val closeFloatConfig: CloseFloatConfig = CloseFloatConfig(),
    private val expandedFloatConfig: ExpandedFloatConfig = ExpandedFloatConfig(),
) {
    private val composeOwner = FloatLifecycleOwner()
    private var isComposeOwnerInit: Boolean = false
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
    private var closeView: ComposeView? = null
    private var closeLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }
    private var createFloatViews: CreateFloatViews? = null
    private var floatsCount: Int = 0
    private val addedViews = mutableListOf<View>()

    init {
        if (closeFloatConfig.enabled) {
            createCloseView()
        }
    }

    /**
     * Starts a foreground service with a default customizable notification.
     * Can be omitted for `service.startForeground` with custom notification or just pass custom parameters
     */
    fun startForegroundService(icon: Int = R.drawable.round_bubble_chart_24, title: String = "Floating views running") {
        val service = context as Service
        val notificationHelper = NotificationHelper(service)
        notificationHelper.createNotificationChannel()

        service.startForeground(notificationHelper.notificationId, notificationHelper.createDefaultNotification(icon, title))
    }

    /**
     * Sets up a new floating view, as well as close, expanded, and overlay views based on the configs
     * passed to the `FloatingViewsController` and manages the lifecycle and interactions between them
     */
    fun initializeNewFloatSystem() {
        floatsCount += 1

        createFloatViews = CreateFloatViews(
            context,
            enableAnimations,
            mainFloatConfig,
            expandedFloatConfig,
            closeFloatConfig,
            addToComposeLifecycle = { composable ->
                addToComposeLifecycle(composable)
            },
            closeView,
            closeLayoutParams,
            getFloatsCount = {floatsCount},
            setFloatsCount = {floatsCount = it},
            stopService,
            addViewToTrackingList = {view -> addedViews.add(view)},
        )
    }

    /**
     * Removes all views added while the Service was alive
     */
    fun shutdownAllFloatSystems() {
        addedViews.forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
            }
        }
        addedViews.clear()
    }

    private fun createCloseView() {
        val closeFloat = ComposeView(context).apply {
            closeView = this
            this.setContent {
                CloseFloat(
                    windowManager = windowManager,
                    containerView = closeView!!,
                    layoutParams = closeLayoutParams,
                ) {
                    AdaptiveSizeWrapper(updateLayoutParams = { contentSize, screenSize ->
                        closeLayoutParams = if (contentSize.width >= screenSize.width) {
                            closeLayoutParams.apply {
                                width = screenSize.width
                            }
                        } else {
                            closeLayoutParams.apply {
                                width = contentSize.width
                            }
                        }
                    }) {
                        when {
                            closeFloatConfig.viewFactory != null -> closeFloatConfig.viewFactory.let { factory ->
                                AndroidView(
                                    factory = { context ->
                                        factory(context)
                                    }
                                )
                            }

                            closeFloatConfig.composable != null -> closeFloatConfig.composable.invoke()
                            else -> DefaultCloseButton()
                        }
                    }
                }
            }
        }

        addToComposeLifecycle(closeFloat)
    }

    private fun addToComposeLifecycle(composable: ComposeView) {
        composeOwner.attachToDecorView(composable)
        if (!isComposeOwnerInit) {
            composeOwner.onCreate()

            isComposeOwnerInit = true
        }
        composeOwner.onStart()
        composeOwner.onResume()
    }
}

class CreateFloatViews(
    private val context: Context,
    private val enableAnimations: Boolean,
    private val mainFloatConfig: MainFloatConfig = MainFloatConfig(),
    private val expandedFloatConfig: ExpandedFloatConfig,
    private val closeFloatConfig: CloseFloatConfig,
    private val addToComposeLifecycle: (composable: ComposeView) -> Unit,
    private val closeView: ComposeView?,
    private val closeLayoutParams: WindowManager.LayoutParams,
    private val getFloatsCount: () -> Int,
    private val setFloatsCount: (newCount: Int) -> Unit,
    private val stopService: () -> Unit,
    private val addViewToTrackingList: (view: View) -> Unit,
) {
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager

    private var mainView: ComposeView? = null
    private var expandedView: ComposeView? = null
    private var overlayView: ComposeView? = null
    private val mainStartPoint = Point(
            (mainFloatConfig.startPointDp?.x?.toPx() ?: mainFloatConfig.startPointPx?.x ?: 0f).toInt(),
            (mainFloatConfig.startPointDp?.y?.toPx() ?: mainFloatConfig.startPointPx?.y ?: 0f).toInt()
        )
    private val expandedStartPoint = Point(
        (expandedFloatConfig.startPointDp?.x?.toPx() ?: expandedFloatConfig.startPointPx?.x ?: 0f).toInt(),
        (expandedFloatConfig.startPointDp?.y?.toPx() ?: expandedFloatConfig.startPointPx?.y ?: 0f).toInt()
    )
    private var mainLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        x = mainStartPoint.x
        y = mainStartPoint.y
    }
    private var expandedLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
        dimAmount = expandedFloatConfig.dimAmount
        x = expandedStartPoint.x
        y = expandedStartPoint.y
    }
    private val overlayLayoutParams = baseLayoutParams().apply {
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
    }

    private var isOverlayVisible = false
    private var isExpandedVisible = false
    private var isMainVisible = false

    init {
        createMainView()
    }

    private fun createMainView() {
        val _mainView = ComposeView(context).apply {
            mainView = this
            this.setContent {
                DraggableFloat(
                    type = DraggableType.MAIN,
                    windowManager = windowManager,
                    containerView = mainView!!,
                    closeContainerView = closeView!!,
                    closeLayoutParams = closeLayoutParams,
                    layoutParams = mainLayoutParams,
                    enableAnimations = enableAnimations,
                    mainConfig = mainFloatConfig,
                    closeConfig = closeFloatConfig,
                    expandedConfig = expandedFloatConfig,
                    openExpandedView = { openExpanded() },
                    onClose = { openMainAfter ->
                        if (getFloatsCount() <= 1) {
                            stopService()
                        }
                        if (openMainAfter == false) {
                            setFloatsCount(getFloatsCount() - 1)
                        }
                        tryCloseDraggable(openMainAfter ?: false)
                    }
                ) {
                    AdaptiveSizeWrapper(updateLayoutParams = { contentSize, screenSize ->
                        mainLayoutParams = if (contentSize.width >= screenSize.width) {
                            mainLayoutParams.apply {
                                width = screenSize.width
                            }
                        } else {
                            mainLayoutParams.apply {
                                width = contentSize.width
                            }
                        }
                    }) {
                        when {
                            mainFloatConfig.viewFactory != null -> mainFloatConfig.viewFactory.let { viewFactory ->
                                AndroidView(
                                    factory = { context ->
                                        viewFactory(context)
                                    }
                                )
                            }

                            mainFloatConfig.composable != null -> mainFloatConfig.composable.invoke()
                            else -> throw IllegalArgumentException("Either compose or view must be provided for MainFloat")
                        }
                    }
                }
            }
        }

        addToComposeLifecycle(_mainView)
        windowManager.addView(_mainView, mainLayoutParams)
        addViewToTrackingList(_mainView)
        isMainVisible = true
    }

    private fun createExpandedView() {
        val _expandedView = ComposeView(context).apply {
            expandedView = this
            this.setContent {
                DraggableFloat(
                    type = DraggableType.EXPANDED,
                    windowManager = windowManager,
                    containerView = expandedView!!,
                    closeContainerView = closeView!!,
                    closeLayoutParams = closeLayoutParams,
                    layoutParams = expandedLayoutParams,
                    enableAnimations = enableAnimations,
                    mainConfig = mainFloatConfig,
                    closeConfig = closeFloatConfig,
                    expandedConfig = expandedFloatConfig,
                    onClose = { openMainAfter ->
                        if (getFloatsCount() <= 1) {
                            stopService()
                        }
                        if (openMainAfter == false) {
                            setFloatsCount(getFloatsCount() - 1)
                        }
                        tryCloseDraggable(openMainAfter ?: false)
                    }
                ) {
                    AdaptiveSizeWrapper(updateLayoutParams = { contentSize, screenSize ->
                        expandedLayoutParams = if (contentSize.width >= screenSize.width) {
                            expandedLayoutParams.apply {
                                width = screenSize.width
                            }
                        } else {
                            expandedLayoutParams.apply {
                                width = contentSize.width
                            }
                        }
                    }) {
                        when {
                            expandedFloatConfig.viewFactory != null -> expandedFloatConfig.viewFactory.let { viewFactory ->
                                AndroidView(
                                    factory = { context ->
                                        viewFactory(context) { tryCloseDraggable() }
                                    }
                                )
                            }
                            expandedFloatConfig.composable != null -> expandedFloatConfig.composable.let {composable ->
                                composable { tryCloseDraggable() }
                            }

                            else -> throw IllegalArgumentException("Either compose or view must be provided for MainFloat")
                        }
                    }
                }
            }
        }

        addToComposeLifecycle(_expandedView)
        windowManager.addView(_expandedView, expandedLayoutParams)
        addViewToTrackingList(_expandedView)
        isExpandedVisible = true
    }

    private fun tryCreateOverlayView() {
        if (expandedFloatConfig.tapOutsideToClose) {
            val _overlayView = ComposeView(context).apply {
                overlayView = this
                this.setContent {
                    FullscreenOverlayFloat(
                        onTap = { tryCloseDraggable() }
                    )
                }
            }

            addToComposeLifecycle(_overlayView)
            windowManager.addView(_overlayView, overlayLayoutParams)
            addViewToTrackingList(_overlayView)
            isOverlayVisible = true
        }
    }

    private fun openExpanded() {
        tryRemoveMain()
        tryCreateOverlayView()
        createExpandedView()
    }
    private fun tryCloseDraggable(openMainAfter: Boolean = true) {
        tryRemoveMain()
        tryRemoveExpanded()
        tryRemoveOverlay()
        if (openMainAfter) {
            createMainView()
        }
    }
    private fun tryRemoveMain() {
        if (mainView != null && isMainVisible) {
            windowManager.removeView(mainView)
            isMainVisible = false
        }
    }
    private fun tryRemoveExpanded() {
        if (expandedView != null && isExpandedVisible) {
            windowManager.removeView(expandedView)
            isExpandedVisible = false
        }
    }
    private fun tryRemoveOverlay() {
        if (overlayView != null && isOverlayVisible) {
            windowManager.removeView(overlayView)
            isOverlayVisible = false
        }
    }
}

@Composable
private fun DefaultCloseButton() {
    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.rounded_cancel_24),
            contentDescription = "Close float view",
            modifier = Modifier.size(60.dp)
        )
    }
}

private fun baseLayoutParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT

        gravity = Gravity.TOP or Gravity.START
        format = PixelFormat.TRANSLUCENT

        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
}
