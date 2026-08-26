package com.cardify.app

import android.app.Application
import com.cardify.app.data.local.CardDatabase
import com.cardify.app.data.local.LocaleHelper
import com.cardify.app.data.local.ThemeHelper
import com.cardify.app.data.repository.BackupRepository
import com.cardify.app.data.repository.CardRepository
import com.cardify.app.data.repository.CategoryRepository

import com.cardify.app.ui.components.HapticPreference

class CardifyApp : Application() {

    val database by lazy { CardDatabase.getInstance(this) }
    val cardRepository by lazy { CardRepository(database.cardDao(), database.categoryDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val backupRepository by lazy { BackupRepository(this, database.cardDao(), database.categoryDao()) }

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.init(this)
        ThemeHelper.init(this)
        HapticPreference.init(this)
    }
}
