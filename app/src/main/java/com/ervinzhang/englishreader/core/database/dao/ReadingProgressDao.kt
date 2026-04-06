package com.ervinzhang.englishreader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ervinzhang.englishreader.core.database.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getByBook(userId: String, bookId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeAll(userId: String): Flow<List<ReadingProgressEntity>>
}
