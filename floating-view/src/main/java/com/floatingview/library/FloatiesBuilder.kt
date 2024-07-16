package com.floatingview.library

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
import androidx.compose.animation.core.Transition
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
import com.floatingview.library.composables.CloseFloaty
import com.floatingview.library.composables.MainFloaty
import com.floatingview.library.helpers.NotificationHelper
import com.floatingview.library.helpers.toPx

data class MainFloatyConfig(
    val composable: (@Composable () -> Unit)? = null,
    val view: View? = null,
    var startPointDp: PointF? = PointF(0f, 0f),
    var startPointPx: PointF? = PointF(0f, 0f),
    var enableAnimations: Boolean? = true,
    var draggingTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)? = null,
    var snapToEdgeTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)? = null,
    /**
     * drag floaty to closest screen edge
     */
    var isSnapToEdgeEnabled: Boolean? = true,
    var onTap: ((Offset) -> Unit)? = null,
    var onDragStart: ((offset: Offset) -> Unit)? = null,
    var onDrag: ((
        change: PointerInputChange,
        dragAmount: Offset,
        newX: Int,
        newY: Int,
        animatedX: Int?,
        animatedY: Int?) -> Unit)? = null,
    var onDragEnd: (() -> Unit)? = null,
)
data class CloseFloatyConfig(
    val enabled: Boolean? = true,
    val composable: (@Composable () -> Unit)? = null,
    val view: View? = null,
    var startPointDp: PointF? = null,
    var startPointPx: PointF? = null,
    val openThresholdDp: Float? = 5f,
    val openThresholdPx: Float? = 5f,
    val closeThresholdDp: Float? = 100f,
    val closeThresholdPx: Float? = 100f,
    var snapToCloseTransitionSpec: (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)? = null,
    /**
     * drag floaty to closing area when distance between them is over `CloseFloatyConfig.openThreshold`
     */
    var isSnapToCloseEnabled: Boolean? = true,
)
data class BottomBackConfig(
    val composable: (@Composable () -> Unit)? = null,
    val view: View? = null,
    val enabled: Boolean? = true
)

class FloatiesBuilder(
    private val context: Context,
    private val mainFloatyConfig: MainFloatyConfig? = MainFloatyConfig(),
    private val closeFloatyConfig: CloseFloatyConfig? = CloseFloatyConfig(),
    private val bottomBackConfig: BottomBackConfig? = BottomBackConfig()
) {
    private val composeOwner = FloatyLifecycleOwner()
    private var isComposeOwnerInit: Boolean = false
    private val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
    private lateinit var closeContainerView: ComposeView
    private lateinit var closeLayoutParams: WindowManager.LayoutParams

    fun startForegroundWithDefaultNotification(icon: Int = R.drawable.round_bubble_chart_24, title: String = "Floaty is running") {
        val service = context as Service
        val notificationHelper = NotificationHelper(service)
        notificationHelper.createNotificationChannel()

        service.startForeground(notificationHelper.notificationId, notificationHelper.createDefaultNotification(icon, title))
    }
    fun setup(context: Service) {
        // 1. create close and bottombackground (if active)
        if (closeFloatyConfig?.enabled == true) {
            createCloseView()
        }
    }
    fun addFloaty() {
        createMainView()
    }

    private fun createCloseView() {
        val startPoint = PointF(
            closeFloatyConfig?.startPointDp?.x?.toPx() ?: closeFloatyConfig?.startPointPx?.x ?: 0f,
            closeFloatyConfig?.startPointDp?.y?.toPx() ?: closeFloatyConfig?.startPointPx?.y ?: 0f
        )

        val hasCustomPos = closeFloatyConfig?.startPointDp != null || closeFloatyConfig?.startPointPx != null
        closeLayoutParams = baseLayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            if (hasCustomPos) {
                x = startPoint.x.toInt()
                y = startPoint.y.toInt()
            }
        }

        val closeFloaty = ComposeView(context).apply {
            closeContainerView = this
            this.visibility = if (hasCustomPos) View.VISIBLE else View.INVISIBLE
            this.setContent {
                CloseFloaty(
                    windowManager = windowManager,
                    containerView = this,
                    layoutParams = closeLayoutParams,
                    isInvisible = !hasCustomPos,
                ) {
                    when {
                        closeFloatyConfig?.view != null -> AndroidView(factory = { closeFloatyConfig.view })
                        closeFloatyConfig?.composable != null -> closeFloatyConfig.composable.invoke()
                        else -> DefaultCloseButton()
                    }
                }
            }
        }

        composeOwnerLifecycle(closeFloaty)
    }

    private fun createBottomView() {

    }
    private fun createMainView() {
        val startPoint = PointF(
            mainFloatyConfig?.startPointDp?.x?.toPx() ?: mainFloatyConfig?.startPointPx?.x ?: 0f,
            mainFloatyConfig?.startPointDp?.y?.toPx() ?: mainFloatyConfig?.startPointPx?.y ?: 0f
        )
        val layoutParams = baseLayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            x = startPoint.x.toInt()
            y = startPoint.y.toInt()
        }

        // Composable
        val mainFloaty = ComposeView(context).apply {
            this.setContent {
                MainFloaty(
                    windowManager = windowManager,
                    containerView = this,
                    closeContainerView = closeContainerView,
                    layoutParams = layoutParams,
                    closeLayoutParams = closeLayoutParams,
                    config = mainFloatyConfig!!,
                    closeConfig = closeFloatyConfig!!,
                ) {
                    when {
                        mainFloatyConfig.view != null -> AndroidView(factory = { mainFloatyConfig.view })
                        mainFloatyConfig.composable != null -> mainFloatyConfig.composable.invoke()
                        else -> throw IllegalArgumentException("Either compose or view must be provided for MainFloaty")
                    }
                }
            }
        }

        composeOwnerLifecycle(mainFloaty)
        windowManager.addView(mainFloaty, layoutParams)
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

    private fun composeOwnerLifecycle(composable: ComposeView) {
        composeOwner.attachToDecorView(composable)
        if (!isComposeOwnerInit) {
            composeOwner.onCreate()

            isComposeOwnerInit = true
        }
        composeOwner.onStart()
        composeOwner.onResume()
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

