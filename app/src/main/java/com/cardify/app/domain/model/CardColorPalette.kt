package com.cardify.app.domain.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

data class ExpressiveColorOption(
    val id: String,
    val name: String,
    val primaryHex: String,
    val secondaryHex: String,
    val surfaceColorHex: String
)

object CardColorPalette {
    val options = listOf(
        ExpressiveColorOption("google_blue", "Google Синий", "#1A73E8", "#4285F4", "#E8F0FE"),
        ExpressiveColorOption("google_red", "Google Красный", "#EA4335", "#FF7043", "#FCE8E6"),
        ExpressiveColorOption("google_yellow", "Google Желтый", "#FBBC04", "#FFA000", "#FEF7E0"),
        ExpressiveColorOption("google_green", "Google Зеленый", "#34A853", "#0F9D58", "#E6F4EA"),
        ExpressiveColorOption("neon_purple", "Неоновый Фиолетовый", "#8E24AA", "#BA68C8", "#F3E5F5"),
        ExpressiveColorOption("sunset_orange", "Закатный Оранжевый", "#FF6D00", "#FF9E80", "#FFF3E0"),
        ExpressiveColorOption("cyber_cyan", "Кибернетический Циан", "#00B4D8", "#48CAE4", "#E0F7FA"),
        ExpressiveColorOption("deep_indigo", "Глубокий Индиго", "#3949AB", "#5C6BC0", "#E8EAF6"),
        ExpressiveColorOption("vibrant_pink", "Яркий Розовый", "#E91E63", "#F06292", "#FCE4EC"),
        ExpressiveColorOption("emerald", "Изумрудный", "#00897B", "#26A69A", "#E0F2F1"),
        ExpressiveColorOption("carbon_dark", "Карбон Графит", "#212121", "#424242", "#EEEEEE"),
        ExpressiveColorOption("warm_amber", "Теплый Янтарь", "#D97706", "#F59E0B", "#FEF3C7")
    )

    /**
     * Resolves the raw RGB color from a hex string without theme harmonization.
     */
    fun getColor(hex: String, default: Color = Color(0xFF1A73E8)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            default
        }
    }

    /**
     * Material 3 Color Harmonization algorithm.
     * Applies an ultra-subtle, refined Hue shift (max 5°) and gentle Chroma harmonization
     * towards the system's dynamic Monet primary.
     * This guarantees that Red is always unequivocally Red, Blue is always Blue,
     * Green is always Green, etc., while still adding a cohesive ambient temperature
     * matching the system theme.
     */
    fun harmonize(
        baseColor: Color,
        systemPrimary: Color,
        maxHueShiftDegrees: Float = 5f,
        hueShiftFactor: Float = 0.05f,
        chromaBlendFactor: Float = 0.03f
    ): Color {
        val baseHsl = FloatArray(3)
        val sysHsl = FloatArray(3)

        ColorUtils.colorToHSL(baseColor.toArgb(), baseHsl)
        ColorUtils.colorToHSL(systemPrimary.toArgb(), sysHsl)

        val hBase = baseHsl[0]
        val sBase = baseHsl[1]
        val lBase = baseHsl[2]

        val hSys = sysHsl[0]
        val sSys = sysHsl[1]

        // Neutral colors like Carbon Graphite (#212121) get very subtle ambient tinting without hue jumping
        if (sBase < 0.08f) {
            val blended = ColorUtils.blendARGB(baseColor.toArgb(), systemPrimary.toArgb(), 0.03f)
            return Color(blended)
        }

        // Calculate circular shortest-path delta
        var diff = (hSys - hBase) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        // Ultra-subtle hue shift (capped at 5° to prevent color drifting, e.g. red to orange)
        val shift = (diff * hueShiftFactor).coerceIn(-maxHueShiftDegrees, maxHueShiftDegrees)
        var harmonizedHue = (hBase + shift + 360f) % 360f

        // Extra safety clamp for Red: keep within true red sector [345°, 10°]
        if (hBase in 0f..15f && harmonizedHue > 10f) {
            harmonizedHue = 10f
        }

        // Very subtle chroma harmonization
        val harmonizedSat = (sBase * (1f - chromaBlendFactor) + sSys * chromaBlendFactor).coerceIn(0.40f, 1.0f)

        val resultHsl = floatArrayOf(harmonizedHue, harmonizedSat, lBase)
        return Color(ColorUtils.HSLToColor(resultHsl))
    }

    /**
     * Resolves a card's color with automatic dynamic Monet harmonization against the current theme.
     */
    @Composable
    fun getHarmonizedColor(
        hex: String,
        systemPrimary: Color = MaterialTheme.colorScheme.primary,
        default: Color = Color(0xFF1A73E8)
    ): Color {
        val base = getColor(hex, default)
        return remember(hex, systemPrimary) {
            harmonize(base, systemPrimary)
        }
    }
}

