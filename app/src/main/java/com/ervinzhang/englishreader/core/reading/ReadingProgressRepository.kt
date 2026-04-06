package com.ervinzhang.englishreader.core.reading

import com.ervinzhang.englishreader.core.database.dao.ReadingProgressDao
import com.ervinzhang.englishreader.core.database.entity.ReadingProgressEntity
import com.ervinzhang.englishreader.core.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ReadingProgressRepository {
    suspend fun getProgress(userId: String, bookId: String): ReadingProgress?

    fun observeAll(userId: String): Flow<List<ReadingProgress>>

    suspend fun saveProgress(progress: ReadingProgress)
}

class RoomReadingProgressRepository(
    private val readingProgressDao: ReadingProgressDao,
) : ReadingProgressRepository {
    override suspend fun getProgress(userId: String, bookId: String): ReadingProgress? {
        return readingProgressDao.getByBook(userId = userId, bookId = bookId)?.toModel()
    }

    override fun observeAll(userId: String): Flow<List<ReadingProgress>> {
        return readingProgressDao.observeAll(userId).map { items -> items.map { it.toModel() } }
    }

    override suspend fun saveProgress(progress: ReadingProgress) {
        readingProgressDao.upsert(progress.toEntity())
    }
}

private fun ReadingProgressEntity.toModel(): ReadingProgress {
    return ReadingProgress(
        userId = userId,
        bookId = bookId,
        currentPage = currentPage,
        finished = finished,
        updatedAt = updatedAt,
    )
}

private fun ReadingProgress.toEntity(): ReadingProgressEntity {
    return ReadingProgressEntity(
        userId = userId,
        bookId = bookId,
        currentPage = currentPage,
        finished = finished,
        updatedAt = updatedAt,
    )
}
