package com.sample.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun PlayerExpandedFloat(close: () -> Unit) {
  var isPlaying by remember { mutableStateOf(false) }
  var currentPosition by remember { mutableFloatStateOf(0f) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.primaryContainer)
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      imageVector = Icons.Rounded.MusicNote,
      contentDescription = "Album art",
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(100.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      "Song Title",
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onPrimaryContainer
    )
    Text(
      "Artist Name",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onPrimaryContainer
    )
    Spacer(modifier = Modifier.height(16.dp))
    Slider(
      modifier = Modifier.widthIn(150.dp, 250.dp),
      value = currentPosition,
      onValueChange = { currentPosition = it },
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.secondary,
        activeTrackColor = MaterialTheme.colorScheme.secondary,
        inactiveTrackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
      )
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      IconButton(onClick = { /* Skip to previous */ }) {
        Icon(
          Icons.Rounded.SkipPrevious,
          contentDescription = "Previous",
          tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
      IconButton(onClick = { isPlaying = !isPlaying }) {
        Icon(
          imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
          contentDescription = if (isPlaying) "Pause" else "Play",
          tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
      IconButton(onClick = { /* Skip to next */ }) {
        Icon(
          Icons.Rounded.SkipNext,
          contentDescription = "Next",
          tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = close,
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
      )
    ) {
      Text("Minimize")
    }
  }
}