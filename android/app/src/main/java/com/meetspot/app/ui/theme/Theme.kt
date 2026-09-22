package com.meetspot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MeetSpotColorScheme = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Orange,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Line,
    onSurfaceVariant = Muted,
    error = Orange,
)

/** Light, warm palette matching the web app's public/styles.css theme. */
@Composable
fun MeetSpotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MeetSpotColorScheme,
        typography = MeetSpotTypography,
        content = content,
    )
}
