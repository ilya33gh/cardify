package com.cardify.app.domain.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

data class AdaptiveColorOption(
    val id: String,
    val name: String,
    val lightHex: String,
    val darkHex: String,
    val legacyHex: String,
    val lightTextHex: String,
    val darkTextHex: String
) {
    // Backwards compatibility alias for primaryHex
    val primaryHex: String get() = id
}

// Backwards compatibility typealias
typealias ExpressiveColorOption = AdaptiveColorOption

object CardColorPalette {
    val options = listOf(
        AdaptiveColorOption("red", "Красный", "#FCA5A5", "#9E1B24", "#EA4335", "#4F141B", "#FFEBEF"),
        AdaptiveColorOption("blue", "Синий", "#93C5FD", "#1E448A", "#1A73E8", "#0F2847", "#EBF3FF"),
        AdaptiveColorOption("green", "Зеленый", "#86EFAC", "#156836", "#34A853", "#0B381C", "#EAFAEC"),
        AdaptiveColorOption("orange", "Оранжевый", "#FDBA74", "#A8380A", "#FF6D00", "#4A1E06", "#FFF2EA"),
        AdaptiveColorOption("purple", "Фиолетовый", "#C4B5FD", "#5B21B6", "#8E24AA", "#2C125E", "#F6EEFF"),
        AdaptiveColorOption("cyan", "Бирюзовый", "#67E8F9", "#09607A", "#00B4D8", "#062E3B", "#E6FAFF"),
        AdaptiveColorOption("yellow", "Золотой", "#FCD34D", "#9A5805", "#FBBC04", "#3E2400", "#FFF8E7"),
        AdaptiveColorOption("indigo", "Индиго", "#A5B4FC", "#312E81", "#3949AB", "#1E1B5E", "#EEF0FF"),
        AdaptiveColorOption("pink", "Розовый", "#F472B6", "#881343", "#E91E63", "#420C26", "#FFEBF4"),
        AdaptiveColorOption("emerald", "Мятный / Нефрит", "#5EEAD4", "#0F6B64", "#00897B", "#093632", "#E6FAF7"),
        AdaptiveColorOption("coral", "Карамель / Мокко", "#D4BFA8", "#883D24", "#D97706", "#3E2114", "#FFF0EB"),
        AdaptiveColorOption("graphite", "Графит", "#CBD5E1", "#282E3A", "#212121", "#1E2430", "#F1F4F9")
    )

    private val idToOptionMap: Map<String, AdaptiveColorOption> by lazy {
        options.associateBy { it.id.lowercase() }
    }

    private val legacyHexToOptionMap: Map<String, AdaptiveColorOption> by lazy {
        options.associateBy { it.legacyHex.lowercase() }
    }

    fun findOption(key: String): AdaptiveColorOption? {
        val normalized = key.trim().lowercase()
        return idToOptionMap[normalized] ?: legacyHexToOptionMap[normalized]
    }

