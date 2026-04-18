package com.ervinzhang.englishreader.feature.vocabulary.data

import com.ervinzhang.englishreader.core.database.dao.VocabularyDao
import com.ervinzhang.englishreader.core.database.entity.VocabularyEntity
import com.ervinzhang.englishreader.core.model.VocabularyItem
import com.ervinzhang.englishreader.core.model.Word
import com.ervinzhang.englishreader.core.model.normalizeWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface VocabularyRepository {
    fun observeAll(userId: String): Flow<List<VocabularyItem>>

    suspend fun addWord(userId: String, word: Word): AddVocabularyResult

    suspend fun delete(item: VocabularyItem)
}

enum class AddVocabularyResult {
    Added,
    AlreadyExists,
}

class RoomVocabularyRepository(
    private val vocabularyDao: VocabularyDao,
) : VocabularyRepository {
    override fun observeAll(userId: String): Flow<List<VocabularyItem>> {
        return vocabularyDao.observeAll(userId).map { items -> items.map { it.toModel() } }
    }

    override suspend fun addWord(userId: String, word: Word): AddVocabularyResult {
        val normalizedWord = word.text.normalizeWord()
        val inserted = vocabularyDao.insert(
            VocabularyEntity(
                id = buildVocabularyId(userId = userId, normalizedWord = normalizedWord),
                userId = userId,
                normalizedWord = normalizedWord,
                word = word.text.trim(),
                meaningZh = word.meaningZh,
                phonetic = word.phonetic,
                audioUri = word.audioUri,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return if (inserted == -1L) AddVocabularyResult.AlreadyExists else AddVocabularyResult.Added
    }

    override suspend fun delete(item: VocabularyItem) {
        vocabularyDao.deleteByNormalizedWord(
            userId = item.userId,
            normalizedWord = item.normalizedWord,
        )
    }
}

private fun VocabularyEntity.toModel(): VocabularyItem {
    return VocabularyItem(
        userId = userId,
        normalizedWord = normalizedWord,
        word = word,
        meaningZh = meaningZh,
        phonetic = phonetic,
        audioUri = audioUri,
        createdAt = createdAt,
    )
}

private fun buildVocabularyId(
    userId: String,
    normalizedWord: String,
): String {
    return "$userId::$normalizedWord"
}
