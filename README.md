# 🎈 Floating Views for Jetpack Compose

A Jetpack Compose library for adding floating views to your Android apps. Because who doesn't want their UI elements floating around like bubbles in a lava lamp?
<p align="center">
    <img src="https://github.com/user-attachments/assets/482c11ad-999a-47cd-ba37-a3a3099d67cc" alt="Floating Views Demo" width="300"/>
</p>

## Features

- 🚀 Declarative API using Jetpack Compose for easy integration
- 🌊 Smooth animations with customizable transition specs
- 🧲 Smart edge-snapping behavior for improved UX
- 🎛️ Fine-grained control over float behavior and interactions
- 📏 Adaptive sizing to fit content and screen dimensions
- 🔒 Built-in runtime permission handling for overlay views
- 🎨 Flexible view creation with Jetpack Compose or traditional Views
- 🔌 Easy service start/stop controls from any part of your app

## Requirements

- Android SDK version 21+
- Maven central

## Quick Start

### 1. Import to project

Add the following dependency to your app's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("io.github.luiisca.floating.views:1.0.5")
}
```
>[sample/build.gradle.kts:55](https://github.com/luiisca/floating-views/blob/5c5cc9b03b1aa4cc37c964104c2eac6cb49b0d65/sample/build.gradle.kts#L55)
> 

### 2. Update your AndroidManifest.xml

Add the following to your `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permission to draw over other apps -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <!-- Permission to run a foreground service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <!-- Permission for special use foreground service (Android 14+) -->
    <!-- You can adjust this based on your use case. See: https://developer.android.com/develop/background-work/services/fg-service-types -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Sample"
        tools:targetApi="33">

        <!-- Foreground service declaration -->
        <!-- This service is crucial for drawing content over other apps -->
        <!-- The foregroundServiceType should match your use case. Options include:
             - dataSync, mediaPlayback, phoneCall, location, connectedDevice, mediaProjection, camera, microphone
             - If none of these fit, use "specialUse" as shown here -->
        <service
            android:name=".Service"
            android:enabled="true"
            android:foregroundServiceType="specialUse">
            <!-- Required for "specialUse" type. Describe your use case for app store review -->
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Describe your special use case here" />
        </service>

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Sample"
            android:windowSoftInputMode="adjustResize"> <!-- insets support -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```
>[sample/.../AndroidManifest.xml](https://github.com/luiisca/floating-views/blob/5c5cc9b03b1aa4cc37c964104c2eac6cb49b0d65/sample/src/main/AndroidManifest.xml)

### 3. Launch and manage your custom floating view

This library allows you to create a highly customizable floating view with three main components: main float, close float, and expanded float. Here's how to configure and launch your floating view:

3.1. **Create a custom configuration**: Use `FloatingViewsConfig` to define the behavior of a single floating view and its components.
```kotlin
val config = FloatingViewsConfig(
  enableAnimations = true, // Enable or disable animations
  main = MainFloatConfig(
    composable = { YourMainFloatContent() },
    // Configure main float behavior
  ),
  close = CloseFloatConfig(
    composable = { YourCloseFloatContent() },
    // Configure close float behavior
  ),
  expanded = ExpandedFloatConfig(
    composable = { close -> YourExpandedFloatContent(close) },
    // Configure expanded float behavior
  )
)
```
Each config object (`MainFloatConfig`, `CloseFloatConfig`, `ExpandedFloatConfig`) allows you to customize various aspects of that specific component of your floating view.

3.2. **Launch the floating view**: Use `FloatingViewsManager.startFloatServiceIfPermitted()` to start the service and display your custom floating view.
```kotlin
FloatingViewsManager.startFloatServiceIfPermitted(context, config)
```

3.3 **Monitor service state**: Subscribe to `FloatServiceStateManager.isServiceRunning` to be notified when the service starts or stops.
```kotlin
@Composable
fun YourComposable() {
	val isServiceRunning by FloatServiceStateManager.isServiceRunning.collectAsState()

    // Use isServiceRunning to update your UI
    if (isServiceRunning) {
        Text("Service is running")
    } else {
        Text("Service is not running")
    }
}
```

Here's an example demonstrating how to create and launch multiple, distinct floating views while monitoring the service state:

```kotlin
@Composable
fun App() {
  Scaffold { innerPadding ->
    println(innerPadding)

    val context = LocalContext.current
    val isServiceRunning by FloatServiceStateManager.isServiceRunning.collectAsState()

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Base Float
      Button(
        modifier = Modifier.widthIn(min = 200.dp, max = 300.dp),
        onClick = {
          val config = FloatingViewsConfig(
            enableAnimations = false,
            main = MainFloatConfig(
              composable = { BaseFloat() },
              // Add other main float configurations here
            ),
            close = CloseFloatConfig(
              closeBehavior = CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT,
              // Add other close float configurations here
            ),
            expanded = ExpandedFloatConfig(
              composable = {close -> BaseExpandedFloat(close) },
              // Add other expanded float configurations here
            )
          )

          // Launch a new music player floating view
          FloatingViewsManager.startFloatServiceIfPermitted(context, config)
        }
      ) {
        Text(text = "Base", style = MaterialTheme.typography.bodyLarge)
      }
	 //...

      // Display a button to stop the service if it's running
      if (isServiceRunning) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          modifier = Modifier.widthIn(min = 200.dp, max = 300.dp),
          onClick = {
            FloatingViewsManager.stopFloatService(context)
          }
        ) {
          Text(text = "Remove all", style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}
```
>[sample/.../Screen.kt](https://github.com/luiisca/floating-views/blob/5c5cc9b03b1aa4cc37c964104c2eac6cb49b0d65/sample/src/main/kotlin/com/sample/app/Screen.kt#L74)

## Customization

The `FloatingViewsController` offers extensive customization options. Here's an overview of key features, followed by a complete reference.

### Key Customization Examples

1. Main Floating View:
```kotlin
val mainFloatConfig = MainFloatConfig(
    composable = { /* Your content */ },
    startPointDp = PointF(100f, 100f),
    isSnapToEdgeEnabled = true,
    onTap = { /* Handle tap */ }
)
```
2. Expanded View:
```kotlin
val expandedFloatConfig = ExpandedFloatConfig(
    enabled = true,
    tapOutsideToClose = true,
    dimAmount = 0.5f,
    composable = { close -> /* Expanded content */ }
)
```
3. Close View:
```kotlin
val closeFloatConfig = CloseFloatConfig(
    enabled = true,
    composable = { /* Custom close button */ },
    closeBehavior = CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT
)
```

### Complete Configuration Reference

Below is a comprehensive list of all configuration options:

#### MainFloatConfig

| Option                      | Description                                                                                                                                                                                      | Type                                                                                            | Default                                                                                      |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `composable`                | The Composable to be rendered inside the floating view. If null, `viewFactory` is used.                                                                                                          | `(@Composable () -> Unit)?`                                                                     | `null`                                                                                       |
| `viewFactory`               | Factory function to create a traditional Android view inside the floating view. If null, `composable` is used.                                                                                   | `((Context) -> View)?`                                                                          | `null`                                                                                       |
| `startPointDp`              | Initial position of the floating view in density-independent pixels (dp). When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used.                                     | `PointF?`                                                                                       | `null`                                                                                       |
| `startPointPx`              | Initial position of the floating view in pixels (px). When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used.                                                         | `PointF?`                                                                                       | `null`                                                                                       |
| `draggingTransitionSpec`    | Animation spec for dragging transitions. Applied when `FloatingViewsController.enableAnimations` is `true`.                                                                                      | `(Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)`                                    | `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)`       |
| `snapToEdgeTransitionSpec`  | Animation spec for snapping to the screen edge. Applied when `FloatingViewsController.enableAnimations` is `true` and `isSnapToEdgeEnabled` is `true`.                                           | `(Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)`                                    | `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)` |
| `snapToCloseTransitionSpec` | Animation spec for snapping to close float. Applied when `FloatingViewsController.enableAnimations` is `true` and `CloseFloatConfig.closeBehavior` is `CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT`. | `(Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)`                                    | `spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)`    |
| `isSnapToEdgeEnabled`       | If `true`, the floating view will snap to the nearest screen edge when dragging ends.                                                                                                            | `Boolean`                                                                                       | `true`                                                                                       |
| `onTap`                     | Callback triggered when the floating view is tapped.                                                                                                                                             | `((Offset) -> Unit)?`                                                                           | `null`                                                                                       |
| `onDragStart`               | Callback triggered when dragging of the floating view begins.                                                                                                                                    | `((offset: Offset) -> Unit)?`                                                                   | `null`                                                                                       |
| `onDrag`                    | Callback triggered during dragging of the floating view.                                                                                                                                         | `((PointerInputChange, dragAmount: Offset, newPoint: Point, newAnimatedPoint: Point?) -> Unit)?` | `null`                                                                                       |
| `onDragEnd`                 | Callback triggered when dragging of the floating view ends.                                                                                                                                      | `(() -> Unit)?`                                                                                 | `null`                                                                                       |

#### ExpandedFloatConfig

| Option                              | Description                                                                                                                                        | Type                                               | Default |
| ----------------------------------- |----------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------| ------- |
| `enabled`                           | If `true`, enables expanded view mode.                                                                                                             | `Boolean`                                          | `true`  |
| `tapOutsideToClose`                 | If `true` adds an overlay view that will close expanded view when tapped.                                                                          | `Boolean`                                          | `true`  |
| `dimAmount`                         | Controls the dimming amount of the background when the view is expanded. Range is from 1.0 for completely opaque to 0.0 for no dim.                | `Float`                                            | `0.5f`  |
| `composable`                        | The Composable to be rendered inside the expanded view. If null, `viewFactory` is used. Call close to remove expanded view.                        | `(@Composable (close: () -> Unit) -> Unit)?`       | `null`  |
| `viewFactory`                       | Factory function to create a traditional Android view inside the expanded view. If null, `composable` is used. Call close to remove expanded view. | `((context: Context, close:() -> Unit) -> View)?`  | `null`  |
| All properties from MainFloatConfig | All properties from `MainFloatConfig` are also available here.                                                                                     |                                                    |         |

#### CloseFloatConfig

| Option                     | Description                                                                                                                                                                                                                                | Type                                                         | Default                                                                                |
| -------------------------- |--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `enabled`                  | If `true`, enables the close float behavior.                                                                                                                                                                                               | `Boolean`                                                    | `true`                                                                                 |
| `composable`               | The Composable to be rendered inside the close floating view. If null, `viewFactory` is used.                                                                                                                                              | `(@Composable () -> Unit)?`                                  | `null`                                                                                 |
| `viewFactory`              | Factory function to create a traditional Android view inside the close floating view. If null, `composable` is used.                                                                                                                       | `((Context) -> View)?`                                       | `null`                                                                                 |
| `startPointDp`             | Initial position of the close floating view in density-independent pixels (dp). When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used.                                                                         | `PointF?`                                                    | `null`                                                                                 |
| `startPointPx`             | Initial position of the close float in pixels (px). When neither `startPointDp` nor `startPointPx` are provided `PointF(0,0)` is used.                                                                                                     | `PointF?`                                                    | `null`                                                                                 |
| `mountThresholdDp`         | Dragging distance required to show the close float, in density-independent pixels (dp). When neither `mountThresholdDp` nor `mountThresholdPx` are provided `1.dp` is used.                                                                | `Float?`                                                     | `null`                                                                                 |
| `mountThresholdPx`         | Dragging distance required to show the close float, in pixels (px). When neither `mountThresholdDp` nor `mountThresholdPx` are provided `1.                                                                                                | `Float?`                                                     | `null`                                                                                 |
| `closingThresholdDp`       | Dragging distance (in density-independent pixels) between the main float and the close float that triggers a `CloseFloatConfig.closeBehavior`. When neither `closingThresholdDp` nor `closingThresholdPx` are provided `100.dp` is used.   | `Float?`                                                     | `null`                                                                                 |
| `closingThresholdPx`       | Dragging distance (in pixels) between the main float and the close float that triggers a `CloseFloatConfig.closeBehavior`. When neither `closingThresholdDp` nor `closingThresholdPx` are provided `100.dp` is used.                       | `Float?`                                                     | `null`                                                                                 |
| `bottomPaddingDp`          | Bottom padding for the close float in density-independent pixels (dp). When neither `bottomPaddingDp` nor `bottomPaddingPx` are provided `16.dp` is used.                                                                                  | `Float?`                                                     | `null`                                                                                 |
| `bottomPaddingPx`          | Bottom padding for the close float in pixels (px). When neither `bottomPaddingDp` nor `bottomPaddingPx` are provided `16.dp` is used.                                                                                                      | `Float?`                                                     | `null`                                                                                 |
| `draggingTransitionSpec`   | Animation specification for dragging. Applied when `FloatingViewsController.enableAnimations` is `true` and `CloseFloatConfig.closeBehavior` is `CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.                                                 | `(Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)` | `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)` |
| `snapToMainTransitionSpec` | Animation specification for snapping to main float. Applied when `FloatingViewsController.enableAnimations` is `true` and `CloseFloatConfig.closeBehavior` is `CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.                                   | `(Transition.Segment<Point>.() -> FiniteAnimationSpec<Int>)` | `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)` | 
| `closeBehavior`            | Determines how the close float interacts with the main float.                                                                                                                                                                              | `CloseBehavior?`                                             | `CloseBehavior.MAIN_SNAPS_TO_CLOSE_FLOAT`                                              |
| `followRate`               | Defines the rate at which the close float follows the main float when dragged. Only used when `CloseFloatConfig.closeBehavior` is `CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT`.                                                               | `Float`                                                      | `0.1f`                                                                                 |

## Contributing

Found a bug? Have a cool idea? Feel free to open an issue or submit a PR. We're all friends here!

Happy floating! 🎈