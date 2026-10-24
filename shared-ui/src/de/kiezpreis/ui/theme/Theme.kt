package de.kiezpreis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Kiezpreis colour system: calm neutrals, one interactive accent (periwinkle), green reserved for
 * good deals, amber for "please note" information. Text colours meet WCAG AA on their surfaces.
 */
@Immutable
data class KiezColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundDeep: Color,
    val aurora1: Color,
    val aurora2: Color,
    val aurora3: Color,
    /** Fill of glass elements on top of content (nav bar, search bar). */
    val glass: Color,
    /** Stronger fill used when blur is unavailable or transparency is reduced. */
    val glassSolid: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    /** Cards: quieter than glass, always readable. */
    val card: Color,
    val cardBorder: Color,
    val cardPressed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val deal: Color,
    val dealSoft: Color,
    val onDeal: Color,
    val caution: Color,
    val cautionSoft: Color,
    val info: Color,
    val infoSoft: Color,
    val danger: Color,
    val divider: Color,
    val scrim: Color,
    val shadow: Color,
)

val DarkKiez = KiezColors(
    isDark = true,
    background = Color(0xFF080A11),
    backgroundDeep = Color(0xFF0C1120),
    aurora1 = Color(0xFF5B5BF0),
    aurora2 = Color(0xFF1FD1A5),
    aurora3 = Color(0xFFB45CFF),
    glass = Color(0x2E1A2033),
    glassSolid = Color(0xFA151A28),
    glassBorderTop = Color(0x38FFFFFF),
    glassBorderBottom = Color(0x0DFFFFFF),
    card = Color(0x14FFFFFF),
    cardBorder = Color(0x1AFFFFFF),
    cardPressed = Color(0x22FFFFFF),
    textPrimary = Color(0xFFF2F5FA),
    textSecondary = Color(0xFFB0B9CC),
    textTertiary = Color(0xFF7D879C),
    accent = Color(0xFF9AA6FF),
    onAccent = Color(0xFF0B0F24),
    accentSoft = Color(0x299AA6FF),
    deal = Color(0xFF45E3A6),
    dealSoft = Color(0x2445E3A6),
    onDeal = Color(0xFF03170F),
    caution = Color(0xFFFFC56B),
    cautionSoft = Color(0x24FFC56B),
    info = Color(0xFF7CC4FF),
    infoSoft = Color(0x247CC4FF),
    danger = Color(0xFFFF8A8A),
    divider = Color(0x14FFFFFF),
    scrim = Color(0x99000000),
    shadow = Color(0x66000000),
)

val LightKiez = KiezColors(
    isDark = false,
    background = Color(0xFFF4F5F9),
    backgroundDeep = Color(0xFFE9ECF4),
    aurora1 = Color(0xFFAFB8FF),
    aurora2 = Color(0xFF8EEBCB),
    aurora3 = Color(0xFFFFD2B8),
    glass = Color(0x99FFFFFF),
    glassSolid = Color(0xFCFFFFFF),
    glassBorderTop = Color(0xF2FFFFFF),
    glassBorderBottom = Color(0x14101828),
    card = Color(0xB8FFFFFF),
    cardBorder = Color(0x14101828),
    cardPressed = Color(0xE6FFFFFF),
    textPrimary = Color(0xFF0E1320),
    textSecondary = Color(0xFF475166),
    textTertiary = Color(0xFF6B7488),
    accent = Color(0xFF4652D1),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0x1F4652D1),
    deal = Color(0xFF07875C),
    dealSoft = Color(0x1A07875C),
    onDeal = Color(0xFFFFFFFF),
    caution = Color(0xFF9A5B00),
    cautionSoft = Color(0x1FC27A00),
    info = Color(0xFF1F63B5),
    infoSoft = Color(0x1A1F63B5),
    danger = Color(0xFFC0392B),
    divider = Color(0x14101828),
    scrim = Color(0x66101828),
    shadow = Color(0x1F101828),
)

/** Motion settings. With reduced motion, transitions become simple short fades. */
@Immutable
data class KiezMotion(val reduced: Boolean) {
    val short: Int get() = if (reduced) 0 else 180
    val medium: Int get() = if (reduced) 0 else 280
}

/** Glass rendering settings. */
@Immutable
data class GlassSettings(
    /** Real backdrop blur is available (Android 12+). Without it, glass falls back to a denser tint. */
    val blurAvailable: Boolean,
    /** User asked for less transparency. */
    val reduceTransparency: Boolean,
) {
    val useBlur: Boolean get() = blurAvailable && !reduceTransparency
}

val LocalKiezColors = staticCompositionLocalOf { DarkKiez }
val LocalKiezMotion = staticCompositionLocalOf { KiezMotion(false) }
val LocalGlassSettings = staticCompositionLocalOf { GlassSettings(blurAvailable = false, reduceTransparency = false) }

object Kiez {
    val colors: KiezColors @Composable get() = LocalKiezColors.current
    val motion: KiezMotion @Composable get() = LocalKiezMotion.current
    val glass: GlassSettings @Composable get() = LocalGlassSettings.current
}

fun kiezTypography(c: KiezColors) = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, color = c.textPrimary),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, color = c.textPrimary),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = c.textPrimary),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp, color = c.textPrimary),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, color = c.textPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = c.textSecondary),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, color = c.textSecondary),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp, color = c.textSecondary),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp, color = c.textSecondary),
)

@Composable
fun KiezTheme(
    dark: Boolean,
    reduceMotion: Boolean,
    glass: GlassSettings,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) DarkKiez else LightKiez
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.background,
            surface = colors.backgroundDeep, onSurface = colors.textPrimary, onBackground = colors.textPrimary,
            surfaceVariant = colors.card, onSurfaceVariant = colors.textSecondary, outline = colors.cardBorder,
            secondary = colors.deal, onSecondary = colors.onDeal, error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent, background = colors.background,
            surface = colors.background, onSurface = colors.textPrimary, onBackground = colors.textPrimary,
            surfaceVariant = colors.card, onSurfaceVariant = colors.textSecondary, outline = colors.cardBorder,
            secondary = colors.deal, onSecondary = colors.onDeal, error = colors.danger,
        )
    }
    CompositionLocalProvider(
        LocalKiezColors provides colors,
        LocalKiezMotion provides KiezMotion(reduceMotion),
        LocalGlassSettings provides glass,
    ) {
        MaterialTheme(colorScheme = scheme, typography = kiezTypography(colors), content = content)
    }
}

/** Price text: tabular figures so prices line up in lists. */
val PriceFeatures = "tnum"
