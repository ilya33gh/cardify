package com.cardify.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.cardify.app.data.local.SecurityHelper
import com.cardify.app.data.local.ThemeHelper
import com.cardify.app.ui.navigation.CardifyNavHost
import com.cardify.app.ui.security.BiometricAuthLockScreen
import com.cardify.app.ui.theme.CardifyTheme

class MainActivity : FragmentActivity() {

    private var lastPauseTimestamp: Long = 0L
    private val isAppLocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Shrinking scale + fade out exit animation on splash screen
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val iconView = splashScreenViewProvider.iconView
            val splashView = splashScreenViewProvider.view

            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.35f, 0f)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.35f, 0f)
            val iconAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f)

            val iconAnimator = ObjectAnimator.ofPropertyValuesHolder(iconView, scaleX, scaleY, iconAlpha).apply {
                interpolator = AnticipateInterpolator(1.6f)
                duration = 380L
            }

            val bgAlpha = ObjectAnimator.ofFloat(splashView, View.ALPHA, 1f, 0f).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 380L
            }

            AnimatorSet().apply {
                playTogether(iconAnimator, bgAlpha)
                doOnEnd {
                    splashScreenViewProvider.remove()
                }
                start()
            }
        }

        val app = application as CardifyApp

        // Lock app on cold start if biometric protection is enabled
        if (SecurityHelper.isBiometricEnabled.value) {
            isAppLocked.value = true
        }

        setContent {
            val themeMode by ThemeHelper.themeMode.collectAsState()
            val isDynamicColor by ThemeHelper.isDynamicColorEnabled.collectAsState()
            val isFlagSecure by SecurityHelper.isFlagSecureEnabled.collectAsState()
            val isBiometricEnabled by SecurityHelper.isBiometricEnabled.collectAsState()

            // Dynamic FLAG_SECURE window protection
            DisposableEffect(isFlagSecure) {
                if (isFlagSecure) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                onDispose { }
            }

            CardifyTheme(
                themeMode = themeMode,
                dynamicColor = isDynamicColor
            ) {
                val navController = rememberNavController()
                Box(modifier = Modifier.fillMaxSize()) {
                    CardifyNavHost(
                        navController = navController,
                        cardRepository = app.cardRepository,
                        categoryRepository = app.categoryRepository,
                        backupRepository = app.backupRepository
                    )

                    if (isBiometricEnabled) {
                        BiometricAuthLockScreen(
                            isLocked = isAppLocked.value,
                            onUnlockSuccess = { isAppLocked.value = false }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lastPauseTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        if (SecurityHelper.isBiometricEnabled.value && lastPauseTimestamp > 0L) {
            val elapsedSeconds = (System.currentTimeMillis() - lastPauseTimestamp) / 1000L
            val timeoutSeconds = SecurityHelper.lockTimeoutSeconds.value
            if (elapsedSeconds >= timeoutSeconds) {
                isAppLocked.value = true
            }
        }
    }
}
