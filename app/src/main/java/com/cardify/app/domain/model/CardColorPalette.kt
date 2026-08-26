package com.cardify.app.domain.model

import androidx.compose.ui.graphics.Color

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

    fun getColor(hex: String, default: Color = Color(0xFF1A73E8)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            default
        }
    }
}
