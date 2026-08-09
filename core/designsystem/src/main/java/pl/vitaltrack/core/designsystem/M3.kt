package pl.vitaltrack.core.designsystem

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import pl.vitaltrack.core.common.Hub
import pl.vitaltrack.core.common.SettingsPort

/**
 * Material 3 — tokeny i typografia. Systemowa czcionka (Roboto) zamiast
 * bundlowanej → mniejszy APK i natywny wygląd Material. Paleta „ocean”
 * (teal→blue) generowana tonalnie.
 */
object M3 {
    var dark = true

    // role kolorów
    var primary = 0; var onPrimary = 0; var primaryContainer = 0; var onPrimaryContainer = 0
    var secondary = 0; var onSecondary = 0; var secondaryContainer = 0; var onSecondaryContainer = 0
    var tertiary = 0; var onTertiary = 0
    var error = 0; var onError = 0; var errorContainer = 0; var onErrorContainer = 0
    var background = 0; var onBackground = 0
    var surface = 0; var onSurface = 0
    var surfaceVariant = 0; var onSurfaceVariant = 0
    var outline = 0; var outlineVariant = 0
    var scrim = 0
    var surfaceContainerLowest = 0; var surfaceContainerLow = 0; var surfaceContainer = 0
    var surfaceContainerHigh = 0; var surfaceContainerHighest = 0

    // semantyka (custom)
    var good = 0; var warn = 0; var danger = 0; var info = 0

    // aliasy wsteczne (ekrany)
    var bg = 0; var card = 0; var cardAlt = 0; var border = 0
    var text = 0; var textSec = 0; var textFaint = 0
    var accent = 0; var accent2 = 0; var accentSoft = 0; var onAccent = 0

    // typografia (system)
    val regular: Typeface get() = Typeface.create("sans-serif", Typeface.NORMAL)
    val medium: Typeface get() = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val semi: Typeface get() = Typeface.create("sans-serif", Typeface.BOLD)
    val bold: Typeface get() = Typeface.create("sans-serif-black", Typeface.NORMAL)

    object Space { const val xs = 4; const val sm = 8; const val md = 16; const val lg = 24; const val xl = 32 }
    object Shape { const val SMALL = 8; const val MEDIUM = 12; const val LARGE = 16; const val XL = 28 }
    object Motion { const val SHORT = 150L; const val MEDIUM = 300L }
    object Layer { const val PRESSED = 0.12f }

    private var density = 2f

    fun apply(c: Context) {
        density = c.resources.displayMetrics.density
        dark = if (Hub.ok()) Hub.settings.getBool(SettingsPort.Keys.DARK, true) else true
        val ai = if (Hub.ok()) Hub.settings.getInt(SettingsPort.Keys.ACCENT, 0) else 0
        scheme(if (ai == 1) tealDark() else tealDark(), if (ai == 1) tealLight() else tealLight())
        if (!dark) scheme(tealLight(), tealLight())
    }

    /** Paleta „ocean”: teal #00696D → blue #0061A4 (M3 tonalnie uproszczona). */
    private fun tealLight() = ColorPalette(
        primary = 0xFF006A6A.toInt(), onPrimary = 0xFFFFFFFF.toInt(),
        primaryContainer = 0xFF9CF1F0.toInt(), onPrimaryContainer = 0xFF002020.toInt(),
        secondary = 0xFF4A6363.toInt(), onSecondary = 0xFFFFFFFF.toInt(),
        secondaryContainer = 0xFFCCE8E7.toInt(), onSecondaryContainer = 0xFF051F1F.toInt(),
        tertiary = 0xFF00639B.toInt(), onTertiary = 0xFFFFFFFF.toInt(),
        background = 0xFFFAFDFC.toInt(), onBackground = 0xFF191C1C.toInt(),
        surface = 0xFFFAFDFC.toInt(), onSurface = 0xFF191C1C.toInt(),
        surfaceVariant = 0xFFDAE5E4.toInt(), onSurfaceVariant = 0xFF3F4948.toInt(),
        outline = 0xFF6F7979.toInt(), outlineVariant = 0xFFBEC9C8.toInt(),
        error = 0xFFBA1A1A.toInt(), onError = 0xFFFFFFFF.toInt(),
        errorContainer = 0xFFFFDAD6.toInt(), onErrorContainer = 0xFF410002.toInt(),
        surfaceContainerLowest = 0xFFFFFFFF.toInt(), surfaceContainerLow = 0xFFF4F7F6.toInt(),
        surfaceContainer = 0xFFEEF1F0.toInt(), surfaceContainerHigh = 0xFFE8EBEB.toInt(),
        surfaceContainerHighest = 0xFFE2E6E5.toInt(),
    )

