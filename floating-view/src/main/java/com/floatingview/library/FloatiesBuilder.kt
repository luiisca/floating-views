package com.floatingview.library
import android.app.Service
import android.content.Context
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
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.floatingview.library.composables.CloseFloaty
import com.floatingview.library.composables.DraggableFloat
import com.floatingview.library.composables.FullscreenOverlayFloat
import com.floatingview.library.helpers.NotificationHelper
import com.floatingview.library.helpers.getScreenSizeWithoutInsets
import com.floatingview.library.helpers.toDp
import com.floatingview.library.helpers.toPx

enum class CloseBehavior {
    MAIN_SNAPS_TO_CLOSE_FLOAT,
    CLOSE_SNAPS_TO_MAIN_FLOAT,
}
enum class DraggableType {
    MAIN,
    EXTENDED
}

/**
 * @property startPointDp Initial position of the floating view in density-independent pixels (dp).
 * @property startPointPx Initial position of the floating view in pixels (px).
 * @property draggingTransitionSpec Animation specification for dragging.
 *                                  Applied when `enableAnimations == true`.
 *
 * Default:
 *
 *                                  spring(
 *                                      dampingRatio = Spring.DampingRatioNoBouncy,
 *                                      stiffness = Spring.StiffnessHigh
 *                                  )
 * @property snapToEdgeTransitionSpec Animation specification for snapping to screen edge.
 *                                    Applied when `enableAnimations == true && isSnapToEdgeEnabled`.
 *
 * Default:
 *
 *                                    spring(
 *                                      dampingRatio = Spring.DampingRatioMediumBouncy,
 *                                      stiffness = Spring.StiffnessMedium
 *                                    )
 * @property snapToCloseTransitionSpec Animation specification for snapping to close float.
 *                                     Applied when `enableAnimations == true &&
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
sealed interface FloatyConfig {
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
data class MainFloatyConfig(
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
): FloatyConfig

/**
 * @property enabled Determines if the expanded floating view is active.
 * @property tapOutsideToClose Determines if tapping outside of expanded view should close it.
 * @property composable Jetpack Compose function defining the content of the expanded view.
 * Call hide to remove expanded view and add main view to windowManager again
 * @property viewFactory A function that creates an Android View to be displayed in the floating view.
 * This is an alternative to using a Composable.
 * Call hide to remove expanded view and add main view to windowManager again
 * @property dimAmount This is the amount of dimming to apply behind expanded view. Range is from 1.0 for completely opaque to 0.0 for no dim.
 */
data class ExpandedFloatyConfig(
    val enabled: Boolean = true,
    val tapOutsideToClose: Boolean = true,
    val dimAmount: Float = 0.5f,
    val composable: (@Composable (close: () -> Unit) -> Unit)? = null,
    val viewFactory: ((context: Context, hide:() -> Unit) -> View)? = null,
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
): FloatyConfig


/**
 * Configuration class for the close floating view.
 *
 * @property enabled Determines if the close floating view is active.
 * @property composable Jetpack Compose function defining the content of the close floating view.
 * @property viewFactory A function that creates an Android View to be displayed in the close floating view.
 * This is an alternative to using a Composable.
 * @property startPointDp The initial position of the close floating view in density-independent pixels (dp).
 * @property startPointPx The initial position of the close floating view in pixels (px).
 * @property mountThresholdDp The drag distance required to show the close float, in density-independent pixels (dp).
 *                            A larger value requires more dragging before the close float becomes visible.
 * @property mountThresholdPx The drag distance required to show the close float, in pixels (px).
 *                            A larger value requires more dragging before the close float becomes visible.
 * @property closingThresholdDp The distance (in density-independent pixels) between the main float
 * and the close float that triggers the `closeBehavior`.
 * @property closingThresholdPx The distance (in pixels) between the main float and the close float
 * that triggers the `closeBehavior`.
 * @property bottomPaddingDp The bottom padding for the close float in density-independent pixels (dp).
 *                           Applied only when neither `startPointDp` nor `startPointPx` is specified.
 * @property bottomPaddingPx The bottom padding for the close float in pixels (px).
 *                           Applied only when neither `startPointDp` nor `startPointPx` is specified.
 * @property draggingTransitionSpec Defines the animation for dragging the close float.
 * Used when `enableAnimations == true && closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.
 *
 * Default:
 *
 *                                  spring(
 *                                      dampingRatio = Spring.DampingRatioNoBouncy,
 *                                      stiffness = Spring.StiffnessHigh
 *                                  )
 * @property snapToMainTransitionSpec Defines the animation for the close float snapping to the main float.
 *                                    Used when `enableAnimations == true && closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.
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
 *                      Only used when `closeConfig.closeBehavior == CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.
 *
 *                      Default: 0.1f
 */
