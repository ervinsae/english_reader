package com.ervinzhang.englishreader.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = StorybookPrimary,
    onPrimary = StorybookOnPrimary,
    primaryContainer = StorybookPrimaryContainer,
    onPrimaryContainer = StorybookOnPrimaryContainer,
    secondary = StorybookSecondary,
    onSecondary = StorybookOnSecondary,
    secondaryContainer = StorybookSecondaryContainer,
    onSecondaryContainer = StorybookOnSecondaryContainer,
    tertiary = StorybookTertiary,
    onTertiary = StorybookOnTertiary,
    tertiaryContainer = StorybookTertiaryContainer,
    onTertiaryContainer = StorybookOnTertiaryContainer,
    background = StorybookBackground,
    onBackground = StorybookOnBackground,
    surface = StorybookSurface,
    onSurface = StorybookOnSurface,
    surfaceVariant = StorybookSurfaceVariant,
    onSurfaceVariant = StorybookOnSurfaceVariant,
    surfaceContainerLowest = StorybookSurfaceContainerLowest,
    surfaceContainerLow = StorybookSurfaceContainerLow,
    surfaceContainer = StorybookSurfaceContainer,
    surfaceContainerHigh = StorybookSurfaceContainerHigh,
    surfaceContainerHighest = StorybookSurfaceContainerHighest,
    outline = StorybookOutline,
    outlineVariant = StorybookOutlineVariant,
    error = StorybookError,
    onError = StorybookOnError,
    errorContainer = StorybookErrorContainer,
    onErrorContainer = StorybookOnErrorContainer,
)

@Composable
fun EnglishReaderTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