    fun parseColorSafe(hex: String, fallback: Color = Color(0xFF93C5FD)): Color {
        return try {
            val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(cleanHex))
        } catch (e: Exception) {
            fallback
        }
    }

    /**
     * Finds the closest AdaptiveColorOption for any given Color based on HSL.
     */
    fun findClosestOption(color: Color): AdaptiveColorOption {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), hsl)
        val hue = hsl[0]
        val sat = hsl[1]

        if (sat < 0.15f) return idToOptionMap["graphite"] ?: options.last()

        return when {
            hue in 350f..360f || hue in 0f..15f -> idToOptionMap["red"]
            hue in 15f..40f -> idToOptionMap["orange"]
            hue in 40f..65f -> idToOptionMap["yellow"]
            hue in 65f..150f -> idToOptionMap["green"]
            hue in 150f..175f -> idToOptionMap["emerald"]
            hue in 175f..205f -> idToOptionMap["cyan"]
            hue in 205f..230f -> idToOptionMap["blue"]
            hue in 230f..260f -> idToOptionMap["indigo"]
            hue in 260f..310f -> idToOptionMap["purple"]
            hue in 310f..350f -> idToOptionMap["pink"]
            else -> idToOptionMap["blue"]
        } ?: options.first()
    }

    /**
     * Resolves the adaptive Color for a card depending on whether the UI is in dark or light theme.
     */
    fun getAdaptiveCardColor(
        colorKeyOrHex: String?,
        isDark: Boolean
    ): Color {
        if (colorKeyOrHex.isNullOrBlank()) {
            return parseColorSafe(if (isDark) "#1E448A" else "#93C5FD")
        }

        val option = findOption(colorKeyOrHex)
        if (option != null) {
            return parseColorSafe(if (isDark) option.darkHex else option.lightHex)
        }

        // Check if it's an integer ARGB string (like Catima "-769226")
        colorKeyOrHex.toIntOrNull()?.let { intArgb ->
            val color = Color(intArgb)
            val matched = findClosestOption(color)
            return parseColorSafe(if (isDark) matched.darkHex else matched.lightHex)
        }

        // Custom hex fallback
        return try {
            val parsed = parseColorSafe(colorKeyOrHex)
            val matched = findClosestOption(parsed)
            parseColorSafe(if (isDark) matched.darkHex else matched.lightHex)
        } catch (e: Exception) {
            parseColorSafe(if (isDark) "#1E448A" else "#93C5FD")
        }
    }

    /**
     * Resolves the adaptive Color for a card within a Composable.
     */
    @Composable
    fun getAdaptiveColor(
        colorKeyOrHex: String?,
        isDark: Boolean = isDarkTheme()
    ): Color {
        return remember(colorKeyOrHex, isDark) {
            getAdaptiveCardColor(colorKeyOrHex, isDark)
        }
    }

    /**
     * Backwards compatibility alias for getHarmonizedColor
     */
    @Composable
    fun getHarmonizedColor(
        colorKeyOrHex: String?,
        systemPrimary: Color = MaterialTheme.colorScheme.primary,
        default: Color = Color(0xFF93C5FD)
    ): Color {
        return getAdaptiveColor(colorKeyOrHex)
    }

    /**
     * Tonal on-container content color for text and icons on cards:
     * - Dark / OLED: Very light luminous color tinted with the main card hue (e.g. #EBF3FF on blue, #FFEBEF on red)
     * - Light: Harmonious deep tonal color (e.g. #0F2847 on blue, #4F141B on red)
     */
    @Composable
    fun getCardContentColor(
        colorKeyOrHex: String? = null,
        isDark: Boolean = isDarkTheme()
    ): Color {
        if (colorKeyOrHex.isNullOrBlank()) {
            return parseColorSafe(if (isDark) "#EBF3FF" else "#0F2847")
        }

        val option = findOption(colorKeyOrHex)
        if (option != null) {
            return parseColorSafe(if (isDark) option.darkTextHex else option.lightTextHex)
        }

        colorKeyOrHex.toIntOrNull()?.let { intArgb ->
            val color = Color(intArgb)
            val matched = findClosestOption(color)
            return parseColorSafe(if (isDark) matched.darkTextHex else matched.lightTextHex)
        }

        return try {
            val parsed = parseColorSafe(colorKeyOrHex)
            val matched = findClosestOption(parsed)
            parseColorSafe(if (isDark) matched.darkTextHex else matched.lightTextHex)
        } catch (e: Exception) {
            if (isDark) Color(0xFFF1F4F9) else Color(0xFF1E2430)
        }
    }

    @Composable
    fun getCardSecondaryContentColor(
        colorKeyOrHex: String? = null,
        isDark: Boolean = isDarkTheme()
    ): Color {
        val base = getCardContentColor(colorKeyOrHex, isDark)
        return if (isDark) base.copy(alpha = 0.85f) else base.copy(alpha = 0.80f)
    }

    /**
     * Badge background container (Monogram circle & Category pill):
     * - Dark / OLED: Subtle translucent black
     * - Light: Frosted semi-translucent white glass
     */
    @Composable
    fun getCardBadgeContainerColor(isDark: Boolean = isDarkTheme()): Color {
        return if (isDark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.55f)
    }

    /**
     * Helper to detect if current theme is dark or OLED.
     */
    @Composable
    fun isDarkTheme(): Boolean {
        val surface = MaterialTheme.colorScheme.surface
        val r = surface.red
        val g = surface.green
        val b = surface.blue
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        return luminance < 0.5f
    }
}

