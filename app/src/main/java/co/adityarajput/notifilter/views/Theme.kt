package co.adityarajput.notifilter.views

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = darkColorScheme(
    primary = Color(0xFF7C70FF), // Purple
    secondary = Color(0xFF70FFA4), // Green
    tertiary = Color(0xFFFF7070), // Red
)

val Orange = Color(0xFFffad7d)

@Composable
fun Theme(content: @Composable () -> Unit) =
    MaterialTheme(ColorScheme, MaterialTheme.shapes, Typography(), content)
