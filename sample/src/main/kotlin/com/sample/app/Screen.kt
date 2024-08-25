package com.sample.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sample.app.ui.ExpandedView
import com.sample.app.ui.FloatView
import io.github.luiisca.floating.views.CloseBehavior
import io.github.luiisca.floating.views.CloseFloatConfig
import io.github.luiisca.floating.views.ExpandedFloatConfig
import io.github.luiisca.floating.views.FloatingViewsConfig
import io.github.luiisca.floating.views.MainFloatConfig
import io.github.luiisca.floating.views.helpers.FloatServiceStateManager
import io.github.luiisca.floating.views.helpers.FloatingViewsManager

@Composable
fun App() {
  Scaffold { innerPadding ->
    println(innerPadding)

    val context = LocalContext.current
    val isServiceRunning by FloatServiceStateManager.isServiceRunning.collectAsState()

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Button(onClick = {
        val config = FloatingViewsConfig(
          enableAnimations = false,
          main = MainFloatConfig(
            composable = { FloatView() },
            // Add other main float configurations here
          ),
          close = CloseFloatConfig(
            closeBehavior = CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT,
            // Add other close float configurations here
          ),
          expanded = ExpandedFloatConfig(
            composable = {close -> ExpandedView(close) },
            // Add other expanded float configurations here
          )
        )
        FloatingViewsManager.startFloatServiceIfPermitted(context, config)
      }) {
        Text(text = "Add counter")
      }
      Button(onClick = {
        val config = FloatingViewsConfig(
          main = MainFloatConfig(
            composable = { FloatView() },
            // Add other main float configurations here
          ),
          close = CloseFloatConfig(
//            closeBehavior = CloseBehavior.CLOSE_SNAPS_TO_MAIN_FLOAT,
            // Add other close float configurations here
          ),
          expanded = ExpandedFloatConfig(
            composable = {close -> ExpandedView(close) },
            // Add other expanded float configurations here
          )
        )
        FloatingViewsManager.startFloatServiceIfPermitted(context, config)
      }) {
        Text(text = "Add timer")
      }
    }
  }
}

//@Composable
//fun App() {
//  val context = LocalContext.current
//  val isServiceRunning by FloatServiceStateManager.isServiceRunning.collectAsState()
//
//  Column(
//    modifier = Modifier.fillMaxSize(),
//    verticalArrangement = Arrangement.Center,
//    horizontalAlignment = Alignment.CenterHorizontally
//  ) {
//    Button(onClick = {
//      PermissionHelper.startFloatServiceIfPermitted(context, Service::class.java)
//    }) {
//      Text(text = "Add floating view")
//    }
//
//    if (isServiceRunning) {
//      Button(
//        onClick = {
//          context.stopService(Intent(context, Service::class.java))
//        }
//      ) {
//        Text("Stop floating service")
//      }
//    }
//  }
//}