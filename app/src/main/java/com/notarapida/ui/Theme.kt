package com.notarapida.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Colores de la nota adhesiva: se usan igual en modo claro y oscuro. */
val StickyYellow = Color(0xFFFFF59D)
val StickyInk = Color(0xFF3E3A00)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A6200),
    onPrimary = Color.White,
    primaryContainer = StickyYellow,
    onPrimaryContainer = StickyInk,
    background = Color(0xFFFFFBF0),
    surface = Color(0xFFFFFBF0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8C94A),
    onPrimary = Color(0xFF3E3000),
    primaryContainer = Color(0xFF5B4800),
    onPrimaryContainer = StickyYellow,
)

@Composable
fun NotaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
