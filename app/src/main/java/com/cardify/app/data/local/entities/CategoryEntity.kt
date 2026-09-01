package com.cardify.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "purple",
    val orderIndex: Int = 0
)
