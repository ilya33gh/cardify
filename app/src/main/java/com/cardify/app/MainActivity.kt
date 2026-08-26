package com.cardify.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
