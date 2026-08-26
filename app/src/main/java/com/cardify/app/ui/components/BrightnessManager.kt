package com.cardify.app.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun SetMaxScreenBrightnessEffect(enabled: Boolean = true) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        if (enabled && window != null) {
            val originalBrightness = window.attributes.screenBrightness
            val layoutParams = window.attributes
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = layoutParams

            onDispose {
                val restoreParams = window.attributes
                restoreParams.screenBrightness = originalBrightness
                window.attributes = restoreParams
            }
        } else {
            onDispose { }
        }
    }
}
