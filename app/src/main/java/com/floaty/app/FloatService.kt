package com.floaty.app

import FloatView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.floatingview.library.service.expandable.ExpandableBubbleService
import kotlin.random.Random

class FloatService : ExpandableBubbleService() {
/*    private lateinit var windowManager: WindowManager
    private val floatViewLifecycleOwner = FloatViewLifecycleOwner()

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()
        floatViewLifecycleOwner.onCreate()

        Log.d("❌FloatService", "onCreate")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // Start foreground service with a notification
        val channelId = "float_service_channel"
        val channel = NotificationChannel(
            channelId,
            "Floating Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Float Service")
            .setContentText("Float service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        floatViewLifecycleOwner.onStart()

        Log.d("❌FloatService", "onStartCommand")

        val params = WindowManager.LayoutParams().apply {
            this.width = WindowManager.LayoutParams.WRAP_CONTENT
            this.height = WindowManager.LayoutParams.WRAP_CONTENT
            this.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            this.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            this.format = PixelFormat.TRANSLUCENT
            this.x = Random.nextInt(0, 1000)
            this.y = Random.nextInt(0, 1000)
        }
        try {
            val floatView = composeFloatView()
//            Log.d("✅FloatView-hashcode", floatView!!.hashCode().toString())
            windowManager.addView(floatView!!, params)
        } catch (e: Exception) {
            Log.e("❌FloatService", "ERROR: ", e)

            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        floatViewLifecycleOwner.onPause()
        floatViewLifecycleOwner.onStop()
        floatViewLifecycleOwner.onDestroy()
    }

    private fun composeFloatView(): View {
        val floatView = ComposeView(this).apply {
            this.setContent {
                FloatView(windowManager, this)
            }
        }

        floatViewLifecycleOwner.attachToDecorView(floatView)

        return floatView
    }*/


    override fun startNotificationForeground() {

        val noti = NotificationHelper(this)
        noti.createNotificationChannel()
        startForeground(noti.notificationId, noti.defaultNotification())
    }

    override fun onCreate() {
        Log.d("✅MyServiceKt", "onCreate")
        super.onCreate()
        minimize()
    }

    override fun configBubble(): BubbleBuilder {
        val imgView = ViewHelper.fromDrawable(this, R.drawable.ic_rounded_blue_diamond, 60, 60)

        imgView.setOnClickListener { expand() }

        // TODO: refactor to use simpler context fn or something
        // so there is no need to manually define each builder function
        // instead directly manipulate instance props. I think apply may work, not sure though
        return BubbleBuilder(this)

            // set bubble view
//                .bubbleView(imgView)
            .triggerClickablePerimeterPx(5f)

            // or our sweetie, Jetpack Compose
            .bubbleCompose {
                FloatView()
            }
            .forceDragging(false)

            // set style for the bubble, fade animation by default
            .bubbleStyle(null)

            // set start location for the bubble, (x=0, y=0) is the top-left
            .startLocation(100, 100) // in dp
            .startLocationPx(100, 100) // in px

            // enable auto animate bubble to the left/right side when release, true by default
            .enableAnimateToEdge(true)

            // set close-bubble view
            .closeBubbleView(ViewHelper.fromDrawable(this, R.drawable.ic_close_bubble, 60, 60))

            // set style for close-bubble, null by default
            .closeBubbleStyle(null)

            // DYNAMIC_CLOSE_BUBBLE: close-bubble moving based on the bubble's location
            // FIXED_CLOSE_BUBBLE (default): bubble will automatically move to the close-bubble
            // when it reaches the closable-area
            .closeBehavior(CloseBubbleBehavior.FIXED_CLOSE_BUBBLE)

            // the more value (dp), the larger closeable-area
            .distanceToClose(100)

            // enable bottom background, false by default
            .bottomBackground(false)
            .addFloatingBubbleListener(
                object : FloatingBubbleListener {
                    override fun onFingerMove(
                        x: Float,
                        y: Float
                    ) {} // The location of the finger on the screen which triggers the
                    // movement of the bubble.

                    override fun onFingerUp(
                        x: Float,
                        y: Float
                    ) {} // ..., when finger release from bubble

                    override fun onFingerDown(
                        x: Float,
                        y: Float
                    ) {} // ..., when finger tap the bubble
                }
            )
    }

    override fun configExpandedBubble(): ExpandedBubbleBuilder? {

        val expandedView = LayoutInflater.from(this).inflate(R.layout.layout_view_test, null)
        expandedView.findViewById<View>(R.id.btn).setOnClickListener { minimize() }

        return ExpandedBubbleBuilder(this)
            //            .expandedView(expandedView)
            .expandedCompose { TestComposeView(popBack = { minimize() }) }
            .onDispatchKeyEvent {
                if (it.keyCode == KeyEvent.KEYCODE_BACK) {
                    minimize()
                }
                null
            }
            .startLocation(0, 0)
            .draggable(true)
            .style(null)
            .fillMaxWidth(false)
            .enableAnimateToEdge(true)
            .dimAmount(0.5f)
    }

}