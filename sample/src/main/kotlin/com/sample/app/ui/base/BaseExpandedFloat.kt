package com.sample.app.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun BaseExpandedFloat(close: () -> Unit) {
  var txt by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf(listOf<String>()) }

  Card(
    modifier = Modifier
      .heightIn(300.dp, 511.dp)
      .widthIn(200.dp, 400.dp),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Close button
      Button(
        onClick = { close() },
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .align(Alignment.End)
          .background(MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(0.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Hide expanded view",
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Text input for note-taking
      TextField(
        value = txt,
        onValueChange = { txt = it },
        label = { Text("Enter your note") },
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Save button to add the note to the list
      Button(
        onClick = {
          if (txt.isNotBlank()) {
            notes = notes + txt
            txt = ""
          }
        },
        modifier = Modifier.align(Alignment.End)
      ) {
        Text("Save Note")
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Display saved notes
      LazyColumn {
        items(notes.size) { note ->
          Text(
            text = notes[note],
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            style = MaterialTheme.typography.bodyLarge
          )
        }
      }
    }
  }
}
