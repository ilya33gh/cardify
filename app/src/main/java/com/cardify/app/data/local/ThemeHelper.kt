package com.cardify.app.data.local

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val code: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark"),
    OLED("oled");

    companion object {
        fun fromCode(code: String): ThemeMode {
            return entries.firstOrNull { it.code == code } ?: AUTO
        }
    }
}

object ThemeHelper {
    private const val PREFS_NAME = "cardify_theme_prefs"
    private const val KEY_THEME_MODE = "selected_theme_mode"
    private const val KEY_DYNAMIC_COLOR = "selected_dynamic_color"
    private const val KEY_LAYOUT_MODE = "selected_layout_mode"

    private val _themeMode = MutableStateFlow(ThemeMode.AUTO)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDynamicColorEnabled = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val isDynamicColorEnabled: StateFlow<Boolean> = _isDynamicColorEnabled.asStateFlow()

    private val _layoutMode = MutableStateFlow("FULL_CARDS")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_THEME_MODE, ThemeMode.AUTO.code) ?: ThemeMode.AUTO.code
        _themeMode.value = ThemeMode.fromCode(code)

        val defaultDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val dynamic = prefs.getBoolean(KEY_DYNAMIC_COLOR, defaultDynamic)
        _isDynamicColorEnabled.value = dynamic

        _layoutMode.value = prefs.getString(KEY_LAYOUT_MODE, "FULL_CARDS") ?: "FULL_CARDS"
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.code).apply()
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _isDynamicColorEnabled.value = enabled
    }

    fun setLayoutMode(context: Context, modeName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAYOUT_MODE, modeName).apply()
        _layoutMode.value = modeName
    }
}
