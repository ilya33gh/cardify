package com.cardify.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val barcodeValue: String,
    val barcodeFormat: BarcodeFormatEnum,
    val categoryId: Long? = null,
    val colorHex: String = "#4D88FF",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val useCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)
