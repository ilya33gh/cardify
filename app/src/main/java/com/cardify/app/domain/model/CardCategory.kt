package com.cardify.app.domain.model

import com.cardify.app.data.local.entities.CategoryEntity

data class CardCategory(
    val id: Long = 0L,
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "#6750A4",
    val orderIndex: Int = 0
)

fun CategoryEntity.toDomain(): CardCategory {
    return CardCategory(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        orderIndex = orderIndex
    )
}

fun CardCategory.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        orderIndex = orderIndex
    )
}
