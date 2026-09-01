package com.cardify.app.data.repository

import android.content.Context
import android.net.Uri
import com.cardify.app.data.local.CardDao
import com.cardify.app.data.local.CategoryDao
import com.cardify.app.data.local.entities.CardEntity
import com.cardify.app.data.local.entities.CategoryEntity
import com.cardify.app.domain.model.LoyaltyCard
import com.cardify.app.domain.model.toDomain
import com.cardify.app.domain.model.toEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class BackupPayload(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("exportedAt") val exportedAt: Long = System.currentTimeMillis(),
    @SerializedName("cards") val cards: List<CardEntity>,
    @SerializedName("categories") val categories: List<CategoryEntity>
)

class BackupRepository(
    private val context: Context,
    private val cardDao: CardDao,
    private val categoryDao: CategoryDao
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun getAllCards(): List<LoyaltyCard> = withContext(Dispatchers.IO) {
        val cards = cardDao.getAllCards()
        val categoriesMap = categoryDao.getAllCategories().associateBy { it.id }
        cards.map { it.toDomain(categoriesMap[it.categoryId]) }
    }

    suspend fun batchImportCards(cards: List<com.cardify.app.domain.util.SharedCardPayload>): Int = withContext(Dispatchers.IO) {
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
            cardDao.insertCard(card.toEntity())
            com.cardify.app.barcode.BarcodeGenerator.preloadBarcode(card.barcodeValue, card.barcodeFormat)
        }
        cards.size
    }

    suspend fun exportToJson(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val cards = cardDao.getAllCards()
            val categories = categoryDao.getAllCategories()
            val payload = BackupPayload(
                cards = cards,
                categories = categories
            )
            val json = gson.toJson(payload)

            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть файл для записи"))

            Result.success(cards.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportSelectedToJson(uri: Uri, selectedCardIds: List<Long>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allCards = cardDao.getAllCards()
            val selectedCards = allCards.filter { it.id in selectedCardIds }
            val usedCategoryIds = selectedCards.mapNotNull { it.categoryId }.toSet()
            val categories = categoryDao.getAllCategories().filter { it.id in usedCategoryIds }
            val payload = BackupPayload(
                cards = selectedCards,
                categories = categories
            )
            val json = gson.toJson(payload)

            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть файл для записи"))

            Result.success(selectedCards.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromJson(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { `is` ->
                BufferedReader(InputStreamReader(`is`)).use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Не удалось прочитать файл"))

            val payload = gson.fromJson(json, BackupPayload::class.java)
            if (payload?.cards == null) {
                return@withContext Result.failure(Exception("Неверный формат резервной копии"))
            }

            if (payload.categories.isNotEmpty()) {
                val normalizedCategories = payload.categories.map { cat ->
                    val mappedId = com.cardify.app.domain.model.CardColorPalette.findOption(cat.colorHex)?.id ?: cat.colorHex
                    cat.copy(colorHex = mappedId)
                }
                categoryDao.insertCategories(normalizedCategories)
            }

            val normalizedCards = payload.cards.map { card ->
                val mappedId = com.cardify.app.domain.model.CardColorPalette.findOption(card.colorHex)?.id ?: card.colorHex
                card.copy(colorHex = mappedId)
            }
            cardDao.insertCards(normalizedCards)

            Result.success(payload.cards.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromCatima(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val existingCategories = categoryDao.getAllCategories()
            val importResult = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                CatimaImporter.parse(inputStream, existingCategories)
            } ?: return@withContext Result.failure(Exception("Не удалось открыть файл для импорта"))

            if (importResult.cards.isEmpty()) {
                return@withContext Result.failure(Exception("В выбранном файле не найдено карт Catima"))
            }

            if (importResult.categories.isNotEmpty()) {
                categoryDao.insertCategories(importResult.categories)
            }
            cardDao.insertCards(importResult.cards)

            Result.success(importResult.cards.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
