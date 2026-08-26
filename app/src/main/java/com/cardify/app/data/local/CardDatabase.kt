package com.cardify.app.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cardify.app.data.local.entities.BarcodeFormatEnum
import com.cardify.app.data.local.entities.CardEntity
import com.cardify.app.data.local.entities.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromBarcodeFormat(value: BarcodeFormatEnum): String {
        return value.name
    }

    @TypeConverter
    fun toBarcodeFormat(value: String): BarcodeFormatEnum {
        return BarcodeFormatEnum.fromString(value)
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cards ADD COLUMN useCount INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [CardEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CardDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: CardDatabase? = null

        fun getInstance(context: Context): CardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CardDatabase::class.java,
                    "cardify_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }
        }

        suspend fun populateDefaultCategories(dao: CategoryDao) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Супермаркеты", iconName = "shopping_cart", colorHex = "#1E88E5", orderIndex = 0),
                CategoryEntity(name = "Одежда и обувь", iconName = "checkroom", colorHex = "#E91E63", orderIndex = 1),
                CategoryEntity(name = "Аптеки и здоровье", iconName = "local_pharmacy", colorHex = "#00897B", orderIndex = 2),
                CategoryEntity(name = "АЗС и авто", iconName = "local_gas_station", colorHex = "#FB8C00", orderIndex = 3),
                CategoryEntity(name = "Рестораны и кафе", iconName = "restaurant", colorHex = "#8E24AA", orderIndex = 4),
                CategoryEntity(name = "Электроника", iconName = "devices", colorHex = "#3949AB", orderIndex = 5),
                CategoryEntity(name = "Развлечения", iconName = "sports_esports", colorHex = "#D81B60", orderIndex = 6),
                CategoryEntity(name = "Другое", iconName = "folder", colorHex = "#546E7A", orderIndex = 7)
            )
            dao.insertCategories(defaultCategories)
        }
    }
}
