package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val HtetThuYaColorScheme = darkColorScheme(
    primary = PitchEmerald,
    onPrimary = Color.White,
    primaryContainer = DeepPitchElevated,
    onPrimaryContainer = ChampionshipGoldBright,
    secondary = ChampionshipGold,
    onSecondary = InkDark,
    secondaryContainer = Color(0xFF282210),
    onSecondaryContainer = ChampionshipGoldBright,
    tertiary = SkyInfo,
    onTertiary = Color.White,
    background = DeepPitchBg,
    onBackground = Color(0xFFF1F5F3),
    surface = DeepPitchSurface,
    onSurface = Color(0xFFF1F5F3),
    surfaceVariant = DeepPitchElevated,
    onSurfaceVariant = Color(0xFFB3C2BA),
    outline = Color(0xFF2E473B),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val HtetThuYaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun HtetThuYaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HtetThuYaColorScheme,
        typography = AppTypography,
        shapes = HtetThuYaShapes,
        content = content
    )
}
