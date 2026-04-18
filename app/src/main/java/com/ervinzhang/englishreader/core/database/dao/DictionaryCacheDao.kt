package com.ervinzhang.englishreader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ervinzhang.englishreader.core.database.entity.DictionaryCacheEntity

@Dao
interface DictionaryCacheDao {
    @Query("SELECT * FROM dictionary_cache WHERE normalizedWord = :normalizedWord LIMIT 1")
    suspend fun findByNormalizedWord(normalizedWord: String): DictionaryCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DictionaryCacheEntity)
}
