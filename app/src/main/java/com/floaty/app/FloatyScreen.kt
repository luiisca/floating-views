package com.floaty.app

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatingview.library.helpers.PermissionHelper

@Composable
fun FloatyApp() {
  Scaffold(
    floatingActionButton = {
      val context = LocalContext.current

      LargeFloatingActionButton(
        onClick = {
          PermissionHelper.startFloatyServiceIfPermitted(context, FloatyService::class.java)
        },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create new floaty",
          modifier = Modifier.size(28.dp)
        )
      }
    }
  ) { innerPadding ->
    println(innerPadding)
  }
}