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

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ComicShapes = Shapes(
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(20.dp)
)

private val DarkColorScheme =
  darkColorScheme(
    primary = ActionBlue,
    secondary = SecondaryTextDark,
    tertiary = ActionBlue,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = BorderDark,
    onPrimary = PrimaryTextLight,
    onSecondary = PrimaryTextDark,
    onBackground = PrimaryTextDark,
    onSurface = PrimaryTextDark,
    onSurfaceVariant = SecondaryTextDark,
    outline = BorderDark
  )


private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF1E1E1E), // Bold hand-inked near-black for cartoon outlines
    secondary = Color(0xFF00E5FF), // Electric Schwifty Turquoise
    tertiary = Color(0xFF2EDD3E), // Neon Portal Green
    background = Color(0xFFFFFFFF), // Pure white main paper canvas
    surface = Color(0xFFFFFFFF), // Pure white styled cards
    surfaceVariant = Color(0xFFF3FAF2), // Subtly tinted neon-mint highlight
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF1E1E1E),
    onBackground = Color(0xFF1E1E1E),
    onSurface = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFF444444),
    outline = Color(0xFF1E1E1E) // Stout hand-drawn line color
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = ComicShapes, content = content)
}
