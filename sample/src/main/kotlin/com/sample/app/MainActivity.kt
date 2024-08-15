package com.sample.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sample.app.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // insets support
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        this.setContent {
            Theme {
                App()
            }
        }
    }
}
