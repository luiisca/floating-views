package com.floaty.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpandedView(close: () -> Unit) {
  val color = remember { mutableStateOf(randomColor()) }
  var txt by remember {
    mutableStateOf("")
  }

//  Box(modifier = Modifier
////    .size(200.dp)
//    .height(511.dp)
//    .requiredWidth(400.dp)
//    .background(color.value, shape = CircleShape)
//  ) {
//
//    Column {
//      Button(onClick = { close() }) {
//        Icon(
//          imageVector = Icons.Default.Close,
//          contentDescription = "Hide expanded view",
//          modifier = Modifier.size(28.dp)
//        )
//      }
//    }
//  }
  TextField(value = txt, onValueChange = {txt = it})
}

fun randomColor() =
  Color(
    red = kotlin.random.Random.nextFloat(),
    green = kotlin.random.Random.nextFloat(),
    blue = kotlin.random.Random.nextFloat()
  )