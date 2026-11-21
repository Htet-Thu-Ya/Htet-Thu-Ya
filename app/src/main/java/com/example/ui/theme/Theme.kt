package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val EmeraldGoldColorScheme = darkColorScheme(
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

private val RoyalBlueColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF172554),
    onPrimaryContainer = ChampionshipGoldBright,
    secondary = ChampionshipGold,
    onSecondary = InkDark,
    secondaryContainer = Color(0xFF272010),
    onSecondaryContainer = ChampionshipGoldBright,
    tertiary = Color(0xFF38BDF8),
    onTertiary = InkDark,
    background = Color(0xFF070F1E),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F1B33),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF172747),
    onSurfaceVariant = Color(0xFFB6C5DF),
    outline = Color(0xFF2A416F),
    error = Color(0xFFEF4444),
    onError = Color.White
)

private val CrimsonGoldColorScheme = darkColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0D19),
    onPrimaryContainer = ChampionshipGoldBright,
    secondary = ChampionshipGold,
    onSecondary = InkDark,
    secondaryContainer = Color(0xFF2C220F),
    onSecondaryContainer = ChampionshipGoldBright,
    tertiary = Color(0xFFF59E0B),
    onTertiary = InkDark,
    background = Color(0xFF14080C),
    onBackground = Color(0xFFFAF2F4),
    surface = Color(0xFF210E14),
    onSurface = Color(0xFFFAF2F4),
    surfaceVariant = Color(0xFF2E141D),
    onSurfaceVariant = Color(0xFFD9B8C2),
    outline = Color(0xFF562333),
    error = Color(0xFFF87171),
    onError = InkDark
)

private val ObsidianAmberColorScheme = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = InkDark,
    primaryContainer = Color(0xFF2A2110),
    onPrimaryContainer = ChampionshipGoldBright,
    secondary = PitchEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF11291E),
    onSecondaryContainer = Color(0xFF6EE7B7),
    tertiary = SkyInfo,
    onTertiary = Color.White,
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF151921),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1F2530),
    onSurfaceVariant = Color(0xFFBFC5D0),
    outline = Color(0xFF364052),
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
    themePreset: String = "emerald_gold",
    content: @Composable () -> Unit
) {
    val scheme = when (themePreset) {
        "royal_blue" -> RoyalBlueColorScheme
        "crimson_gold" -> CrimsonGoldColorScheme
        "obsidian_amber" -> ObsidianAmberColorScheme
        else -> EmeraldGoldColorScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = HtetThuYaShapes,
        content = content
    )
}
