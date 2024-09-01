package com.sample.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sample.app.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // insets support
        enableEdgeToEdge()
//        FloatingViewsManager.setNotificationProperties(
//            R.drawable.ic_launcher_foreground,
//            "Floating views running"
//        )

        setContent {
            Theme {
                App()
            }
        }
    }
}
