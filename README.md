# 🎈 Floating Views for Jetpack Compose

A Jetpack Compose library for adding floating views to your Android apps. Because who doesn't want their UI elements floating around like bubbles in a lava lamp?
<p align="center">
  <img src="demo.gif" alt="Floating Views Demo" width="300"/>
</p>

# Features

🚀 Simple, declarative API
🎨 Fully customizable views
🔀 Smooth animations
🔒 Handles runtime permissions
📏 Adaptive sizing
🔧 Fine-tuned control over behavior

Getting Started
1. Add the dependency
   gradleCopydependencies {
   implementation "io.github.luiisca:floating-views:1.0.0"
   }
2. Update your AndroidManifest.xml
   xmlCopy<manifest>
   <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
   <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application>
        <service
            android:name=".YourFloatingService"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Your special use case description" />
        </service>
    </application>
</manifest>
3. Create your floating service
kotlinCopyclass YourFloatingService : Service() {
    private lateinit var floatingViewsController: FloatingViewsController

    override fun onCreate() {
        super.onCreate()
        
        floatingViewsController = FloatingViewsController(
            context = this,
            stopService = { stopSelf() },
            mainFloatConfig = MainFloatConfig(
                composable = { YourFloatingContent() }
            ),
            closeFloatConfig = CloseFloatConfig(
                composable = { YourCloseButton() }
            ),
            expandedFloatConfig = ExpandedFloatConfig(
                composable = { close -> YourExpandedView(close) }
            )
        )
        
        floatingViewsController.startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        floatingViewsController.initializeNewFloatSystem()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingViewsController.shutdownAllFloatSystems()
    }
}
4. Launch your floating view
   kotlinCopyPermissionHelper.startFloatServiceIfPermitted(context, YourFloatingService::class.java)
   Customization
   Floating Views offers a buffet of customization options. Here's a taste:
   kotlinCopyMainFloatConfig(
   startPointDp = PointF(100f, 100f),
   isSnapToEdgeEnabled = true,
   onTap = { /* Your logic */ },
   onDragEnd = { /* Your logic */ }
   )

CloseFloatConfig(
closeBehavior = CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT,
followRate = 0.1f
)

ExpandedFloatConfig(
tapOutsideToClose = true,
dimAmount = 0.5f
)

# Contributing
Found a bug? Have a cool idea? Feel free to open an issue or submit a PR. We're all friends here!

Happy floating! 🎈