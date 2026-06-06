package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CinemaAmber,               // D0BCFF (Sleek Lavender Accent)
    onPrimary = Color(0xFF381E72),       // Contrast deep dark purple text on lavender
    primaryContainer = SleekSeedPurple,  // 4F378B (Sleek Deep Purple)
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = SkyBlue,
    onSecondary = Color.Black,
    surfaceVariant = SlateFocus,        // 4A4458 (Active selection / dark lavender variant)
    background = SlateDark,              // 1C1B1F (Deep Slate Dark Background)
    surface = SlateCard,                 // 2B2930 (Surfaces, nav bars, tiles)
    onBackground = Color(0xFFE6E1E5),    // Sleek light tint text
    onSurface = Color(0xFFE6E1E5),       // Sleek light tint text
    error = LiveRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    background = Color(0xFFF9F9FB),
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // For cinematic sleek theme, disable dynamic colors by default to preserve custom brand colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
