import android.app.Service
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import com.floatingview.library.FloatyLifecycleOwner
import com.floatingview.library.R
import com.floatingview.library.composables.MainFloaty
import com.floatingview.library.helpers.NotificationHelper
import com.floatingview.library.helpers.toPx

interface FloatiesConfig {
    var composable: (@Composable () -> Unit)?
    var view: View?
    var closeView: View?
    var startPointDp: Point?
    var startPointPx: Point?
    var isAnimateToEdgeEnabled: Boolean
    var distanceToCloseDp: Int
    var isBottomBackgroundEnabled: Boolean
}

class FloatiesBuilder(private val context: Context): FloatiesConfig {
    override var composable: (@Composable () -> Unit)? = null
    override var view: View? = null
    override var closeView: View? = null
    override var startPointDp: Point? = Point(0, 0)
    override var startPointPx: Point? = Point(0, 0)
    override var isAnimateToEdgeEnabled: Boolean = true
    override var distanceToCloseDp: Int = 100
    override var isBottomBackgroundEnabled: Boolean = false

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
            startPointDp?.x?.toPx() ?: startPointPx?.x ?: 0,
            startPointDp?.y?.toPx() ?: startPointPx?.y ?: 0
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
                    onTap = {Log.d("//library - onTap", "tapped!")},
                    onMove = {x, y -> Log.d("||library - onMove", "$x - $y")},
                ) {
                    when {
                        view != null -> AndroidView(factory = { view!! })
                        composable != null -> composable!!()
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

    companion object {
        inline operator fun invoke(
                context: Context,
                block: FloatiesConfig.() -> Unit
        ): FloatiesBuilder {
            return FloatiesBuilder(context).apply(block)
        }
    }
}