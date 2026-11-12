package ai.dadacast.app.ui

import ai.dadacast.app.R
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Colours mapped to Pleos Connect design tokens (Light/Dark pairs). Text and icon colours keep at
 * least 4.5:1 against the surfaces they sit on.
 */
@Immutable
data class DadaColors(
    val background: Color,     // background_primary (basic_50)
    val surface: Color,        // surface_basic
    val surfaceHigh: Color,    // surface_high (basic_100)
    val surfacePressed: Color, // surface_accent (basic_200)
    val text: Color,           // text_*_primary
    val textSecondary: Color,  // text_*_tertiary (64%)
    val accent: Color,         // music_primary family, darkened or lightened for contrast
    val onAccent: Color,
    val track: Color,
)

val LightColors = DadaColors(
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEDEEF2),
    surfacePressed = Color(0xFFE0E1E6),
    text = Color(0xFF131417),
    textSecondary = Color(0xFF68696B),
    accent = Color(0xFF2B63D9),
    onAccent = Color(0xFFFFFFFF),
    track = Color(0xFFE0E1E6),
)

val DarkColors = DadaColors(
    background = Color(0xFF131417),
    surface = Color(0xFF1D1E21),
    surfaceHigh = Color(0xFF313236),
    surfacePressed = Color(0xFF44464E),
    text = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAEAEAF),
    accent = Color(0xFF6A9BFF),
    onAccent = Color(0xFF131417),
    track = Color(0xFF44464E),
)

@OptIn(ExperimentalTextApi::class)
private fun asta(weight: Int) = Font(
    R.font.asta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Asta Sans, the Pleos Connect system font (SIL Open Font License, bundled). */
val AstaSans = FontFamily(asta(400), asta(600), asta(700), asta(800))

/** Pleos type scale: size/line height in sp. */
object DadaType {
    val headlineSmall = TextStyle(fontFamily = AstaSans, fontSize = 48.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold)
    val titleLarge = TextStyle(fontFamily = AstaSans, fontSize = 40.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold)
    val titleMedium = TextStyle(fontFamily = AstaSans, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
    val titleSmall = TextStyle(fontFamily = AstaSans, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal)
    val bodyLarge = TextStyle(fontFamily = AstaSans, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Normal)
    val bodyLargeStrong = bodyLarge.copy(fontWeight = FontWeight.SemiBold)
    val bodyMedium = TextStyle(fontFamily = AstaSans, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal)
    val bodySmall = TextStyle(fontFamily = AstaSans, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal)
    val label = TextStyle(fontFamily = AstaSans, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal)
}

val LocalDadaColors = staticCompositionLocalOf { LightColors }

/** Follows the system day/night setting. */
@Composable
fun DadaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(primary = colors.accent, background = colors.background, surface = colors.surface)
    } else {
        lightColorScheme(primary = colors.accent, background = colors.background, surface = colors.surface)
    }
    CompositionLocalProvider(LocalDadaColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Pleos motion tokens. */
private val PleosEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private const val SHORT2_MS = 150

/**
 * Pleos press feedback: the element shrinks to 95% and its background shifts while pressed.
 * Replaces the default ripple.
 */
@Composable
fun Modifier.pressable(
    shape: Shape,
    color: Color,
    pressedColor: Color,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(SHORT2_MS, easing = PleosEasing), label = "press")
    return this
        .scale(scale)
        .clip(shape)
        .background(if (pressed) pressedColor else color)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
