package com.cardify.app.ui.screens.wallet

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cardify.app.data.local.ThemeHelper
import com.cardify.app.data.repository.CardRepository
import com.cardify.app.data.repository.CategoryRepository
import com.cardify.app.domain.model.CardCategory
import com.cardify.app.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    ALPHABETICAL,
    DATE_ADDED,
    FREQUENCY
}

enum class LayoutMode {
    FULL_CARDS,
    LIST_ROWS,
    GRID_TWO_COLUMNS
}

data class WalletUiState(
    val cards: List<LoyaltyCard> = emptyList(),
    val allCards: List<LoyaltyCard> = emptyList(),
    val allCardsCount: Int = 0,
    val favoritesCount: Int = 0,
    val categories: List<CardCategory> = emptyList(),
    val selectedCategoryId: Long? = null,
    val onlyFavorites: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_ADDED,
    val layoutMode: LayoutMode = LayoutMode.FULL_CARDS,
    val selectedCardForDetail: LoyaltyCard? = null,
    val isLoading: Boolean = false
)

private data class FilterState(
    val selectedCategoryId: Long? = null,
    val onlyFavorites: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_ADDED,
    val layoutMode: LayoutMode = LayoutMode.FULL_CARDS
)

class WalletViewModel(
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val initialLayoutMode = try {
        LayoutMode.valueOf(ThemeHelper.layoutMode.value)
    } catch (e: Exception) {
        LayoutMode.FULL_CARDS
    }

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _onlyFavorites = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    private val _layoutMode = MutableStateFlow(initialLayoutMode)
    private val _selectedCardForDetail = MutableStateFlow<LoyaltyCard?>(null)

    private val filterState: Flow<FilterState> = combine(
        _selectedCategoryId,
        _onlyFavorites,
        _searchQuery,
        _sortOrder,
        _layoutMode
    ) { categoryId, onlyFav, query, sort, layout ->
        FilterState(categoryId, onlyFav, query, sort, layout)
    }

    val uiState: StateFlow<WalletUiState> = combine(
        cardRepository.getAllCards(),
        categoryRepository.getAllCategories(),
        filterState,
        _selectedCardForDetail
    ) { allCards, categories, filter, detailCard ->
        val favCount = allCards.count { it.isFavorite }

        // Cards matching search query (unrestricted by favorite tab for smooth transition scenes)
        val allSearchFiltered = allCards.filter { card ->
            filter.searchQuery.isBlank() ||
                    card.title.contains(filter.searchQuery, ignoreCase = true) ||
                    card.barcodeValue.contains(filter.searchQuery, ignoreCase = true) ||
                    card.notes.contains(filter.searchQuery, ignoreCase = true) ||
                    (card.categoryName?.contains(filter.searchQuery, ignoreCase = true) == true)
        }

        // Base cards matching favorites filter
        val baseFiltered = allSearchFiltered.filter { card ->
            !filter.onlyFavorites || card.isFavorite
        }

        // Category-filtered cards
        val categoryFiltered = baseFiltered.filter { card ->
            filter.selectedCategoryId == null || card.categoryId == filter.selectedCategoryId
        }

        // Apply Sorting
        val sortedCards = when (filter.sortOrder) {
            SortOrder.ALPHABETICAL -> categoryFiltered.sortedBy { it.title.lowercase() }
            SortOrder.DATE_ADDED -> categoryFiltered.sortedByDescending { it.createdAt }
            SortOrder.FREQUENCY -> categoryFiltered.sortedWith(
                compareByDescending<LoyaltyCard> { it.useCount }
                    .thenByDescending { it.lastUsedAt }
                    .thenByDescending { it.createdAt }
            )
        }

        val currentDetail = detailCard?.let { dc -> allCards.find { it.id == dc.id } }

        WalletUiState(
            cards = sortedCards,
            allCards = allSearchFiltered,
            allCardsCount = allCards.size,
            favoritesCount = favCount,
            categories = categories,
            selectedCategoryId = filter.selectedCategoryId,
            onlyFavorites = filter.onlyFavorites,
            searchQuery = filter.searchQuery,
            sortOrder = filter.sortOrder,
            layoutMode = filter.layoutMode,
            selectedCardForDetail = currentDetail
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WalletUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun onToggleOnlyFavorites(onlyFav: Boolean) {
        _onlyFavorites.value = onlyFav
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setLayoutMode(mode: LayoutMode, context: Context? = null) {
        _layoutMode.value = mode
        if (context != null) {
            ThemeHelper.setLayoutMode(context, mode.name)
        }
    }

    fun onCardClicked(card: LoyaltyCard) {
        _selectedCardForDetail.value = card
        viewModelScope.launch {
            cardRepository.recordCardUsed(card.id)
        }
    }

    fun onDismissCardDetail() {
        _selectedCardForDetail.value = null
    }

    fun onToggleFavorite(card: LoyaltyCard) {
        viewModelScope.launch {
            cardRepository.setFavorite(card.id, !card.isFavorite)
        }
    }

    fun onDeleteCard(cardId: Long) {
        viewModelScope.launch {
            cardRepository.deleteCard(cardId)
            _selectedCardForDetail.value = null
        }
    }

    class Factory(
        private val cardRepository: CardRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WalletViewModel(cardRepository, categoryRepository) as T
        }
    }
}
