package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class PrimaryThemeColor(
    val key: String,
    val titleFa: String,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val lightContainer: Color,
    val darkContainer: Color
) {
    BLUE("BLUE", "آبی کلاسیک", Color(0xFF0066FF), Color(0xFF3385FF), Color(0xFFE8F1FF), Color(0xFF1E3A8A)),
    GREEN("GREEN", "سبز زمردی", Color(0xFF059669), Color(0xFF10B981), Color(0xFFE6F4EA), Color(0xFF064E3B)),
    PURPLE("PURPLE", "بنفش شاهانه", Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFFF3E8FF), Color(0xFF4C1D95)),
    INDIGO("INDIGO", "نیلی", Color(0xFF4F46E5), Color(0xFF6366F1), Color(0xFFEEF2FF), Color(0xFF312E81)),
    ROSE("ROSE", "سرخابی", Color(0xFFE11D48), Color(0xFFF43F5E), Color(0xFFFFE4E6), Color(0xFF881337)),
    ORANGE("ORANGE", "نارنجی کهربایی", Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFFFEDD5), Color(0xFF7C2D12)),
    TEAL("TEAL", "فیروزه‌ای", Color(0xFF0D9488), Color(0xFF14B8A6), Color(0xFFCCFBF1), Color(0xFF134E4A));

    companion object {
        fun fromKey(key: String): PrimaryThemeColor {
            return entries.firstOrNull { it.key == key } ?: BLUE
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = Color.Black,
    primaryContainer = EmeraldPrimary,
    secondary = BlueAccent,
    background = DarkSlateBackground,
    surface = DarkSlateSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    error = ExpenseRed
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    secondary = BlueAccent,
    background = SlateBackground,
    surface = SlateSurface,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    error = ExpenseRed
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryColorKey: String = "BLUE",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeColor = PrimaryThemeColor.fromKey(primaryColorKey)

    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val customColorScheme = if (darkTheme) {
        baseColorScheme.copy(
            primary = themeColor.darkPrimary,
            primaryContainer = themeColor.darkContainer,
            secondary = themeColor.darkPrimary
        )
    } else {
        baseColorScheme.copy(
            primary = themeColor.lightPrimary,
            primaryContainer = themeColor.lightContainer,
            secondary = themeColor.lightPrimary
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = customColorScheme,
            typography = Typography,
            content = content
        )
    }
}
