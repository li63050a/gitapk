package com.git.app.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.git.app.R
import com.git.app.data.AccentPreset
import com.git.app.data.BgPreset
import com.git.app.data.SettingsRepository
import com.git.app.data.ThemeMode
import java.io.File

private fun mix(base: Color, target: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = base.red + (target.red - base.red) * f,
        green = base.green + (target.green - base.green) * f,
        blue = base.blue + (target.blue - base.blue) * f,
        alpha = base.alpha,
    )
}

private fun presetScheme(bg: BgPreset, accent: AccentPreset, dark: Boolean): ColorScheme {
    val bgLight = android.graphics.Color.parseColor(bg.light)
    val bgDark = android.graphics.Color.parseColor(bg.dark)
    val accentLight = android.graphics.Color.parseColor(accent.light)
    val accentDark = android.graphics.Color.parseColor(accent.dark)
    
    val background = if (dark) Color(bgDark) else Color(bgLight)
    val primary = if (dark) Color(accentDark) else Color(accentLight)
    val surfaceVariant = if (dark) mix(background, Color.White, 0.06f) else mix(background, Color.Black, 0.05f)
    val outline = if (dark) mix(background, Color.White, 0.18f) else mix(background, Color.Black, 0.22f)
    val surfaceContainer = if (dark) mix(background, Color.White, 0.04f) else mix(background, Color.Black, 0.03f)
    val surface = if (dark) mix(background, Color.White, 0.02f) else mix(background, Color.Black, 0.01f)
    return if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF181818),
            primaryContainer = mix(primary, Color.White, 0.15f),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = mix(primary, Color.White, 0.35f),
            background = background,
            onBackground = Color(0xFFE6E6E6),
            surface = surface,
            onSurface = Color(0xFFE6E6E6),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFB8B8B8),
            outline = outline,
            error = Color(0xFFFF8A8A),
            outlineVariant = surfaceVariant,
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = primary,
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = mix(primary, Color.White, 0.82f),
            onPrimaryContainer = mix(primary, Color.Black, 0.35f),
            secondary = mix(primary, Color.Black, 0.12f),
            background = background,
            onBackground = Color(0xFF1B1B1B),
            surface = surface,
            onSurface = Color(0xFF1B1B1B),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF555555),
            outline = outline,
            outlineVariant = surfaceVariant,
            error = Color(0xFFB3261E),
        )
    }
}

@Composable
fun GitAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settings by SettingsRepository.getSettings(context).collectAsState(
        initial = com.git.app.data.UiSettings()
    )
    
    val themeMode = when (settings.themeMode) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    
    val colorScheme = presetScheme(settings.bgPreset, settings.accentPreset, themeMode)
    
    if (settings.customBgPath.isNullOrEmpty()) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    } else {
        val bitmap = try {
            BitmapFactory.decodeFile(settings.customBgPath)
        } catch (e: Exception) {
            null
        }
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MaterialTheme(
                    colorScheme = colorScheme,
                    content = content
                )
            }
        }
    }
}
