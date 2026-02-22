package com.example.arthguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.arthguard.core.util.AppColors

private val AppColorScheme = darkColorScheme(
    primary = AppColors.bgSecondary,
    secondary = AppColors.bgSecondary,
    tertiary = AppColors.bgSecondary,
    background = AppColors.bgPrimary,
    surface = AppColors.bgPrimary,
    surfaceContainer = AppColors.bgPrimary,
    surfaceContainerLow = AppColors.bgPrimary,
    surfaceContainerHigh = AppColors.bgSecondary,
    onPrimary = AppColors.textPrimary,
    onSecondary = AppColors.textPrimary,
    onTertiary = AppColors.textPrimary,
    onBackground = AppColors.textPrimary,
    onSurface = AppColors.textPrimary,
)

@Composable
fun ArthGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