data class CloseFloatyConfig(
    val enabled: Boolean = true,
    val composable: (@Composable () -> Unit)? = null,
    val viewFactory: ((Context) -> View)? = null,
    var startPointDp: PointF? = null,
    var startPointPx: PointF? = null,
    // TODO: figure how to still handle default values without ignoring custom ones
    val mountThresholdDp: Float = 1f,
    val mountThresholdPx: Float = 5f,
    val closingThresholdDp: Float = 100f,
    val closingThresholdPx: Float = 100f,
    val bottomPaddingDp: Float = 16f,
    val bottomPaddingPx: Float = 48f,
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

class FloatiesBuilder(
    private val context: Context,
    private val enableAnimations: Boolean = true,
    private val mainFloatyConfig: MainFloatyConfig = MainFloatyConfig(),
    private val closeFloatyConfig: CloseFloatyConfig = CloseFloatyConfig(),
    private val expandedFloatyConfig: ExpandedFloatyConfig = ExpandedFloatyConfig(),
) {
    private val composeOwner = FloatyLifecycleOwner()
    private var isComposeOwnerInit: Boolean = false
    private var isCloseViewCreated: Boolean = false
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager

    private lateinit var closeContainerView: ComposeView
    private lateinit var closeLayoutParams: WindowManager.LayoutParams

    fun startForegroundWithDefaultNotification(icon: Int = R.drawable.round_bubble_chart_24, title: String = "Floaty is running") {
        val service = context as Service
        val notificationHelper = NotificationHelper(service)
        notificationHelper.createNotificationChannel()

        service.startForeground(notificationHelper.notificationId, notificationHelper.createDefaultNotification(icon, title))
    }
    fun addFloaty() {
        if (closeFloatyConfig.enabled && !isCloseViewCreated) {
            createCloseView()
        }

//        CreateFloatViews(
//            context,
//            enableAnimations,
//            mainFloatyConfig,
//            expandedFloatyConfig,
//            closeFloatyConfig,
//            addToComposeLifecycle = { composable ->
//                addToComposeLifecycle(composable)
//            },
//            closeContainerView,
//            closeLayoutParams
//        )
    }


    private fun createCloseView() {
        closeLayoutParams = baseLayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }

        val closeFloaty = ComposeView(context).apply {
            closeContainerView = this
            this.setContent {
                CloseFloaty(
                    windowManager = windowManager,
                    containerView = this,
                    layoutParams = closeLayoutParams,
                ) {
                    when {
                        closeFloatyConfig.viewFactory != null -> closeFloatyConfig.viewFactory.let { factory ->
                            AndroidView(
                                factory = { context ->
                                    factory(context)
                                }
                            )
                        }
                        closeFloatyConfig.composable != null -> closeFloatyConfig.composable.invoke()
                        else -> DefaultCloseButton()
                    }
                }
            }
        }

        addToComposeLifecycle(closeFloaty)
    }

    fun addToComposeLifecycle(composable: ComposeView) {
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
    private val mainFloatyConfig: MainFloatyConfig = MainFloatyConfig(),
    private val expandedFloatyConfig: ExpandedFloatyConfig,
    private val closeFloatyConfig: CloseFloatyConfig,
    private val addToComposeLifecycle: (composable: ComposeView) -> Unit,
    private val closeContainerView: ComposeView,
    private val closeLayoutParams: WindowManager.LayoutParams,
) {
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
    var mainView: ComposeView? = null
    var expandedView: ComposeView? = null
    var overlayView: ComposeView? = null
    val mainStartPoint = Point(
            (mainFloatyConfig.startPointDp?.x?.toPx() ?: mainFloatyConfig.startPointPx?.x ?: 0f).toInt(),
            (mainFloatyConfig.startPointDp?.y?.toPx() ?: mainFloatyConfig.startPointPx?.y ?: 0f).toInt()
        )
    val expandedStartPoint = Point(
        (expandedFloatyConfig.startPointDp?.x?.toPx() ?: expandedFloatyConfig.startPointPx?.x ?: 0f).toInt(),
        (expandedFloatyConfig.startPointDp?.y?.toPx() ?: expandedFloatyConfig.startPointPx?.y ?: 0f).toInt()
    )
    val mainLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        x = mainStartPoint.x
        y = mainStartPoint.y
    }
    val expandedLayoutParams = baseLayoutParams().apply {
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
        dimAmount = expandedFloatyConfig.dimAmount
        x = expandedStartPoint.x
        y = expandedStartPoint.y
    }
    val overlayLayoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
    }

    var isOverlayVisible = false
    var isExpandedVisible = false
    var isMainVisible = false


    init {
        createMainView()
    }

    private fun createMainView() {
        val mainFloaty = ComposeView(context).apply {
            mainView = this
            this.setContent {
                DraggableFloat(
                    type = DraggableType.MAIN,
                    windowManager = windowManager,
                    containerView = mainView!!,
                    closeContainerView = closeContainerView,
                    layoutParams = mainLayoutParams,
                    closeLayoutParams = closeLayoutParams,
                    enableAnimations = enableAnimations,
                    mainConfig = mainFloatyConfig,
                    closeConfig = closeFloatyConfig,
                    expandedConfig = expandedFloatyConfig,
                    openExpandedView = { openExpanded() },
                    onClose = { closeMain() }
                ) {
                    when {
                        mainFloatyConfig.viewFactory != null -> mainFloatyConfig.viewFactory.let { viewFactory ->
                            AndroidView(
                                factory = { context ->
                                    viewFactory(context)
                                }
                            )
                        }
                        mainFloatyConfig.composable != null -> mainFloatyConfig.composable.invoke()
                        else -> throw IllegalArgumentException("Either compose or view must be provided for MainFloaty")
                    }
                }
            }
        }

        addToComposeLifecycle(mainFloaty)
        windowManager.addView(mainFloaty, mainLayoutParams)
        isMainVisible = true
    }

    private fun createExpandedView() {
        val expandedFloaty = ComposeView(context).apply {
            expandedView = this
            this.setContent {
                DraggableFloat(
                    type = DraggableType.EXTENDED,
                    windowManager = windowManager,
                    containerView = expandedView!!,
                    closeContainerView = closeContainerView,
                    layoutParams = expandedLayoutParams,
                    closeLayoutParams = closeLayoutParams,
                    enableAnimations = enableAnimations,
                    mainConfig = mainFloatyConfig,
                    closeConfig = closeFloatyConfig,
                    expandedConfig = expandedFloatyConfig,
                    onClose = {
                        closeExpanded(openMainAfter = false)
                    }
                ) {
                    when {
                        expandedFloatyConfig.viewFactory != null -> expandedFloatyConfig.viewFactory.let { viewFactory ->
                            AndroidView(
                                factory = { context ->
                                    viewFactory(context) { closeExpanded() }
                                }
                            )
                        }
                        expandedFloatyConfig.composable != null -> expandedFloatyConfig.composable.let {composable ->
                            composable { closeExpanded() }
                        }

                        else -> throw IllegalArgumentException("Either compose or view must be provided for MainFloaty")
                    }
                }
            }
        }

        addToComposeLifecycle(expandedFloaty)
        windowManager.addView(expandedView, expandedLayoutParams)
        isExpandedVisible = true
    }

    private fun createOverlayView() {
        val overlayFloat = ComposeView(context).apply {
            overlayView = this
            this.setContent {
                FullscreenOverlayFloat(
                    onTap = { closeExpanded() }
                )
            }
        }

        addToComposeLifecycle(overlayFloat)
        windowManager.addView(overlayView, overlayLayoutParams)
        isOverlayVisible = true
    }

    private fun openExpanded() {
        if (mainView != null && !isExpandedVisible && !isOverlayVisible) {
            windowManager.removeView(mainView)
            if (expandedFloatyConfig.tapOutsideToClose) {
                createOverlayView()
            }
            createExpandedView()

            isMainVisible = false
        }
    }
    private fun closeMain() {
        if (mainView != null && isMainVisible) {
            windowManager.removeView(mainView)
            isMainVisible = false
        }
    }
    private fun closeExpanded(openMainAfter: Boolean = true) {
        if (expandedView != null
            && overlayView != null
            && !isMainVisible
            && isExpandedVisible
            ) {
            windowManager.removeView(expandedView)
            isExpandedVisible = false
            if (expandedFloatyConfig.tapOutsideToClose) {
                closeOverlay()
            }

            if (openMainAfter) {
                createMainView()
            }
        }
    }
    private fun closeOverlay() {
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
            contentDescription = "Close floaty view",
            modifier = Modifier.size(60.dp)
        )
    }
}

private fun baseLayoutParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams().apply {
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT

        gravity = Gravity.TOP or Gravity.LEFT
        format = PixelFormat.TRANSLUCENT

        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
}