import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import com.floatingview.library.FloatyLifecycleOwner
import com.floatingview.library.R
import com.floatingview.library.composables.MainFloaty
import com.floatingview.library.helpers.NotificationHelper
import com.floatingview.library.helpers.toPx

data class MainFloatyConfig(
    val composable: (@Composable () -> Unit)? = null,
    val view: View? = null,
    var startPointDp: Point? = Point(0, 0),
    var startPointPx: Point? = Point(0, 0),
    var enableAnimations: Boolean? = true,
    var draggingTransitionSpec: @Composable() (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)? = null,
    var snapToEdgeTransitionSpec: @Composable() (Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)? = null,
    /**
     * determines whether `onDragEnd` callback will cause composable to be dragged to the edge of the screen
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
    val composable: (@Composable () -> Unit)? = null,
    val view: View? = null,
    val distanceToCloseDp: Int? = 100,
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

    fun startForegroundWithDefaultNotification(icon: Int = R.drawable.round_bubble_chart_24, title: String = "Floaty is running") {
        val service = context as Service
        val notificationHelper = NotificationHelper(service)
        notificationHelper.createNotificationChannel()

        service.startForeground(notificationHelper.notificationId, notificationHelper.createDefaultNotification(icon, title))
    }
    fun setup(context: Service) {
        // 1. create close and bottombackground (if active)
    }
    fun addFloaty() {
        createMainView()
    }

    private fun createCloseView() {
        // 1. call composable with some arguments (possibly mainFloaty)
        // 2. define params
        // 3. add to window
    }
    private fun createBottomView() {

    }
    private fun createMainView() {
        val startPoint = Point(
            mainFloatyConfig?.startPointDp?.x?.toPx() ?: mainFloatyConfig?.startPointPx?.x ?: 0,
            mainFloatyConfig?.startPointDp?.y?.toPx() ?: mainFloatyConfig?.startPointPx?.y ?: 0
        )
        val layoutParams = baseLayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            x = startPoint.x
            y = startPoint.y
        }

        // Composable
        val windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
        val floaty = ComposeView(context).apply {
            this.setContent {
                MainFloaty(
                    windowManager = windowManager,
                    containerView = this,
                    layoutParams = layoutParams,
                    config = mainFloatyConfig!!
                ) {
                    when {
                        mainFloatyConfig.view != null -> AndroidView(factory = { mainFloatyConfig.view })
                        mainFloatyConfig.composable != null -> mainFloatyConfig.composable.invoke()
                        else -> throw IllegalArgumentException("Either compose or view must be provided")
                    }
                }
            }
        }

        composeOwnerLifecycle(floaty)
        windowManager.addView(floaty, layoutParams)
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