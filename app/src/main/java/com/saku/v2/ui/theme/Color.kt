package com.saku.v2.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SakuColorTokens(
    val isDark: Boolean,
    val canvasGradientStart: Color,
    val canvasGradientMid: Color,
    val canvasGradientEnd: Color,
    val poolSage: Color,
    val poolLimestone: Color,
    val poolSlate: Color,
    val glassSurface: Color,
    val glassSurfaceFallback: Color,
    val glassTopRim: Color,
    val glassBottomHairline: Color,
    val sheetSurface: Color,
    val cardBorderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val transfer: Color,
    val transferContainer: Color,
    val warning: Color,
    val warningContainer: Color
)

/**
 * Light Mode — "Mineral Alabaster": warm limestone & sage mineral daylight palette.
 */
val LightSakuColors = SakuColorTokens(
    isDark = false,
    canvasGradientStart = Color(0xFFF5F3EE),
    canvasGradientMid = Color(0xFFEDEAE3),
    canvasGradientEnd = Color(0xFFE6EBE7),
    poolSage = Color(0xFFD8E4DE).copy(alpha = 0.42f),
    poolLimestone = Color(0xFFEAE0D3).copy(alpha = 0.38f),
    poolSlate = Color(0xFFDCE3EC).copy(alpha = 0.34f),
    glassSurface = Color(0xFFFFFFFF).copy(alpha = 0.60f),
    glassSurfaceFallback = Color(0xFFFFFFFF).copy(alpha = 0.84f),
    glassTopRim = Color(0xFFFFFFFF).copy(alpha = 0.82f),
    glassBottomHairline = Color(0xFF1A1D1F).copy(alpha = 0.06f),
    sheetSurface = Color(0xFFFAF9F6),
    cardBorderSubtle = Color(0xFF1A1D1F).copy(alpha = 0.08f),
    textPrimary = Color(0xFF16181A),
    textSecondary = Color(0xFF5A5F66),
    textMuted = Color(0xFF8A8F98),
    primary = Color(0xFF1E4D45),
    primaryContainer = Color(0xFFE2EDE9),
    onPrimary = Color(0xFFFAF9F6),
    income = Color(0xFF2A6B57),
    incomeContainer = Color(0xFFE1EFE9),
    expense = Color(0xFFB8563F),
    expenseContainer = Color(0xFFF6E6E2),
    transfer = Color(0xFF3D5473),
    transferContainer = Color(0xFFE6ECF3),
    warning = Color(0xFFA87633),
    warningContainer = Color(0xFFF4ECE0)
)

/**
 * Dark Mode — "Smoked Obsidian": refined charcoal mineral slate palette.
 */
val DarkSakuColors = SakuColorTokens(
    isDark = true,
    canvasGradientStart = Color(0xFF101214),
    canvasGradientMid = Color(0xFF16191D),
    canvasGradientEnd = Color(0xFF131817),
    poolSage = Color(0xFF1B2D2A).copy(alpha = 0.55f),
    poolLimestone = Color(0xFF26201B).copy(alpha = 0.42f),
    poolSlate = Color(0xFF1B232F).copy(alpha = 0.45f),
    glassSurface = Color(0xFF1E2227).copy(alpha = 0.58f),
    glassSurfaceFallback = Color(0xFF1E2227).copy(alpha = 0.86f),
    glassTopRim = Color(0xFFFFFFFF).copy(alpha = 0.14f),
    glassBottomHairline = Color(0xFF000000).copy(alpha = 0.35f),
    sheetSurface = Color(0xFF181B1F),
    cardBorderSubtle = Color(0xFFFFFFFF).copy(alpha = 0.09f),
    textPrimary = Color(0xFFF1EFEA),
    textSecondary = Color(0xFF9CA1AA),
    textMuted = Color(0xFF696E78),
    primary = Color(0xFF6FB09C),
    primaryContainer = Color(0xFF1C332D),
    onPrimary = Color(0xFF0F1B18),
    income = Color(0xFF6EB396),
    incomeContainer = Color(0xFF1B332A),
    expense = Color(0xFFD67B65),
    expenseContainer = Color(0xFF38231E),
    transfer = Color(0xFF7D9AC0),
    transferContainer = Color(0xFF1E2A3A),
    warning = Color(0xFFCFA262),
    warningContainer = Color(0xFF332B1D)
)

internal val SakuLightMaterialScheme = lightColorScheme(
    primary = LightSakuColors.primary,
    onPrimary = LightSakuColors.onPrimary,
    primaryContainer = LightSakuColors.primaryContainer,
    onPrimaryContainer = LightSakuColors.primary,
    secondary = LightSakuColors.transfer,
    onSecondary = Color.White,
    secondaryContainer = LightSakuColors.transferContainer,
    onSecondaryContainer = LightSakuColors.transfer,
    tertiary = LightSakuColors.income,
    onTertiary = Color.White,
    tertiaryContainer = LightSakuColors.incomeContainer,
    onTertiaryContainer = LightSakuColors.income,
    error = LightSakuColors.expense,
    onError = Color.White,
    errorContainer = LightSakuColors.expenseContainer,
    onErrorContainer = LightSakuColors.expense,
    background = LightSakuColors.canvasGradientStart,
    onBackground = LightSakuColors.textPrimary,
    surface = LightSakuColors.sheetSurface,
    onSurface = LightSakuColors.textPrimary,
    surfaceVariant = LightSakuColors.canvasGradientMid,
    onSurfaceVariant = LightSakuColors.textSecondary,
    outline = LightSakuColors.cardBorderSubtle,
    outlineVariant = LightSakuColors.glassBottomHairline
)

internal val SakuDarkMaterialScheme = darkColorScheme(
    primary = DarkSakuColors.primary,
    onPrimary = DarkSakuColors.onPrimary,
    primaryContainer = DarkSakuColors.primaryContainer,
    onPrimaryContainer = DarkSakuColors.primary,
    secondary = DarkSakuColors.transfer,
    onSecondary = DarkSakuColors.onPrimary,
    secondaryContainer = DarkSakuColors.transferContainer,
    onSecondaryContainer = DarkSakuColors.transfer,
    tertiary = DarkSakuColors.income,
    onTertiary = DarkSakuColors.onPrimary,
    tertiaryContainer = DarkSakuColors.incomeContainer,
    onTertiaryContainer = DarkSakuColors.income,
    error = DarkSakuColors.expense,
    onError = DarkSakuColors.onPrimary,
    errorContainer = DarkSakuColors.expenseContainer,
    onErrorContainer = DarkSakuColors.expense,
    background = DarkSakuColors.canvasGradientStart,
    onBackground = DarkSakuColors.textPrimary,
    surface = DarkSakuColors.sheetSurface,
    onSurface = DarkSakuColors.textPrimary,
    surfaceVariant = DarkSakuColors.canvasGradientMid,
    onSurfaceVariant = DarkSakuColors.textSecondary,
    outline = DarkSakuColors.cardBorderSubtle,
    outlineVariant = DarkSakuColors.glassBottomHairline
)
