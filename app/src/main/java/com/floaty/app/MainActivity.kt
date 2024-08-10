package com.floaty.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.floaty.app.ui.theme.CupcakeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // insets support
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        this.setContent {
            CupcakeTheme() {
                FloatyApp()
            }
        }
    }
}
