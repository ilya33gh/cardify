package com.cardify.app.data.repository

import android.content.Context
import android.net.Uri
import com.cardify.app.data.local.CardDao
import com.cardify.app.data.local.CategoryDao
import com.cardify.app.data.local.entities.CardEntity
import com.cardify.app.data.local.entities.CategoryEntity
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
                categoryDao.insertCategories(payload.categories)
            }
            cardDao.insertCards(payload.cards)

            Result.success(payload.cards.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
