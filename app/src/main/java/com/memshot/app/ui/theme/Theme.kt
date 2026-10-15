package com.memshot.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Violet = Color(0xFF6C4DFF)
val Pink = Color(0xFFFF5C8A)
val Amber = Color(0xFFFFB224)

val BrandGradient = Brush.linearGradient(listOf(Color(0xFF7B5CFF), Color(0xFFB45CFF), Pink))

private val Light = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF22175E),
    secondary = Pink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0E8),
    onSecondaryContainer = Color(0xFF5C1027),
    background = Color(0xFFF7F5FF),
    onBackground = Color(0xFF1B1A22),
    surface = Color(0xFFF7F5FF),
    onSurface = Color(0xFF1B1A22),
    surfaceVariant = Color(0xFFECE8F8),
    onSurfaceVariant = Color(0xFF5E5A6E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2EFFC),
    surfaceContainer = Color(0xFFEDE9FA),
    surfaceContainerHigh = Color(0xFFE7E3F6),
    surfaceContainerHighest = Color(0xFFE1DCF2),
    outline = Color(0xFFB9B4CC),
    outlineVariant = Color(0xFFDCD7EA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA897FF),
    onPrimary = Color(0xFF1E1354),
    primaryContainer = Color(0xFF3B2C99),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFFFF8FAE),
    onSecondary = Color(0xFF4A0B1E),
    secondaryContainer = Color(0xFF6E1F37),
    onSecondaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF0E0C17),
    onBackground = Color(0xFFE8E5F2),
    surface = Color(0xFF0E0C17),
    onSurface = Color(0xFFE8E5F2),
    surfaceVariant = Color(0xFF221F2E),
    onSurfaceVariant = Color(0xFFB1ACC4),
    surfaceContainerLowest = Color(0xFF09080F),
    surfaceContainerLow = Color(0xFF15131F),
    surfaceContainer = Color(0xFF1A1725),
    surfaceContainerHigh = Color(0xFF221F2E),
    surfaceContainerHighest = Color(0xFF2B2738),
    outline = Color(0xFF5B566D),
    outlineVariant = Color(0xFF2F2B3D),
)

private val Base = Typography()
private val AppTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MemShotTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
    ) {
        Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, content = content)
    }
}
