package io.github.luiisca.floating.views

import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ServiceCompat
import io.github.luiisca.floating.views.helpers.FloatingViewsManager
import io.github.luiisca.floating.views.ui.CloseFloat
import io.github.luiisca.floating.views.ui.DraggableFloat
import io.github.luiisca.floating.views.ui.FullscreenOverlayFloat
import io.github.luiisca.floating.views.helpers.NotificationHelper
import io.github.luiisca.floating.views.helpers.toPx

enum class CloseBehavior {
    MAIN_SNAPS_TO_CLOSE_FLOAT,
    CLOSE_SNAPS_TO_MAIN_FLOAT,
}

interface EventCallbacks {
    var onTap: ((Offset) -> Unit)?
    var onDragStart: ((offset: Offset) -> Unit)?
    var onDrag: ((
        change: PointerInputChange,
        dragAmount: Offset,
        newPoint: Point,
        newAnimatedPoint: Point?) -> Unit)?
    var onDragEnd: (() -> Unit)?
}
sealed interface FloatConfig: EventCallbacks {
    var startPointDp: PointF?
    var startPointPx: PointF?
    var draggingTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)
    var snapToEdgeTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)
    var snapToCloseTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)
    var isSnapToEdgeEnabled: Boolean
}

/**
 * @property composable The Composable to be rendered inside the floating view. If null, [viewFactory] is used.
 * @property viewFactory Factory function to create a traditional Android view inside the floating view. If null, [composable] is used.
 * @property startPointDp Initial position of the floating view in density-independent pixels (dp).
 *
 * When neither [startPointDp] nor [startPointPx] are provided `PointF(0,0)` is used.
 * @property startPointPx Initial position of the floating view in pixels (px).
 *
 * When neither [startPointDp] nor [startPointPx] are provided `PointF(0,0)` is used.
 * @property draggingTransitionSpec Animation specification for dragging.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true`.
 *
 * Default:
 *
 *                                  spring(
 *                                      dampingRatio = Spring.DampingRatioNoBouncy,
 *                                      stiffness = Spring.StiffnessHigh
 *                                  )
 * @property snapToEdgeTransitionSpec Animation specification for snapping to screen edge.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true` and [isSnapToEdgeEnabled] is `true`.
 *
 * Default:
 *
 *                                    spring(
 *                                      dampingRatio = Spring.DampingRatioMediumBouncy,
 *                                      stiffness = Spring.StiffnessMedium
 *                                    )
 * @property snapToCloseTransitionSpec Animation specification for snapping to close float.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true` and [CloseFloatConfig.closeBehavior] is [CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT].
 *
 * Default:
 *
 *                                     spring(
 *                                       dampingRatio = Spring.DampingRatioMediumBouncy,
 *                                       stiffness = Spring.StiffnessLow
 *                                     )
 * @property isSnapToEdgeEnabled If `true`, the floating view will snap to the nearest screen edge when dragging ends.
 *
 * Default: `true`
 * @property onTap Callback triggered when the floating view is tapped.
 * @property onDragStart Callback triggered when dragging of the floating view begins.
 * @property onDrag Callback triggered during dragging of the floating view.
 * @property onDragEnd Callback triggered when dragging of the floating view ends.
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
 * @property enabled If `true`, enables expanded view mode.
 *
 * Default: `true`
 * @property tapOutsideToClose If `true` adds an overlay view that will close expanded view when tapped.
 *
 * Default: `true`
 * @property dimAmount Controls the dimming amount of the background when the view is expanded. Range is from 1.0 for completely opaque to 0.0 for no dim.
 *
 * Default: `0.5f`
 * @property composable The Composable to be rendered inside the expanded view. If null, [viewFactory] is used. Call close to remove expanded view.
 * @property viewFactory Factory function to create a traditional Android view inside the expanded view. If null, [composable] is used. Call close to remove expanded view.
 * @property startPointDp Initial position of the floating view in density-independent pixels (dp).
 *
 * When neither [startPointDp] nor [startPointPx] are provided `PointF(0,0)` is used.
 * @property startPointPx Initial position of the floating view in pixels (px).
 *
 * When neither [startPointDp] nor [startPointPx] are provided `PointF(0,0)` is used.
 * @property draggingTransitionSpec Animation specification for dragging.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true`.
 *
 * Default:
 *
 *                                  spring(
 *                                      dampingRatio = Spring.DampingRatioNoBouncy,
 *                                      stiffness = Spring.StiffnessHigh
 *                                  )
 * @property snapToEdgeTransitionSpec Animation specification for snapping to screen edge.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true` and [isSnapToEdgeEnabled] is `true`.
 *
 * Default:
 *
 *                                    spring(
 *                                      dampingRatio = Spring.DampingRatioMediumBouncy,
 *                                      stiffness = Spring.StiffnessMedium
 *                                    )
 * @property snapToCloseTransitionSpec Animation specification for snapping to close float.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true` and [CloseFloatConfig.closeBehavior] is [CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT].
 *
 * Default:
 *
 *                                     spring(
 *                                       dampingRatio = Spring.DampingRatioMediumBouncy,
 *                                       stiffness = Spring.StiffnessLow
 *                                     )
 * @property isSnapToEdgeEnabled If `true`, the floating view will snap to the nearest screen edge when dragging ends.
 *
 * Default: `true`
 * @property onTap Callback triggered when the floating view is tapped.
 * @property onDragStart Callback triggered when dragging of the floating view begins.
 * @property onDrag Callback triggered during dragging of the floating view.
 * @property onDragEnd Callback triggered when dragging of the floating view ends.
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
 * @property enabled If `true`, enables the close float behavior.
 *
 * Default: `true`
 * @property composable The Composable to be rendered inside the close floating view. If null, [viewFactory] is used.
 * @property viewFactory Factory function to create a traditional Android view inside the close floating view. If null, [composable] is used.
 * @property startPointDp Initial position of the close floating view in density-independent pixels (dp).
 *
 * When neither [startPointDp] nor [startPointPx] are provided `PointF(0,0)` is used.
 * @property startPointPx Initial position of the close floating view in pixels (px).
 *
 * When neither [startPointDp] nor [startPointPx] are provided `PointF(0,0)` is used.
 * @property mountThresholdDp Dragging distance required to show the close float, in density-independent pixels (dp).
 *                            A larger value requires more dragging before the close float becomes visible.
 *
 * When neither [mountThresholdDp] nor [mountThresholdPx] are provided `1.dp` is used.
 * @property mountThresholdPx Dragging distance required to show the close float, in pixels (px).
 *                            A larger value requires more dragging before the close float becomes visible.
 *
 * When neither [mountThresholdDp] nor [mountThresholdPx] are provided `1.dp` is used.
 * @property closingThresholdDp Dragging distance (in density-independent pixels) between the main float
 * and the close float that triggers a [CloseFloatConfig.closeBehavior].
 *
 * When neither [closingThresholdDp] nor [closingThresholdPx] are provided `100.dp` is used.
 * @property closingThresholdPx Dragging distance (in pixels) between the main float and the close float
 * that triggers a [CloseFloatConfig.closeBehavior].
 *
 * When neither [closingThresholdDp] nor [closingThresholdPx] are provided `100.dp` is used.
 * @property bottomPaddingDp Bottom padding for the close float in density-independent pixels (dp).
 *
 * When neither [bottomPaddingDp] nor [bottomPaddingPx] are provided `16.dp` is used.
 * @property bottomPaddingPx Bottom padding for the close float in pixels (px).
 *
 * When neither [bottomPaddingDp] nor [bottomPaddingPx] are provided `16.dp` is used.
 * @property draggingTransitionSpec Animation specification for dragging.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true` and [CloseFloatConfig.closeBehavior] is [CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT].
 *
 * Default:
 *
 *                                  spring(
 *                                      dampingRatio = Spring.DampingRatioNoBouncy,
 *                                      stiffness = Spring.StiffnessHigh
 *                                  )
 * @property snapToMainTransitionSpec Animation specification for snapping to main float.
 *
 * Applied when [FloatingViewsConfig.enableAnimations] is `true` and [CloseFloatConfig.closeBehavior] is [CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT].
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
 * * [CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT]: The close float follows the main float,
 * moving based on [followRate]. It snaps to the main float when their distance
 * exceeds [closingThresholdDp] or [closingThresholdPx].
 * * [CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT]: The main float snaps to the close float
 * when their distance exceeds [closingThresholdDp] or [closingThresholdPx].
 *
 * Default: [CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT]
 *
 * @property followRate Defines the rate at which the close float follows the main float when dragged.
 *
 * Only used when [CloseFloatConfig.closeBehavior] is [CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT].
 *
 * Default: 0.1f
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

/**
 *
 * @param enableAnimations Whether to enable animations for the floating views.
 *
 * Default: `true`
 * @param main Configuration for the main floating view.
 *
 * Default:  [MainFloatConfig].
 * @param close Configuration for the close floating view.
 *
 * Default: [CloseFloatConfig].
 * @param expanded Configuration for the expanded floating view.
 *
 * Default: [ExpandedFloatConfig].
 *
 * @see MainFloatConfig
 * @see CloseFloatConfig
 * @see ExpandedFloatConfig
 */
