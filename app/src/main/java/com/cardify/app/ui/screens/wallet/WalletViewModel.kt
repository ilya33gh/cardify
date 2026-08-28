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
    val isSortAscending: Boolean = false,
    val layoutMode: LayoutMode = LayoutMode.FULL_CARDS,
    val selectedCardForDetail: LoyaltyCard? = null,
    val isLoading: Boolean = false
)

private data class FilterState(
    val selectedCategoryId: Long? = null,
    val onlyFavorites: Boolean = false,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_ADDED,
    val isSortAscending: Boolean = false,
    val layoutMode: LayoutMode = LayoutMode.FULL_CARDS
)

fun compareCardsAlphabetical(a: LoyaltyCard, b: LoyaltyCard, isAscending: Boolean): Int {
    val s1 = a.title.trim()
    val s2 = b.title.trim()
    if (s1.isEmpty() && s2.isEmpty()) return 0
    if (s1.isEmpty()) return if (isAscending) 1 else -1
    if (s2.isEmpty()) return if (isAscending) -1 else 1

    val c1 = s1.first()
    val c2 = s2.first()

    val isLatin1 = (c1 in 'A'..'Z' || c1 in 'a'..'z')
    val isLatin2 = (c2 in 'A'..'Z' || c2 in 'a'..'z')
    val isCyrillic1 = (c1 in '\u0400'..'\u04FF')
    val isCyrillic2 = (c2 in '\u0400'..'\u04FF')

    // Latin vs Cyrillic grouping: Latin always stays grouped before Cyrillic in A-Z
    val scriptGroup = when {
        isLatin1 && isCyrillic2 -> -1
        isCyrillic1 && isLatin2 -> 1
        else -> 0
    }

    val collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("ru-RU")).apply {
        strength = java.text.Collator.PRIMARY
    }
    val titleCmp = collator.compare(s1, s2)

    val finalCmp = if (scriptGroup != 0) scriptGroup else titleCmp
    return if (isAscending) finalCmp else -finalCmp
}

fun sortCardsList(cards: List<LoyaltyCard>, sortOrder: SortOrder, isSortAscending: Boolean): List<LoyaltyCard> {
    return cards.sortedWith { a, b ->
        if (a.isFavorite != b.isFavorite) {
            return@sortedWith if (a.isFavorite) -1 else 1
        }
        when (sortOrder) {
            SortOrder.ALPHABETICAL -> compareCardsAlphabetical(a, b, isSortAscending)
            SortOrder.DATE_ADDED -> {
                val cmp = a.createdAt.compareTo(b.createdAt)
                if (isSortAscending) cmp else -cmp
            }
            SortOrder.FREQUENCY -> {
                val countCmp = a.useCount.compareTo(b.useCount)
                val lastUsedCmp = a.lastUsedAt.compareTo(b.lastUsedAt)
                val createdCmp = a.createdAt.compareTo(b.createdAt)
                val cmp = if (countCmp != 0) countCmp else if (lastUsedCmp != 0) lastUsedCmp else createdCmp
                if (isSortAscending) cmp else -cmp
            }
        }
    }
}

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
    private val _isSortAscending = MutableStateFlow(false)
    private val _layoutMode = MutableStateFlow(initialLayoutMode)
    private val _selectedCardForDetail = MutableStateFlow<LoyaltyCard?>(null)

    private val sortState = combine(_sortOrder, _isSortAscending) { order, asc ->
        order to asc
    }

    private val filterState: Flow<FilterState> = combine(
        _selectedCategoryId,
        _onlyFavorites,
        _searchQuery,
        sortState,
        _layoutMode
    ) { categoryId, onlyFav, query, (sort, ascending), layout ->
        FilterState(categoryId, onlyFav, query, sort, ascending, layout)
    }

    val uiState: StateFlow<WalletUiState> = combine(
        cardRepository.getAllCards(),
        categoryRepository.getAllCategories(),
        filterState,
        _selectedCardForDetail
    ) { allCards, categories, filter, detailCard ->
        allCards.forEach { card ->
            com.cardify.app.barcode.BarcodeGenerator.preloadBarcode(card.barcodeValue, card.barcodeFormat)
        }

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

        // Apply Sorting with Favorites Pinned to Top
        val sortedCards = sortCardsList(categoryFiltered, filter.sortOrder, filter.isSortAscending)

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
            isSortAscending = filter.isSortAscending,
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
        // Set natural default direction for each sort type
        if (order == SortOrder.ALPHABETICAL) {
            _isSortAscending.value = true // A-Z by default
        } else {
            _isSortAscending.value = false // Newest / Most used first by default
        }
    }

    fun toggleSortDirection() {
        _isSortAscending.value = !_isSortAscending.value
    }

    fun setSortAscending(ascending: Boolean) {
        _isSortAscending.value = ascending
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
