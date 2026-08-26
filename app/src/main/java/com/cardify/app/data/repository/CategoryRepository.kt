package com.cardify.app.data.repository

import com.cardify.app.data.local.CategoryDao
import com.cardify.app.domain.model.CardCategory
import com.cardify.app.domain.model.toDomain
import com.cardify.app.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<CardCategory>> {
        return categoryDao.getAllCategoriesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun addCategory(category: CardCategory): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    suspend fun updateCategory(category: CardCategory) {
        categoryDao.updateCategory(category.toEntity())
    }

    suspend fun updateCategories(categories: List<CardCategory>) {
        categoryDao.updateCategories(categories.map { it.toEntity() })
    }

    suspend fun deleteCategory(categoryId: Long) {
        categoryDao.deleteCategoryById(categoryId)
    }
}
