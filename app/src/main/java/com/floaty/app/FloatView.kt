import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withContext

@Composable
fun FloatView(windowManager: WindowManager, view: View) {
  val color = remember { mutableStateOf(randomColor()) }

  Box(modifier = Modifier
    .size(100.dp)
    .background(color.value, shape = CircleShape)) {
      Button(onClick = { color.value = randomColor() }) { Text("Change Color") }
      FloatingActionButton(
        onClick = {
          try {
            windowManager.removeView(view)
          } catch (e: Exception) {
            e.printStackTrace()
          }
        },
        modifier = Modifier.padding(8.dp),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete floaty",
          modifier = Modifier.size(14.dp)
        )
      }
  }
}

fun randomColor() =
        Color(
                red = kotlin.random.Random.nextFloat(),
                green = kotlin.random.Random.nextFloat(),
                blue = kotlin.random.Random.nextFloat()
        )

