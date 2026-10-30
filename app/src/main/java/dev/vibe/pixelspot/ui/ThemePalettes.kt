package dev.vibe.pixelspot.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.vibe.pixelspot.data.NamedPalette

/** The colours of one version (light or dark) of a theme palette. */
private class Palette(
    val background: Long,
    val lowest: Long, val low: Long, val mid: Long, val high: Long, val highest: Long,
    val text: Long, val subtext: Long, val outline: Long, val outlineVariant: Long,
    val primary: Long, val onPrimary: Long, val primaryContainer: Long, val onPrimaryContainer: Long,
    val secondary: Long, val onSecondary: Long, val secondaryContainer: Long, val onSecondaryContainer: Long,
    val tertiary: Long, val onTertiary: Long, val tertiaryContainer: Long, val onTertiaryContainer: Long,
    val error: Long, val onError: Long,
)

private fun Palette.toScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary), onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(primaryContainer),
        secondary = Color(secondary), onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer), onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary), onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer), onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background), onBackground = Color(text),
        surface = Color(background), onSurface = Color(text),
        surfaceVariant = Color(mid), onSurfaceVariant = Color(subtext),
        surfaceTint = Color(primary),
        inverseSurface = Color(text), inverseOnSurface = Color(background),
        error = Color(error), onError = Color(onError),
        outline = Color(outline), outlineVariant = Color(outlineVariant),
        surfaceBright = Color(high), surfaceDim = Color(lowest),
        surfaceContainerLowest = Color(lowest), surfaceContainerLow = Color(low),
        surfaceContainer = Color(mid), surfaceContainerHigh = Color(high),
        surfaceContainerHighest = Color(highest),
        // The player's big buttons use these; they stay the palette's accent colours.
        primaryFixed = Color(primary), primaryFixedDim = Color(primary), onPrimaryFixed = Color(onPrimary),
        secondaryFixed = Color(secondary), secondaryFixedDim = Color(secondary), onSecondaryFixed = Color(onSecondary),
        tertiaryFixed = Color(tertiary), tertiaryFixedDim = Color(tertiary), onTertiaryFixed = Color(onTertiary),
    )
}

fun NamedPalette.scheme(dark: Boolean): ColorScheme = when (this) {
    NamedPalette.ROSE_PINE -> if (dark) RosePine.toScheme(true) else RosePineDawn.toScheme(false)
    NamedPalette.GRUVBOX -> if (dark) GruvboxDark.toScheme(true) else GruvboxLight.toScheme(false)
    NamedPalette.CATPPUCCIN -> if (dark) CatppuccinMocha.toScheme(true) else CatppuccinLatte.toScheme(false)
    NamedPalette.NORD -> if (dark) NordDark.toScheme(true) else NordLight.toScheme(false)
    NamedPalette.ATOM_ONE -> if (dark) OneDark.toScheme(true) else OneLight.toScheme(false)
    NamedPalette.ZINC -> if (dark) ZincDark.toScheme(true) else ZincLight.toScheme(false)
}

private val RosePine = Palette(
    background = 0xFF191724,
    lowest = 0xFF16141F, low = 0xFF1F1D2E, mid = 0xFF26233A, high = 0xFF2A2740, highest = 0xFF403D52,
    text = 0xFFE0DEF4, subtext = 0xFF908CAA, outline = 0xFF6E6A86, outlineVariant = 0xFF403D52,
    primary = 0xFFC4A7E7, onPrimary = 0xFF191724, primaryContainer = 0xFF433A63, onPrimaryContainer = 0xFFE0DEF4,
    secondary = 0xFF9CCFD8, onSecondary = 0xFF191724, secondaryContainer = 0xFF2B5A6A, onSecondaryContainer = 0xFFE0DEF4,
    tertiary = 0xFFC4A7E7, onTertiary = 0xFF191724, tertiaryContainer = 0xFF433A63, onTertiaryContainer = 0xFFE0DEF4,
    error = 0xFFEB6F92, onError = 0xFF191724,
)

private val RosePineDawn = Palette(
    background = 0xFFFAF4ED,
    lowest = 0xFFFFFAF3, low = 0xFFF4EDE8, mid = 0xFFF2E9E1, high = 0xFFDFDAD9, highest = 0xFFCECACD,
    text = 0xFF575279, subtext = 0xFF797593, outline = 0xFF9893A5, outlineVariant = 0xFFDFDAD9,
    primary = 0xFF907AA9, onPrimary = 0xFFFAF4ED, primaryContainer = 0xFFE4D8EF, onPrimaryContainer = 0xFF575279,
    secondary = 0xFF56949F, onSecondary = 0xFFFAF4ED, secondaryContainer = 0xFFD3E6E8, onSecondaryContainer = 0xFF286983,
    tertiary = 0xFF907AA9, onTertiary = 0xFFFAF4ED, tertiaryContainer = 0xFFE4D8EF, onTertiaryContainer = 0xFF575279,
    error = 0xFFB4637A, onError = 0xFFFAF4ED,
)

