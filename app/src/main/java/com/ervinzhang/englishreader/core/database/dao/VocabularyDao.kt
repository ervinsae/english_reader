package com.ervinzhang.englishreader.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ervinzhang.englishreader.core.database.entity.VocabularyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: VocabularyEntity): Long

    @Delete
    suspend fun delete(item: VocabularyEntity)

    @Query("DELETE FROM vocabulary WHERE userId = :userId AND normalizedWord = :normalizedWord")
    suspend fun deleteByNormalizedWord(userId: String, normalizedWord: String): Int

    @Query("SELECT * FROM vocabulary WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM vocabulary WHERE userId = :userId AND normalizedWord = :normalizedWord LIMIT 1")
    suspend fun findByNormalizedWord(userId: String, normalizedWord: String): VocabularyEntity?
}
