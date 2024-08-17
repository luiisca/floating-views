package com.sample.app

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.luiisca.floating.views.helpers.FloatServiceStateManager
import io.github.luiisca.floating.views.helpers.PermissionHelper

@Composable
fun App() {
  Scaffold(
    floatingActionButton = {
      val context = LocalContext.current
      val isServiceRunning by FloatServiceStateManager.isServiceRunning.collectAsState()
      val padding = 16.dp

      Column(
        horizontalAlignment = Alignment.End,
      ) {
        if (isServiceRunning) {
          FloatingActionButton(onClick = {
            context.stopService(Intent(context, Service::class.java))
          }) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Stop service",
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.size(padding))

        LargeFloatingActionButton(
          onClick = {
            PermissionHelper.startFloatServiceIfPermitted(context, Service::class.java)
          },
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Create new float",
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }
  ) { innerPadding ->
    println(innerPadding)
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