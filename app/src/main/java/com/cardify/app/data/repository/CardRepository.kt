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

    suspend fun setFavorite(cardId: Long, isFavorite: Boolean) {
        cardDao.setFavorite(cardId, isFavorite)
    }

    suspend fun recordCardUsed(cardId: Long) {
        cardDao.recordCardUsed(cardId, System.currentTimeMillis())
    }
}
