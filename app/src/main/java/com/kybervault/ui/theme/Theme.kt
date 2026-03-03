package com.kybervault.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kybervault.R

// Colorblind-safe: blue/amber contrast, no red/green reliance.
val KvBlue = Color(0xFF3B82F6)
val KvBlueDark = Color(0xFF2563EB)
val KvAmber = Color(0xFFF59E0B)
val KvAmberLight = Color(0xFFFBBF24)
val KvCyan = Color(0xFF06B6D4)
val KvRed = Color(0xFFEF4444)
val KvRedSoft = Color(0x1AEF4444)

private val KvDarkColors = darkColorScheme(
    primary = KvBlue, onPrimary = Color.White,
    primaryContainer = KvBlueDark, onPrimaryContainer = Color.White,
    secondary = KvAmber, onSecondary = Color.Black,
    secondaryContainer = Color(0xFF422006), onSecondaryContainer = KvAmberLight,
    tertiary = KvCyan, onTertiary = Color.Black,
    error = KvRed, onError = Color.White, errorContainer = KvRedSoft, onErrorContainer = KvRed,
    background = Color(0xFF111827), onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827), onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B), onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569), outlineVariant = Color(0xFF334155),
    surfaceContainerHighest = Color(0xFF334155),
)

/** OpenDyslexic — replace res/font/opendyslexic_regular.ttf with real file from opendyslexic.org */
val OpenDyslexicFamily = FontFamily(Font(R.font.opendyslexic_regular))

val KvTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.5).sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)

val KvTypographyDyslexic = Typography(
    headlineMedium = TextStyle(fontFamily = OpenDyslexicFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 1.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = OpenDyslexicFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 0.5.sp, lineHeight = 28.sp),
    titleSmall = TextStyle(fontFamily = OpenDyslexicFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.5.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = OpenDyslexicFamily, fontSize = 17.sp, lineHeight = 28.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = OpenDyslexicFamily, fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
    bodySmall = TextStyle(fontFamily = OpenDyslexicFamily, fontSize = 13.sp, lineHeight = 22.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontFamily = OpenDyslexicFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontFamily = OpenDyslexicFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontFamily = OpenDyslexicFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.8.sp),
)

@Composable
fun KyberVaultTheme(dyslexicFont: Boolean = false, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color(0xFF111827).toArgb()
            window.navigationBarColor = Color(0xFF111827).toArgb()
        }
    }
    MaterialTheme(
        colorScheme = KvDarkColors,
        typography = if (dyslexicFont) KvTypographyDyslexic else KvTypography,
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp)),
        content = content
    )
}
