package com.cardify.app.domain.model

import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.cardify.app.data.local.entities.CardEntity
import com.cardify.app.data.local.entities.CategoryEntity

data class LoyaltyCard(
    val id: Long = 0L,
    val title: String,
    val barcodeValue: String,
    val barcodeFormat: BarcodeFormatEnum,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val colorHex: String = "blue",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val useCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)

fun CardEntity.toDomain(category: CategoryEntity? = null): LoyaltyCard {
    return LoyaltyCard(
        id = id,
        title = title,
        barcodeValue = barcodeValue,
        barcodeFormat = barcodeFormat,
        categoryId = categoryId,
        categoryName = category?.name,
        categoryColor = category?.colorHex,
        colorHex = colorHex,
        notes = notes,
        isFavorite = isFavorite,
        useCount = useCount,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}

fun LoyaltyCard.toEntity(): CardEntity {
    return CardEntity(
        id = id,
        title = title,
        barcodeValue = barcodeValue,
        barcodeFormat = barcodeFormat,
        categoryId = categoryId,
        colorHex = colorHex,
        notes = notes,
        isFavorite = isFavorite,
        useCount = useCount,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}
