package com.cardify.app.data.repository

import com.cardify.app.data.local.CardDao
import com.cardify.app.data.local.CategoryDao
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.domain.model.toDomain
import com.cardify.app.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CardRepository(
    private val cardDao: CardDao,
    private val categoryDao: CategoryDao
) {
    fun getAllCards(): Flow<List<LoyaltyCard>> {
        return combine(
            cardDao.getAllCardsFlow(),
            categoryDao.getAllCategoriesFlow()
        ) { cards, categories ->
            val catMap = categories.associateBy { it.id }
            cards.map { it.toDomain(catMap[it.categoryId]) }
        }
    }

    fun getCardsByCategory(categoryId: Long): Flow<List<LoyaltyCard>> {
        return combine(
            cardDao.getCardsByCategory(categoryId),
            categoryDao.getAllCategoriesFlow()
        ) { cards, categories ->
            val catMap = categories.associateBy { it.id }
            cards.map { it.toDomain(catMap[it.categoryId]) }
        }
    }

    fun searchCards(query: String): Flow<List<LoyaltyCard>> {
        return combine(
            cardDao.searchCards(query),
            categoryDao.getAllCategoriesFlow()
        ) { cards, categories ->
            val catMap = categories.associateBy { it.id }
            cards.map { it.toDomain(catMap[it.categoryId]) }
        }
    }

    fun getCardById(id: Long): Flow<LoyaltyCard?> {
        return combine(
            cardDao.getCardByIdFlow(id),
            categoryDao.getAllCategoriesFlow()
        ) { card, categories ->
            val catMap = categories.associateBy { it.id }
            card?.toDomain(catMap[card.categoryId])
        }
    }

    suspend fun saveCard(card: LoyaltyCard): Long {
        return cardDao.insertCard(card.toEntity())
    }

    suspend fun updateCard(card: LoyaltyCard) {
        cardDao.updateCard(card.toEntity())
    }

    suspend fun deleteCard(cardId: Long) {
        cardDao.deleteCardById(cardId)
    }

    suspend fun deleteCards(cardIds: List<Long>) {
        cardDao.deleteCardsByIds(cardIds)
    }

    suspend fun setFavorite(cardId: Long, isFavorite: Boolean) {
        cardDao.setFavorite(cardId, isFavorite)
    }

    suspend fun recordCardUsed(cardId: Long) {
        cardDao.recordCardUsed(cardId, System.currentTimeMillis())
    }

    suspend fun batchImportCards(cards: List<com.cardify.app.domain.util.SharedCardPayload>) {
        val allCategories = categoryDao.getAllCategories()
        cards.forEach { payload ->
            val matchedCat = if (!payload.categoryName.isNullOrBlank()) {
                allCategories.find { it.name.equals(payload.categoryName.trim(), ignoreCase = true) }
            } else null

            val card = LoyaltyCard(
                title = payload.title.ifBlank { "Карта" },
                barcodeValue = payload.barcodeValue,
                barcodeFormat = com.cardify.app.data.local.entities.BarcodeFormatEnum.fromString(payload.barcodeFormat),
                colorHex = payload.colorHex,
                notes = payload.notes ?: "",
                categoryId = matchedCat?.id,
                categoryName = matchedCat?.name
            )
            saveCard(card)
            com.cardify.app.barcode.BarcodeGenerator.preloadBarcode(card.barcodeValue, card.barcodeFormat)
        }
    }
}
