/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.floaty.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.floatingview.library.helpers.PermissionHelper
import com.floaty.app.ui.OrderViewModel

enum class CupcakeScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Flavor(title = R.string.choose_flavor),
    Pickup(title = R.string.choose_pickup_date),
    Summary(title = R.string.order_summary)
}

@Composable
fun FloatyApp(
        viewModel: OrderViewModel = viewModel(),
        navController: NavHostController = rememberNavController()
) {
  val density = LocalDensity.current
  val configuration = LocalConfiguration.current

  var txt by remember {
    mutableStateOf("")
  }

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

    Column {
      TextField(value = txt, onValueChange = {txt = it})
      Test()
    }
  }
}

@Composable
fun Test() {
  var expand by remember { mutableStateOf(false) }
  var text by remember { mutableStateOf("") }
  ExposedDropdownMenuBox(
    expanded = expand,
    onExpandedChange = { expand = !expand },
  ) {
    OutlinedTextField(
      value = text,
      label = { Text("Options") },
      onValueChange = {
        text = it
      },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expand) },
      modifier = Modifier.menuAnchor(),
    )

    ExposedDropdownMenu(
      expanded = expand,
      onDismissRequest = { expand = false },
    ) {
      listOf("foo", "bar", "baz")
        .filter { it.contains(text) }
        .forEach {
          DropdownMenuItem(
            text = { Text(it) },
            onClick = {
              text = it
              expand = false
            }
          )
        }
    }
  }
}