package com.sample.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StopwatchFloat() {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    // Update the elapsed time in a loop when the stopwatch is running
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10)
            elapsedTime += 10
        }
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable {
                isRunning = !isRunning
                if (!isRunning) {
                    coroutineScope.launch {
                        delay(1000)
                        elapsedTime = 0L
                    }
                }
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTime(elapsedTime),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isRunning) "Tap to Stop" else "Tap to Start",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp
            )
        }
    }
}

fun formatTime(timeMillis: Long): String {
    val minutes = timeMillis / 60000
    val seconds = (timeMillis % 60000) / 1000
    val milliseconds = timeMillis % 1000 / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, milliseconds)
}