private val GruvboxDark = Palette(
    background = 0xFF282828,
    lowest = 0xFF1D2021, low = 0xFF32302F, mid = 0xFF3C3836, high = 0xFF504945, highest = 0xFF665C54,
    text = 0xFFEBDBB2, subtext = 0xFFD5C4A1, outline = 0xFF928374, outlineVariant = 0xFF504945,
    primary = 0xFFFABD2F, onPrimary = 0xFF282828, primaryContainer = 0xFF8F5F0F, onPrimaryContainer = 0xFFFBF1C7,
    secondary = 0xFF83A598, onSecondary = 0xFF282828, secondaryContainer = 0xFF1F5C66, onSecondaryContainer = 0xFFEBDBB2,
    tertiary = 0xFFFE8019, onTertiary = 0xFF282828, tertiaryContainer = 0xFF7A3E0C, onTertiaryContainer = 0xFFFBF1C7,
    error = 0xFFFB4934, onError = 0xFF282828,
)

private val GruvboxLight = Palette(
    background = 0xFFFBF1C7,
    lowest = 0xFFF9F5D7, low = 0xFFF2E5BC, mid = 0xFFEBDBB2, high = 0xFFD5C4A1, highest = 0xFFBDAE93,
    text = 0xFF3C3836, subtext = 0xFF504945, outline = 0xFF7C6F64, outlineVariant = 0xFFD5C4A1,
    primary = 0xFFB57614, onPrimary = 0xFFFBF1C7, primaryContainer = 0xFFFABD2F, onPrimaryContainer = 0xFF3C3836,
    secondary = 0xFF076678, onSecondary = 0xFFFBF1C7, secondaryContainer = 0xFF83A598, onSecondaryContainer = 0xFF282828,
    tertiary = 0xFFAF3A03, onTertiary = 0xFFFBF1C7, tertiaryContainer = 0xFFFFC48A, onTertiaryContainer = 0xFF3C3836,
    error = 0xFF9D0006, onError = 0xFFFBF1C7,
)

private val CatppuccinMocha = Palette(
    background = 0xFF1E1E2E,
    lowest = 0xFF11111B, low = 0xFF181825, mid = 0xFF313244, high = 0xFF45475A, highest = 0xFF585B70,
    text = 0xFFCDD6F4, subtext = 0xFFBAC2DE, outline = 0xFF6C7086, outlineVariant = 0xFF45475A,
    primary = 0xFFCBA6F7, onPrimary = 0xFF1E1E2E, primaryContainer = 0xFF45385F, onPrimaryContainer = 0xFFCDD6F4,
    secondary = 0xFF89B4FA, onSecondary = 0xFF1E1E2E, secondaryContainer = 0xFF2F4468, onSecondaryContainer = 0xFFCDD6F4,
    tertiary = 0xFFCBA6F7, onTertiary = 0xFF1E1E2E, tertiaryContainer = 0xFF45385F, onTertiaryContainer = 0xFFCDD6F4,
    error = 0xFFF38BA8, onError = 0xFF1E1E2E,
)

private val CatppuccinLatte = Palette(
    background = 0xFFEFF1F5,
    lowest = 0xFFFFFFFF, low = 0xFFE6E9EF, mid = 0xFFDCE0E8, high = 0xFFCCD0DA, highest = 0xFFBCC0CC,
    text = 0xFF4C4F69, subtext = 0xFF5C5F77, outline = 0xFF9CA0B0, outlineVariant = 0xFFCCD0DA,
    primary = 0xFF8839EF, onPrimary = 0xFFEFF1F5, primaryContainer = 0xFFE3D3FB, onPrimaryContainer = 0xFF4C4F69,
    secondary = 0xFF1E66F5, onSecondary = 0xFFEFF1F5, secondaryContainer = 0xFFD2E0FC, onSecondaryContainer = 0xFF4C4F69,
    tertiary = 0xFF8839EF, onTertiary = 0xFFEFF1F5, tertiaryContainer = 0xFFE3D3FB, onTertiaryContainer = 0xFF4C4F69,
    error = 0xFFD20F39, onError = 0xFFEFF1F5,
)

private val NordDark = Palette(
    background = 0xFF2E3440,
    lowest = 0xFF272C36, low = 0xFF3B4252, mid = 0xFF434C5E, high = 0xFF4C566A, highest = 0xFF5B667D,
    text = 0xFFECEFF4, subtext = 0xFFD8DEE9, outline = 0xFF7B88A1, outlineVariant = 0xFF4C566A,
    primary = 0xFF88C0D0, onPrimary = 0xFF2E3440, primaryContainer = 0xFF3F5A68, onPrimaryContainer = 0xFFECEFF4,
    secondary = 0xFF81A1C1, onSecondary = 0xFF2E3440, secondaryContainer = 0xFF3F5470, onSecondaryContainer = 0xFFECEFF4,
    tertiary = 0xFF88C0D0, onTertiary = 0xFF2E3440, tertiaryContainer = 0xFF3F5A68, onTertiaryContainer = 0xFFECEFF4,
    error = 0xFFBF616A, onError = 0xFFECEFF4,
)

