package com.saku.v2.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.saku.v2.data.model.ThemeMode

val LocalSakuColors = staticCompositionLocalOf { LightSakuColors }

object SakuTheme {
    val colors: SakuColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSakuColors.current
}

@Composable
fun SakuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val sakuColors = if (darkTheme) DarkSakuColors else LightSakuColors
    val materialScheme = if (darkTheme) SakuDarkMaterialScheme else SakuLightMaterialScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalSakuColors provides sakuColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = SakuTypography,
            content = content
        )
    }
}
