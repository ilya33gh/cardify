package com.cardify.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HapticPreference {
    private const val PREFS_NAME = "cardify_haptic_prefs"
    private const val KEY_HAPTIC_ENABLED = "haptic_feedback_enabled"

    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isHapticEnabled.value = prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
        _isHapticEnabled.value = enabled
    }
}

class HapticHelper(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Subtle "rustling" tick effect for brightness slider dragging.
     */
    fun performTick() {
        if (!HapticPreference.isHapticEnabled.value) return
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(5L)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Standard clean click effect for buttons, tabs, card detail opening, and switches.
     */
    fun performClick() {
        if (!HapticPreference.isHapticEnabled.value) return
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12L)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Expressive heavy pulse effect for favorite toggle, save card, and card long-press.
     */
    fun performHeavyClick() {
        if (!HapticPreference.isHapticEnabled.value) return
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20L)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Realistic double-beat "lub-dub" heartbeat vibration effect.
     */
    fun performHeartbeat() {
        if (!HapticPreference.isHapticEnabled.value) return
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 16, 50, 26)
                    val amplitudes = intArrayOf(0, 180, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 16, 50, 26), -1)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Firm warning/destructive thud vibration for delete confirmation dialogs.
     */
    fun performDestructiveWarning() {
        if (!HapticPreference.isHapticEnabled.value) return
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 32, 40, 48)
                    val amplitudes = intArrayOf(0, 220, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 32, 40, 48), -1)
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun rememberHapticHelper(): HapticHelper {
    val context = LocalContext.current
    return remember(context) { HapticHelper(context) }
}
