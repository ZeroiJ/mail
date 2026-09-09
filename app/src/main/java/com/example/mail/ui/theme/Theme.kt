package com.example.mail.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Fixed, hand-tuned dark (OLED) color scheme. Dark color scheme is hardcoded
// so the app renders monochrome regardless of system theme.
private val NothingColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = OLEDBlack,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = PureWhite,
    secondary = MutedGray,
    onSecondary = OLEDBlack,
    secondaryContainer = SurfaceDarkElevated,
    onSecondaryContainer = PureWhite,
    tertiary = BorderGray,
    onTertiary = PureWhite,
    background = OLEDBlack,
    onBackground = PureWhite,
    surface = SurfaceDark,
    onSurface = PureWhite,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = MutedGray,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDarkElevated,
    surfaceContainerHighest = SurfaceDarkElevated,
    outline = BorderGray,
    outlineVariant = BorderGray,
    error = StarkRed,
    onError = OLEDBlack,
    errorContainer = StarkRedAlt,
    onErrorContainer = OLEDBlack
)

/**
 * Nothing OS monochrome theme. Dynamic Material You theming is intentionally
 * disabled so the strict black/white/gray palette is never overridden by the
 * system's pastel colors.
 */
@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NothingColorScheme,
        typography = NothingTypography,
        content = content
    )
}
