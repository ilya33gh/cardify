package com.cardify.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.local.LocaleHelper
import com.cardify.app.data.local.SecurityHelper
import com.cardify.app.data.local.ThemeHelper
import com.cardify.app.data.local.ThemeMode
import com.cardify.app.data.repository.BackupRepository
import com.cardify.app.data.repository.CategoryRepository
import com.cardify.app.domain.model.CardCategory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.cardify.app.ui.components.HapticPreference

data class SecurityTuple(
    val biometric: Boolean,
    val timeout: Int,
    val flagSecure: Boolean,
    val privacy: Boolean
)

data class SettingsUiState(
    val categories: List<CardCategory> = emptyList(),
    val currentLanguage: String = "system",
    val currentThemeMode: ThemeMode = ThemeMode.AUTO,
    val isDynamicColorEnabled: Boolean = true,
    val isHapticEnabled: Boolean = true,
    val isBiometricEnabled: Boolean = false,
    val lockTimeoutSeconds: Int = 0,
    val isFlagSecureEnabled: Boolean = false,
    val isPrivacyModeEnabled: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(
    private val categoryRepository: CategoryRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _isExporting = MutableStateFlow(false)
    private val _isImporting = MutableStateFlow(false)

    private val actionState = combine(_isExporting, _isImporting, _message) { exporting, importing, msg ->
        Triple(exporting, importing, msg)
    }

    private val securityState = combine(
        SecurityHelper.isBiometricEnabled,
        SecurityHelper.lockTimeoutSeconds,
        SecurityHelper.isFlagSecureEnabled,
        SecurityHelper.isPrivacyModeEnabled
    ) { biometric, timeout, flagSecure, privacy ->
        SecurityTuple(biometric, timeout, flagSecure, privacy)
    }

    private val themeTupleState = combine(
        ThemeHelper.themeMode,
        ThemeHelper.isDynamicColorEnabled,
        HapticPreference.isHapticEnabled
    ) { mode, dynamic, haptic ->
        Triple(mode, dynamic, haptic)
    }

    // Pre-warmed state flow combining categories, language, theme, security, and action state
    val uiState: StateFlow<SettingsUiState> = combine(
        categoryRepository.getAllCategories(),
        LocaleHelper.currentLanguage,
        themeTupleState,
        securityState,
        actionState
    ) { categories, language, (themeMode, isDynamicColor, isHaptic), sec, (exporting, importing, msg) ->
        SettingsUiState(
            categories = categories,
            currentLanguage = language,
            currentThemeMode = themeMode,
            isDynamicColorEnabled = isDynamicColor,
            isHapticEnabled = isHaptic,
            isBiometricEnabled = sec.biometric,
            lockTimeoutSeconds = sec.timeout,
            isFlagSecureEnabled = sec.flagSecure,
            isPrivacyModeEnabled = sec.privacy,
            isExporting = exporting,
            isImporting = importing,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState()
    )

    fun setLanguage(context: Context, languageCode: String) {
        LocaleHelper.setLanguage(context, languageCode)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        ThemeHelper.setThemeMode(context, mode)
    }

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        ThemeHelper.setDynamicColorEnabled(context, enabled)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        HapticPreference.setHapticEnabled(context, enabled)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        SecurityHelper.setBiometricEnabled(context, enabled)
    }

    fun setLockTimeoutSeconds(context: Context, timeoutSeconds: Int) {
        SecurityHelper.setLockTimeoutSeconds(context, timeoutSeconds)
    }

    fun setFlagSecureEnabled(context: Context, enabled: Boolean) {
        SecurityHelper.setFlagSecureEnabled(context, enabled)
    }

    fun setPrivacyModeEnabled(context: Context, enabled: Boolean) {
        SecurityHelper.setPrivacyModeEnabled(context, enabled)
    }

    fun addCategory(name: String, colorHex: String, iconName: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = uiState.value.categories.size
            categoryRepository.addCategory(
                CardCategory(
                    name = name.trim(),
                    colorHex = colorHex,
                    iconName = iconName,
                    orderIndex = count
                )
            )
            _message.value = "Категория «$name» добавлена"
        }
    }

    fun updateCategory(category: CardCategory) {
        if (category.name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
            _message.value = "Категория «${category.name}» обновлена"
        }
    }

    fun reorderCategories(reorderedCategories: List<CardCategory>) {
        viewModelScope.launch {
            val indexed = reorderedCategories.mapIndexed { idx, cat ->
                cat.copy(orderIndex = idx)
            }
            categoryRepository.updateCategories(indexed)
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            _message.value = "Категория удалена"
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            val result = backupRepository.exportToJson(uri)
            _isExporting.value = false
            _message.value = if (result.isSuccess) "Экспортировано карт: ${result.getOrNull()}" else "Ошибка экспорта: ${result.exceptionOrNull()?.message}"
        }
    }

    suspend fun generateAllCardsDeepLink(): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cards = backupRepository.getAllCards()
        if (cards.isEmpty()) null else com.cardify.app.domain.util.CardDeepLinkHelper.createBundleDeepLink(cards)
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = backupRepository.importFromJson(uri)
            _isImporting.value = false
            _message.value = if (result.isSuccess) "Импортировано карт: ${result.getOrNull()}" else "Ошибка импорта: ${result.exceptionOrNull()?.message}"
        }
    }

    fun importCatimaBackup(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = backupRepository.importFromCatima(uri)
            _isImporting.value = false
            _message.value = if (result.isSuccess) "Импортировано из Catima карт: ${result.getOrNull()}" else "Ошибка импорта Catima: ${result.exceptionOrNull()?.message}"
        }
    }

    fun batchImportCards(cards: List<com.cardify.app.domain.util.SharedCardPayload>) {
        if (cards.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            val count = backupRepository.batchImportCards(cards)
            _isImporting.value = false
            _message.value = "Импортировано карт: $count"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    class Factory(
        private val categoryRepository: CategoryRepository,
        private val backupRepository: BackupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(categoryRepository, backupRepository) as T
        }
    }
}