data class FloatingViewsConfig(
    val enableAnimations: Boolean = true,
    val main: MainFloatConfig,
    val close: CloseFloatConfig,
    val expanded: ExpandedFloatConfig
)

/**
 * Manages floating views within a service context, including creation, tracking, and cleanup.
 *
 * @param context The service context used to create floating views.
 * @param stopService Callback triggered when all floating views are dismissed.
 */
class FloatingViewsController(
    private val context: Context,
    private val stopService: () -> Unit,
) {
    private val composeOwner = FloatLifecycleOwner()
    private var isComposeOwnerInit: Boolean = false
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
    private var floatsCount: Int = 0
    private val addedViews = mutableListOf<View>()

    /**
     * Elevates the current service to foreground status with a persistent customizable notification.
     *
     * Foreground services receive a higher priority, making them less likely to be terminated
     * by the system under memory pressure.
     *
     * **Note:** Requires `FOREGROUND_SERVICE` permission in `AndroidManifest.xml`
     *
     * **Note:** [icon] and [title] have precedence over [FloatingViewsManager.setNotificationProperties]
     *
     * **Usage:** Call this method in your Service's `onCreate()` method, unless using a custom notification
     *
     * @param icon The resource ID for the notification icon.
     * @param title The notification title text.
     *
     * @throws IllegalStateException if not called from a Service context
     */
    fun initializeAsForegroundService(icon: Int? = null, title: String? = null) {
        val service = context as? Service
            ?: throw IllegalStateException("This function must be called from a Service context")

        val notificationHelper = NotificationHelper(service)
        notificationHelper.createNotificationChannel()
        val notificationIcon: Int = icon ?: FloatingViewsManager.notificationIcon
        val notificationTitle: String = title ?: FloatingViewsManager.notificationTitle

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                service,
                notificationHelper.notificationId,
                notificationHelper.createDefaultNotification(notificationIcon, notificationTitle),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            service.startForeground(notificationHelper.notificationId, notificationHelper.createDefaultNotification(notificationIcon, notificationTitle))
        }
    }

    /**
     * Creates and starts a new dynamic, interactive floating view.
     */
    fun startDynamicFloatingView(config: FloatingViewsConfig) {
        floatsCount += 1

        CreateFloatViews(
            context,
            config,
            getFloatsCount = {floatsCount},
            setFloatsCount = {floatsCount = it},
            stopService,
            addViewToTrackingList = {view -> addedViews.add(view)},
            composeOwner,
            getIsComposeOwnerInit = {isComposeOwnerInit},
            setIsComposeOwnerInit = {isComposeOwnerInit = it}
        )
    }

    /**
     * Removes all views added while the Service was alive
     */
    fun stopAllDynamicFloatingViews() {
        addedViews.forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                Log.e("error", "could not remove view")
            }
        }
        addedViews.clear()
    }
}

