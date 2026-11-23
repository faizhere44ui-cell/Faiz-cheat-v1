package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class PanelExtendedColors(
    val panelTop: Color,
    val panelBottom: Color,
    val itemSurface: Color,
    val subtleBorder: Color,
    val activeGreen: Color,
    val activeGreenTrack: Color,
    val activeGreenSurface: Color,
    val inactiveRed: Color,
    val inactiveRedTrack: Color,
    val inactiveRedSurface: Color,
    val mutedText: Color,
    val footerText: Color,
    val pillText: Color
)

private val DefaultExtendedColors = PanelExtendedColors(
    panelTop = CarbonPanelTop,
    panelBottom = CarbonPanelBottom,
    itemSurface = DarkGreySurface,
    subtleBorder = SubtleBorder,
    activeGreen = EmeraldGreen,
    activeGreenTrack = EmeraldTrackOn,
    activeGreenSurface = EmeraldBarFill,
    inactiveRed = CrimsonRed,
    inactiveRedTrack = CrimsonTrackOff,
    inactiveRedSurface = CrimsonButtonFill,
    mutedText = MutedSlate,
    footerText = DimFooter,
    pillText = CoolSilver
)

val LocalPanelColors = staticCompositionLocalOf { DefaultExtendedColors }

private val PanelDarkColorScheme = darkColorScheme(
    primary = CrimsonRed,
    onPrimary = CrispWhite,
    secondary = EmeraldGreen,
    onSecondary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = CrispWhite,
    surface = CarbonPanelBottom,
    onSurface = CrispWhite,
    surfaceVariant = DarkGreySurface,
    onSurfaceVariant = MutedSlate,
    outline = SubtleBorder,
    error = CrimsonRed,
    onError = CrispWhite
)

private val PanelShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun FaizPanelTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalPanelColors provides DefaultExtendedColors) {
        MaterialTheme(
            colorScheme = PanelDarkColorScheme,
            typography = AppTypography,
            shapes = PanelShapes,
            content = content
        )
    }
}
