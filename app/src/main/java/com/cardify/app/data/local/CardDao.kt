package com.cardify.app.data.local

import androidx.room.*
import com.cardify.app.data.local.entities.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY isFavorite DESC, lastUsedAt DESC")
    fun getAllCardsFlow(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY isFavorite DESC, lastUsedAt DESC")
    suspend fun getAllCards(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    fun getCardByIdFlow(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE categoryId = :categoryId ORDER BY isFavorite DESC, lastUsedAt DESC")
    fun getCardsByCategory(categoryId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR barcodeValue LIKE '%' || :query || '%' ORDER BY isFavorite DESC, lastUsedAt DESC")
    fun searchCards(query: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("UPDATE cards SET useCount = useCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun recordCardUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE cards SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM cards")
    suspend fun clearAll()
}
