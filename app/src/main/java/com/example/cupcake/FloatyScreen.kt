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
package com.example.cupcake

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.example.cupcake.data.DataSource
import com.example.cupcake.ui.OrderSummaryScreen
import com.example.cupcake.ui.OrderViewModel
import com.example.cupcake.ui.SelectOptionScreen
import com.example.cupcake.ui.StartOrderScreen

enum class CupcakeScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    Flavor(title = R.string.choose_flavor),
    Pickup(title = R.string.choose_pickup_date),
    Summary(title = R.string.order_summary)
}

/** Composable that displays the topBar and displays back button if back navigation is possible. */
@Composable
fun CupcakeAppBar(
        crrScreen: CupcakeScreen,
        canNavigateBack: Boolean,
        navigateUp: () -> Unit,
        modifier: Modifier = Modifier
) {
    TopAppBar(
            title = { Text(stringResource(id = crrScreen.title)) },
            colors =
                    TopAppBarDefaults.mediumTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
            modifier = modifier,
            navigationIcon = {
                if (canNavigateBack) {
                    IconButton(onClick = navigateUp) {
                        Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_button)
                        )
                    }
                }
            }
    )
}

@Composable
fun FloatyApp(
        viewModel: OrderViewModel = viewModel(),
        navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val uiState by viewModel.uiState.collectAsState()

    val navGraph = remember {
        navController.createGraph(CupcakeScreen.Start.name) {
            composable(CupcakeScreen.Start.name) {
                StartOrderScreen(
                        quantityOptions = DataSource.quantityOptions,
                        onNextButtonClicked = { numberCupcakes: Int ->
                            viewModel.setQuantity(numberCupcakes)
                            navController.navigate(CupcakeScreen.Flavor.name)
                        },
                        onCancelButtonClicked = {
                            navController.popBackStack(CupcakeScreen.Start.name, inclusive = true)
                        },
                        navController = navController,
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(dimensionResource(R.dimen.padding_medium))
                )
            }
            composable(CupcakeScreen.Flavor.name) {
                val context = LocalContext.current

                SelectOptionScreen(
                        subtotal = uiState.price,
                        options = DataSource.flavors.map { id -> context.resources.getString(id) },
                        onCancelButtonClicked = {
                            cancelOrderAndNavigateToStart(viewModel, navController)
                        },
                        onNextButtonClicked = { navController.navigate(CupcakeScreen.Pickup.name) },
                        onSelectionChanged = { viewModel.setFlavor(it) },
                        modifier = Modifier.fillMaxSize()
                )
            }
            composable(CupcakeScreen.Pickup.name) {
                SelectOptionScreen(
                        subtotal = uiState.price,
                        options = uiState.pickupOptions,
                        onCancelButtonClicked = {
                            cancelOrderAndNavigateToStart(viewModel, navController)
                        },
                        onNextButtonClicked = {
                            navController.navigate(CupcakeScreen.Summary.name)
                        },
                        onSelectionChanged = { viewModel.setDate(it) },
                        modifier = Modifier.fillMaxSize()
                )
            }
            composable(CupcakeScreen.Summary.name) {
                val context = LocalContext.current

                OrderSummaryScreen(
                        orderUiState = uiState,
                        onCancelButtonClicked = {
                            cancelOrderAndNavigateToStart(viewModel, navController)
                        },
                        onSendButtonClicked = { order: String, summary: String ->
                            shareOrder(context, order, summary)
                        },
                        modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Scaffold(
            topBar = {
                CupcakeAppBar(
                        crrScreen =
                                CupcakeScreen.valueOf(
                                        backStackEntry?.destination?.route
                                                ?: CupcakeScreen.Start.name
                                ),
                        canNavigateBack = navController.previousBackStackEntry != null,
                        navigateUp = { navController.navigateUp() }
                )
            }
    ) { innerPadding ->
        val context = LocalContext.current
        println(innerPadding)

        //        NavHost(
        //            navController,
        //            modifier = Modifier.padding(innerPadding),
        //            graph = navGraph
        //        )
        Box(modifier = Modifier.fillMaxSize()) {
            LargeFloatingActionButton(
                    onClick = {
                        if (Settings.canDrawOverlays(context)) {
                            context.startForegroundService(
                                    Intent(context, FloatService::class.java)
                            )
                            val isServiceRunning =
                                    (context.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
                                            .getRunningServices(Integer.MAX_VALUE)
                                            .any {
                                                it.service.className ==
                                                        FloatService::class.java.name
                                            }
                            //                        Log.d("❌FloatService", "Service running:
                            // $isServiceRunning")
                        } else {
                            val intent =
                                    Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                    )
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomEnd),
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
    }
}

private fun cancelOrderAndNavigateToStart(
        viewModel: OrderViewModel,
        navController: NavHostController
) {
    navController.popBackStack(CupcakeScreen.Start.name, inclusive = false)
    viewModel.resetOrder()
}

private fun shareOrder(context: Context, order: String, summary: String) {
    val intent =
            Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, order)
                putExtra(Intent.EXTRA_TEXT, summary)
            }
    context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.new_cupcake_order))
    )
}

