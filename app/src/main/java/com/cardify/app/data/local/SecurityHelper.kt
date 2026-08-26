package com.cardify.app.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SecurityHelper {
    private const val PREFS_NAME = "cardify_security_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_LOCK_TIMEOUT = "lock_timeout_seconds"
    private const val KEY_FLAG_SECURE_ENABLED = "flag_secure_enabled"
    private const val KEY_PRIVACY_MODE_ENABLED = "privacy_mode_enabled"

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _lockTimeoutSeconds = MutableStateFlow(0)
    val lockTimeoutSeconds: StateFlow<Int> = _lockTimeoutSeconds.asStateFlow()

    private val _isFlagSecureEnabled = MutableStateFlow(false)
    val isFlagSecureEnabled: StateFlow<Boolean> = _isFlagSecureEnabled.asStateFlow()

    private val _isPrivacyModeEnabled = MutableStateFlow(false)
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isBiometricEnabled.value = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        _lockTimeoutSeconds.value = prefs.getInt(KEY_LOCK_TIMEOUT, 0)
        _isFlagSecureEnabled.value = prefs.getBoolean(KEY_FLAG_SECURE_ENABLED, false)
        _isPrivacyModeEnabled.value = prefs.getBoolean(KEY_PRIVACY_MODE_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setLockTimeoutSeconds(context: Context, timeoutSeconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LOCK_TIMEOUT, timeoutSeconds).apply()
        _lockTimeoutSeconds.value = timeoutSeconds
    }

    fun setFlagSecureEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FLAG_SECURE_ENABLED, enabled).apply()
        _isFlagSecureEnabled.value = enabled
    }

    fun setPrivacyModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PRIVACY_MODE_ENABLED, enabled).apply()
        _isPrivacyModeEnabled.value = enabled
    }
}
