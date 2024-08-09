import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FloatView() {
  val color = remember { mutableStateOf(randomColor()) }

  Box(modifier = Modifier
    .size(100.dp)
    .background(color.value, shape = CircleShape)
  ) {
    Button(onClick = { color.value = randomColor() }) { Text("Change Color") }
  }
}

fun randomColor() =
        Color(
                red = kotlin.random.Random.nextFloat(),
                green = kotlin.random.Random.nextFloat(),
                blue = kotlin.random.Random.nextFloat()
        )