    private fun tealDark() = ColorPalette(
        primary = 0xFF80D5D4.toInt(), onPrimary = 0xFF003737.toInt(),
        primaryContainer = 0xFF004F4F.toInt(), onPrimaryContainer = 0xFF9CF1F0.toInt(),
        secondary = 0xFFB0CCCB.toInt(), onSecondary = 0xFF1B3534.toInt(),
        secondaryContainer = 0xFF324B4B.toInt(), onSecondaryContainer = 0xFFCCE8E7.toInt(),
        tertiary = 0xFF9BCBEF.toInt(), onTertiary = 0xFF00344F.toInt(),
        background = 0xFF191C1C.toInt(), onBackground = 0xFFE0E3E2.toInt(),
        surface = 0xFF191C1C.toInt(), onSurface = 0xFFE0E3E2.toInt(),
        surfaceVariant = 0xFF3F4948.toInt(), onSurfaceVariant = 0xFFBEC9C8.toInt(),
        outline = 0xFF889392.toInt(), outlineVariant = 0xFF3F4948.toInt(),
        error = 0xFFFFB4AB.toInt(), onError = 0xFF690005.toInt(),
        errorContainer = 0xFF93000A.toInt(), onErrorContainer = 0xFFFFDAD6.toInt(),
        surfaceContainerLowest = 0xFF141717.toInt(), surfaceContainerLow = 0xFF191C1C.toInt(),
        surfaceContainer = 0xFF1D2020.toInt(), surfaceContainerHigh = 0xFF272B2A.toInt(),
        surfaceContainerHighest = 0xFF323635.toInt(),
    )

    private fun scheme(p: ColorPalette, l: ColorPalette) {
        val s = if (dark) p else l
        primary = s.primary; onPrimary = s.onPrimary; primaryContainer = s.primaryContainer
        onPrimaryContainer = s.onPrimaryContainer
        secondary = s.secondary; onSecondary = s.onSecondary; secondaryContainer = s.secondaryContainer
        onSecondaryContainer = s.onSecondaryContainer
        tertiary = s.tertiary; onTertiary = s.onTertiary
        error = s.error; onError = s.onError; errorContainer = s.errorContainer; onErrorContainer = s.onErrorContainer
        background = s.background; onBackground = s.onBackground
        surface = s.surface; onSurface = s.onSurface
        surfaceVariant = s.surfaceVariant; onSurfaceVariant = s.onSurfaceVariant
        outline = s.outline; outlineVariant = s.outlineVariant
        surfaceContainerLowest = s.surfaceContainerLowest; surfaceContainerLow = s.surfaceContainerLow
        surfaceContainer = s.surfaceContainer; surfaceContainerHigh = s.surfaceContainerHigh
        surfaceContainerHighest = s.surfaceContainerHighest
        scrim = 0xFF000000.toInt()

        good = if (dark) 0xFF80D5D4.toInt() else 0xFF00696D.toInt()
        warn = if (dark) 0xFFFFD180.toInt() else 0xFF8A5600.toInt()
        danger = if (dark) 0xFFFFB4AB.toInt() else 0xFFB3261E.toInt()
        info = if (dark) 0xFF9BCBEF.toInt() else 0xFF00639B.toInt()

        bg = background; card = surfaceContainerLow; cardAlt = surfaceContainerHighest
        border = outlineVariant; text = onSurface; textSec = onSurfaceVariant; textFaint = outline
        accent = primary; accent2 = secondary; accentSoft = primaryContainer; onAccent = onPrimary
    }

    fun dp(v: Float): Int = (v * density + 0.5f).toInt()
    fun sp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
        android.content.res.Resources.getSystem().displayMetrics).toInt()
    fun withAlpha(color: Int, a: Int): Int = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    fun layer(base: Int, content: Int, a: Float): Int {
        val al = (a.coerceIn(0f, 1f) * 255).toInt()
        val inv = 255 - al
        return Color.rgb(
            (Color.red(base) * inv + Color.red(content) * al) / 255,
            (Color.green(base) * inv + Color.green(content) * al) / 255,
            (Color.blue(base) * inv + Color.blue(content) * al) / 255,
        )
    }
    fun mix(c1: Int, c2: Int, t: Float): Int = Color.rgb(
        (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * t).toInt(),
        (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t).toInt(),
        (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * t).toInt(),
    )
    fun hero(): IntArray = if (dark) intArrayOf(layer(surfaceContainer, primary, 0.30f), surfaceContainerLow)
    else intArrayOf(layer(surface, primary, 0.12f), surfaceContainerLow)
}

private data class ColorPalette(
    val primary: Int, val onPrimary: Int, val primaryContainer: Int, val onPrimaryContainer: Int,
    val secondary: Int, val onSecondary: Int, val secondaryContainer: Int, val onSecondaryContainer: Int,
    val tertiary: Int, val onTertiary: Int,
    val error: Int, val onError: Int, val errorContainer: Int, val onErrorContainer: Int,
    val background: Int, val onBackground: Int, val surface: Int, val onSurface: Int,
    val surfaceVariant: Int, val onSurfaceVariant: Int, val outline: Int, val outlineVariant: Int,
    val surfaceContainerLowest: Int, val surfaceContainerLow: Int, val surfaceContainer: Int,
    val surfaceContainerHigh: Int, val surfaceContainerHighest: Int,
)
