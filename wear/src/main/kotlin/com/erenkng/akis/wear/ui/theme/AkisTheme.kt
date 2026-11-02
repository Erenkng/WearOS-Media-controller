package com.erenkng.akis.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Wear M3 Expressive temeli, üzerine Akış token'ları (BRIEF Bölüm 4). */
private val AkisColorScheme = ColorScheme(
    primary = AkisColors.AccentFallback,
    onPrimary = AkisColors.Bg,
    primaryContainer = AkisColors.SurfaceHigh,
    onPrimaryContainer = AkisColors.OnSurface,
    secondary = AkisColors.OnSurfaceDim,
    onSecondary = AkisColors.Bg,
    surfaceContainerLow = AkisColors.Surface,
    surfaceContainer = AkisColors.Surface,
    surfaceContainerHigh = AkisColors.SurfaceHigh,
    onSurface = AkisColors.OnSurface,
    onSurfaceVariant = AkisColors.OnSurfaceDim,
    background = AkisColors.Bg,
    onBackground = AkisColors.OnSurface,
    error = AkisColors.Danger,
    onError = AkisColors.Bg,
)

/** @param accent kapaktan çıkan (ya da sabit) vurgu; bkz. [rememberArtworkAccent]. */
@Composable
fun AkisTheme(
    accent: AccentColors = AccentColors(AkisColors.AccentFallback, AkisColors.Bg),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = AkisColorScheme) {
        CompositionLocalProvider(LocalAccent provides accent) {
            content()
        }
    }
}
