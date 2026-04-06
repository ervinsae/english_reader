package com.ervinzhang.englishreader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ervinzhang.englishreader.core.database.dao.ReadingProgressDao
import com.ervinzhang.englishreader.core.database.dao.VocabularyDao
import com.ervinzhang.englishreader.core.database.entity.ReadingProgressEntity
import com.ervinzhang.englishreader.core.database.entity.VocabularyEntity

@Database(
    entities = [ReadingProgressEntity::class, VocabularyEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun vocabularyDao(): VocabularyDao
}
