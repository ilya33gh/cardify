package com.cardify.app.ui.screens.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.cardify.app.data.repository.CardRepository
import com.cardify.app.data.repository.CategoryRepository
import com.cardify.app.domain.model.CardCategory
import com.cardify.app.domain.model.CardColorPalette
import com.cardify.app.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditCardUiState(
    val cardId: Long = 0L,
    val title: String = "",
    val barcodeValue: String = "",
    val barcodeFormat: BarcodeFormatEnum = BarcodeFormatEnum.CODE_128,
    val selectedCategoryId: Long? = null,
    val selectedColorHex: String = CardColorPalette.options.first().primaryHex,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val categories: List<CardCategory> = emptyList(),
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class AddEditCardViewModel(
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository,
    private val initialCardId: Long?,
    initialBarcodeValue: String?,
    initialFormatName: String?
) : ViewModel() {

    private val _formState = MutableStateFlow(
        AddEditCardUiState(
            barcodeValue = initialBarcodeValue ?: "",
            barcodeFormat = initialFormatName?.let { BarcodeFormatEnum.fromString(it) } ?: BarcodeFormatEnum.CODE_128
        )
    )

    // Pre-warm category list eagerly with form state to prevent mid-transition recomposition
    val uiState: StateFlow<AddEditCardUiState> = combine(
        _formState,
        categoryRepository.getAllCategories()
    ) { form, cats ->
        form.copy(categories = cats)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = _formState.value
    )

    init {
        // Load existing card if editing
        if (initialCardId != null && initialCardId > 0) {
            viewModelScope.launch {
                _formState.update { it.copy(isLoading = true) }
                cardRepository.getCardById(initialCardId).firstOrNull()?.let { card ->
                    _formState.update {
                        it.copy(
                            cardId = card.id,
                            title = card.title,
                            barcodeValue = card.barcodeValue,
                            barcodeFormat = card.barcodeFormat,
                            selectedCategoryId = card.categoryId,
                            selectedColorHex = card.colorHex,
                            notes = card.notes,
                            isFavorite = card.isFavorite,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(value: String) = _formState.update { it.copy(title = value, errorMessage = null) }
    fun onBarcodeValueChanged(value: String) = _formState.update { it.copy(barcodeValue = value, errorMessage = null) }
    fun onBarcodeFormatChanged(format: BarcodeFormatEnum) = _formState.update { it.copy(barcodeFormat = format) }
    fun onCategorySelected(categoryId: Long?) = _formState.update { it.copy(selectedCategoryId = categoryId) }
    fun onColorSelected(hex: String) = _formState.update { it.copy(selectedColorHex = hex) }
    fun onNotesChanged(notes: String) = _formState.update { it.copy(notes = notes) }
    fun onFavoriteToggle() = _formState.update { it.copy(isFavorite = !it.isFavorite) }

    fun saveCard() {
        val currentState = uiState.value
        if (currentState.title.isBlank()) {
            _formState.update { it.copy(errorMessage = "Введите название карты или магазина") }
            return
        }
        if (currentState.barcodeValue.isBlank()) {
            _formState.update { it.copy(errorMessage = "Введите номер или код карты") }
            return
        }

        viewModelScope.launch {
            val card = LoyaltyCard(
                id = currentState.cardId,
                title = currentState.title.trim(),
                barcodeValue = currentState.barcodeValue.trim(),
                barcodeFormat = currentState.barcodeFormat,
                categoryId = currentState.selectedCategoryId,
                colorHex = currentState.selectedColorHex,
                notes = currentState.notes.trim(),
                isFavorite = currentState.isFavorite,
                lastUsedAt = System.currentTimeMillis()
            )

            if (currentState.cardId > 0) {
                cardRepository.updateCard(card)
            } else {
                cardRepository.saveCard(card)
            }

            _formState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(
        private val cardRepository: CardRepository,
        private val categoryRepository: CategoryRepository,
        private val initialCardId: Long?,
        private val initialBarcodeValue: String?,
        private val initialFormatName: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditCardViewModel(
                cardRepository,
                categoryRepository,
                initialCardId,
                initialBarcodeValue,
                initialFormatName
            ) as T
        }
    }
}
