package com.ervinzhang.englishreader.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ervinzhang.englishreader.core.database.dao.DictionaryCacheDao
import com.ervinzhang.englishreader.core.database.dao.InviteCodeDao
import com.ervinzhang.englishreader.core.database.dao.ReadingProgressDao
import com.ervinzhang.englishreader.core.database.dao.UserDao
import com.ervinzhang.englishreader.core.database.dao.VocabularyDao
import com.ervinzhang.englishreader.core.database.entity.DictionaryCacheEntity
import com.ervinzhang.englishreader.core.database.entity.InviteCodeEntity
import com.ervinzhang.englishreader.core.database.entity.ReadingProgressEntity
import com.ervinzhang.englishreader.core.database.entity.UserEntity
import com.ervinzhang.englishreader.core.database.entity.VocabularyEntity

@Database(
    entities = [
        DictionaryCacheEntity::class,
        ReadingProgressEntity::class,
        VocabularyEntity::class,
        UserEntity::class,
        InviteCodeEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dictionaryCacheDao(): DictionaryCacheDao
    abstract fun inviteCodeDao(): InviteCodeDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun userDao(): UserDao
    abstract fun vocabularyDao(): VocabularyDao
}