private val NordLight = Palette(
    background = 0xFFECEFF4,
    lowest = 0xFFF5F7FA, low = 0xFFE5E9F0, mid = 0xFFD8DEE9, high = 0xFFCBD3E0, highest = 0xFFBCC6D6,
    text = 0xFF2E3440, subtext = 0xFF4C566A, outline = 0xFF7B88A1, outlineVariant = 0xFFCBD3E0,
    primary = 0xFF5E81AC, onPrimary = 0xFFECEFF4, primaryContainer = 0xFFCFE0EA, onPrimaryContainer = 0xFF2E3440,
    secondary = 0xFF4C7A8C, onSecondary = 0xFFECEFF4, secondaryContainer = 0xFFD3E4E8, onSecondaryContainer = 0xFF2E3440,
    tertiary = 0xFF88C0D0, onTertiary = 0xFF2E3440, tertiaryContainer = 0xFFCFE0EA, onTertiaryContainer = 0xFF2E3440,
    error = 0xFFBF616A, onError = 0xFFECEFF4,
)

private val OneDark = Palette(
    background = 0xFF282C34,
    lowest = 0xFF21252B, low = 0xFF2C313A, mid = 0xFF333842, high = 0xFF3E4451, highest = 0xFF4B5263,
    text = 0xFFD7DAE0, subtext = 0xFFABB2BF, outline = 0xFF5C6370, outlineVariant = 0xFF3E4451,
    primary = 0xFF61AFEF, onPrimary = 0xFF282C34, primaryContainer = 0xFF2F4A63, onPrimaryContainer = 0xFFD7DAE0,
    secondary = 0xFFC678DD, onSecondary = 0xFF282C34, secondaryContainer = 0xFF4E3658, onSecondaryContainer = 0xFFD7DAE0,
    tertiary = 0xFF61AFEF, onTertiary = 0xFF282C34, tertiaryContainer = 0xFF2F4A63, onTertiaryContainer = 0xFFD7DAE0,
    error = 0xFFE06C75, onError = 0xFF282C34,
)

private val OneLight = Palette(
    background = 0xFFFAFAFA,
    lowest = 0xFFFFFFFF, low = 0xFFF0F0F0, mid = 0xFFEAEAEB, high = 0xFFE5E5E6, highest = 0xFFDBDBDC,
    text = 0xFF383A42, subtext = 0xFF696C77, outline = 0xFFA0A1A7, outlineVariant = 0xFFDBDBDC,
    primary = 0xFF4078F2, onPrimary = 0xFFFAFAFA, primaryContainer = 0xFFD9E4FC, onPrimaryContainer = 0xFF383A42,
    secondary = 0xFFA626A4, onSecondary = 0xFFFAFAFA, secondaryContainer = 0xFFF3DAF2, onSecondaryContainer = 0xFF383A42,
    tertiary = 0xFF4078F2, onTertiary = 0xFFFAFAFA, tertiaryContainer = 0xFFD9E4FC, onTertiaryContainer = 0xFF383A42,
    error = 0xFFE45649, onError = 0xFFFAFAFA,
)

// Tailwind's neutral Zinc greys: the accent is near-white in dark mode and near-black in light mode.
private val ZincDark = Palette(
    background = 0xFF18181B,
    lowest = 0xFF09090B, low = 0xFF1F1F23, mid = 0xFF27272A, high = 0xFF3F3F46, highest = 0xFF52525B,
    text = 0xFFFAFAFA, subtext = 0xFFA1A1AA, outline = 0xFF71717A, outlineVariant = 0xFF3F3F46,
    primary = 0xFFFAFAFA, onPrimary = 0xFF18181B, primaryContainer = 0xFF3F3F46, onPrimaryContainer = 0xFFFAFAFA,
    secondary = 0xFFD4D4D8, onSecondary = 0xFF18181B, secondaryContainer = 0xFF27272A, onSecondaryContainer = 0xFFFAFAFA,
    tertiary = 0xFFFAFAFA, onTertiary = 0xFF18181B, tertiaryContainer = 0xFF3F3F46, onTertiaryContainer = 0xFFFAFAFA,
    error = 0xFFF87171, onError = 0xFF18181B,
)

private val ZincLight = Palette(
    background = 0xFFFFFFFF,
    lowest = 0xFFFFFFFF, low = 0xFFFAFAFA, mid = 0xFFF4F4F5, high = 0xFFE4E4E7, highest = 0xFFD4D4D8,
    text = 0xFF18181B, subtext = 0xFF52525B, outline = 0xFFA1A1AA, outlineVariant = 0xFFE4E4E7,
    primary = 0xFF18181B, onPrimary = 0xFFFAFAFA, primaryContainer = 0xFFE4E4E7, onPrimaryContainer = 0xFF18181B,
    secondary = 0xFF3F3F46, onSecondary = 0xFFFAFAFA, secondaryContainer = 0xFFF4F4F5, onSecondaryContainer = 0xFF18181B,
    tertiary = 0xFF18181B, onTertiary = 0xFFFAFAFA, tertiaryContainer = 0xFFE4E4E7, onTertiaryContainer = 0xFF18181B,
    error = 0xFFDC2626, onError = 0xFFFAFAFA,
)
