package pl.btsearch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF1565C0)
val StationColor = Color(0xFFE53935)

@Composable
fun BTSearchTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF90CAF9))
    } else {
        lightColorScheme(primary = Primary)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
