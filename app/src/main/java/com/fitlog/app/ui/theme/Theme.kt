package com.fitlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class FitColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val line2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color,
    val accentInk: Color,
    val accentText: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val danger: Color,
    val glow: Color,
    val tint: Color,
    val glass: Color
)

private val DarkColors = FitColors(
    bg = Color(0xFF0A0C10),
    surface = Color(0xFF14171E),
    surface2 = Color(0xFF1B1F28),
    line = Color(0x12FFFFFF),
    line2 = Color(0x21FFFFFF),
    text = Color(0xFFF4F6FA),
    text2 = Color(0xFF98A1B2),
    text3 = Color(0xFF69707E),
    accent = Color(0xFFB4F06A),
    accentInk = Color(0xFF0E1406),
    accentText = Color(0xFFB4F06A),
    protein = Color(0xFF6EE7B7),
    carbs = Color(0xFF93B8FF),
    fat = Color(0xFFFFC46B),
    danger = Color(0xFFFF7A6B),
    glow = Color(0x1AB4F06A),
    tint = Color(0x0BFFFFFF),
    glass = Color(0xE614171E)
)

private val LightColors = FitColors(
    bg = Color(0xFFF2F3F5),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEEF0F3),
    line = Color(0x1310141A),
    line2 = Color(0x2410141A),
    text = Color(0xFF10141A),
    text2 = Color(0xFF5B6474),
    text3 = Color(0xFF8B93A1),
    accent = Color(0xFFB9EE73),
    accentInk = Color(0xFF16210A),
    accentText = Color(0xFF41730A),
    protein = Color(0xFF14A87B),
    carbs = Color(0xFF3B6FD4),
    fat = Color(0xFFC4831A),
    danger = Color(0xFFD6453A),
    glow = Color(0x2478BE28),
    tint = Color(0x0A10141A),
    glass = Color(0xF2FFFFFF)
)

val LocalFitColors = staticCompositionLocalOf { DarkColors }

object FitTheme {
    val colors: FitColors
        @Composable @ReadOnlyComposable get() = LocalFitColors.current
}

private val FitTypography = Typography(
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun FitLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val fit = if (darkTheme) DarkColors else LightColors

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = fit.accent,
            onPrimary = fit.accentInk,
            background = fit.bg,
            onBackground = fit.text,
            surface = fit.surface,
            onSurface = fit.text,
            surfaceVariant = fit.surface2,
            onSurfaceVariant = fit.text2,
            outline = fit.line2,
            error = fit.danger
        )
    } else {
        lightColorScheme(
            primary = fit.accent,
            onPrimary = fit.accentInk,
            background = fit.bg,
            onBackground = fit.text,
            surface = fit.surface,
            onSurface = fit.text,
            surfaceVariant = fit.surface2,
            onSurfaceVariant = fit.text2,
            outline = fit.line2,
            error = fit.danger
        )
    }

    CompositionLocalProvider(LocalFitColors provides fit) {
        MaterialTheme(colorScheme = scheme, typography = FitTypography, content = content)
    }
}