class CreateFloatViews(
    private val context: Context,
    private val config: FloatingViewsConfig,
    private val getFloatsCount: () -> Int,
    private val setFloatsCount: (newCount: Int) -> Unit,
    private val stopService: () -> Unit,
    private val addViewToTrackingList: (view: View) -> Unit,
    private val composeOwner: FloatLifecycleOwner,
    private val getIsComposeOwnerInit: () -> Boolean,
    private val setIsComposeOwnerInit: (bool: Boolean) -> Unit,
) {
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager

    private var mainView: ComposeView? = null
    private var expandedView: ComposeView? = null
    private var overlayView: ComposeView? = null
    private var closeView: ComposeView? = null
    private val mainStartPoint = Point(
            (config.main.startPointDp?.x?.toPx() ?: config.main.startPointPx?.x ?: 0f).toInt(),
            (config.main.startPointDp?.y?.toPx() ?: config.main.startPointPx?.y ?: 0f).toInt()
        )
    private val expandedStartPoint = Point(
        (config.expanded.startPointDp?.x?.toPx() ?: config.expanded.startPointPx?.x ?: 0f).toInt(),
        (config.expanded.startPointDp?.y?.toPx() ?: config.expanded.startPointPx?.y ?: 0f).toInt()
    )
    private var mainLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        x = mainStartPoint.x
        y = mainStartPoint.y
    }
    private var expandedLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
        dimAmount = config.expanded.dimAmount
        x = expandedStartPoint.x
        y = expandedStartPoint.y
    }
    private var closeLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }
    private val overlayLayoutParams = baseLayoutParams().apply {
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
    }

    init {
        createMainView()

        if (config.close.enabled) {
            createCloseView()
        }
    }

    private fun createMainView() {
        val _mainView = ComposeView(context).apply {
            mainView = this

            this.setContent {
                DraggableFloat(
                    windowManager = windowManager,
                    containerView = mainView!!,
                    closeView = closeView!!,
                    layoutParams = mainLayoutParams,
                    closeLayoutParams = closeLayoutParams,
                    config = config,
                    onKey = { event ->
                        if (event.key == Key.Back) {
                            tryCloseDraggable()

                            true
                        } else {
                            false
                        }
                    },
                    onDestroy = {
                        tryCloseDraggable(true)
                        if (getFloatsCount() <= 1) {
                            stopService()
                        }
                        setFloatsCount(getFloatsCount() - 1)
                    },
                    onTap = { offset ->
                        if (config.expanded.enabled) {
                            openExpanded()
                        }
                        config.main.onTap?.let { it(offset) }
                    },
                    onDragStart = config.main.onDragStart,
                    onDrag = config.main.onDrag,
                    onDragEnd = config.main.onDragEnd,
                ) {
                        when {
                            config.main.viewFactory != null -> config.main.viewFactory.let { viewFactory ->
                                AndroidView(
                                    factory = { context ->
                                        viewFactory(context)
                                    }
                                )
                            }

                            config.main.composable != null -> config.main.composable.invoke()
                            else -> throw IllegalArgumentException("Either compose or view must be provided for MainFloat")
                        }
                }
            }
        }

        _mainView.visibility = View.INVISIBLE
        compose(_mainView, mainLayoutParams)
    }

    private fun createExpandedView() {
        val _expandedView = ComposeView(context).apply {
            expandedView = this
            this.setContent {
                DraggableFloat(
                    windowManager = windowManager,
                    containerView = expandedView!!,
                    closeView = closeView!!,
                    closeLayoutParams = closeLayoutParams,
                    layoutParams = expandedLayoutParams,
                    config = config,
                    onKey = { event ->
                        if (event.key == Key.Back) {
                            tryCloseDraggable()

                            true
                        } else {
                            false
                        }
                    },
                    onDestroy = {
                        tryCloseDraggable(true)
                        if (getFloatsCount() <= 1) {
                            stopService()
                        }
                        setFloatsCount(getFloatsCount() - 1)
                    },
                    onTap = { offset ->
                        config.expanded.onTap?.let { it(offset) }
                    },
                    onDragStart = config.expanded.onDragStart,
                    onDrag = config.expanded.onDrag,
                    onDragEnd = config.expanded.onDragEnd,
                ) {
                        when {
                            config.expanded.viewFactory != null -> config.expanded.viewFactory.let { viewFactory ->
                                AndroidView(
                                    factory = { context ->
                                        viewFactory(context) { tryCloseDraggable() }
                                    }
                                )
                            }
                            config.expanded.composable != null -> config.expanded.composable.let {composable ->
                                composable { tryCloseDraggable() }
                            }

                            else -> throw IllegalArgumentException("Either compose or view must be provided for MainFloat")
                        }
                }
            }
        }

        _expandedView.visibility = View.INVISIBLE
        compose(_expandedView, expandedLayoutParams)
    }

    private fun createCloseView() {
        val _closeView = ComposeView(context).apply {
            closeView = this
            this.setContent {
                CloseFloat(updateSize = { size ->
                    windowManager.updateViewLayout(this, layoutParams.apply {
                        width = size.width
                        height = size.height
                    })
                }) {
                        when {
                            config.close.viewFactory != null -> config.close.viewFactory.let { factory ->
                                AndroidView(
                                    factory = { context ->
                                        factory(context)
                                    }
                                )
                            }

                            config.close.composable != null -> config.close.composable.invoke()
                            else -> DefaultCloseButton()
                        }
                }
            }
        }

        _closeView.visibility = View.INVISIBLE
        compose(_closeView, closeLayoutParams)
    }

    private fun createOverlayView() {
        val _overlayView = ComposeView(context).apply {
            overlayView = this
            this.setContent {
                FullscreenOverlayFloat(
                    onTap = { tryCloseDraggable() }
                )
            }
        }

        compose(_overlayView, overlayLayoutParams)
    }

    private fun openExpanded() {
        tryRemoveView(mainView)

        if (config.expanded.tapOutsideToClose) {
            createOverlayView()
        }
        createExpandedView()
    }

    private fun tryCloseDraggable(destroy: Boolean = false) {
        tryRemoveView(mainView)
        tryRemoveView(expandedView)
        tryRemoveView(overlayView)

        if (!destroy) {
            createMainView()
        }
    }

    private fun tryRemoveView(view: ComposeView?) {
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            Log.e("error", "could not remove view")
        }
    }

    private fun compose(composable: ComposeView, layoutParams: WindowManager.LayoutParams) {
        composable.consumeWindowInsets = false
        addToComposeLifecycle(composable)
        windowManager.addView(composable, layoutParams)
        addViewToTrackingList(composable)
    }
    private fun addToComposeLifecycle(composable: ComposeView) {
        composeOwner.attachToDecorView(composable)
        if (!getIsComposeOwnerInit()) {
            composeOwner.onCreate()

            setIsComposeOwnerInit(true)
        }
        composeOwner.onStart()
        composeOwner.onResume()
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