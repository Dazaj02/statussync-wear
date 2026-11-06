package com.statussync.wear.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** `primary` tiñe el fondo y las superficies: el color del estado en la pantalla principal, [BrandPrimary] en el resto. */
@Composable
fun StatusSyncTheme(primary: Color = BrandPrimary, content: @Composable () -> Unit) {
    val background by animateColorAsState(backgroundFor(primary), tween(300), label = "background")
    val colors = ColorScheme(
        primary = primary,
        onPrimary = onColorFor(primary),
        background = background,
        onBackground = OnSurface,
        surfaceContainer = lerp(background, Color.White, 0.14f),
        onSurface = OnSurface,
        onSurfaceVariant = lerp(Color.White, primary, 0.20f),
        error = Color(0xFFFF8A8A),
    )
    Box(Modifier.fillMaxSize().background(background)) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
