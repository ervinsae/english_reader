package com.ervinzhang.englishreader.feature.dictionary.data

import com.ervinzhang.englishreader.core.database.dao.DictionaryCacheDao
import com.ervinzhang.englishreader.core.database.entity.DictionaryCacheEntity
import com.ervinzhang.englishreader.core.model.Word
import com.ervinzhang.englishreader.core.model.hasUsableMeaning
import com.ervinzhang.englishreader.core.model.needsLookupFallback
import com.ervinzhang.englishreader.core.model.normalizeWord

interface DictionaryRepository {
    suspend fun resolveWord(baseWord: Word): Word
}

data class DictionaryWordDetails(
    val meaningZh: String,
    val phonetic: String? = null,
    val audioUri: String? = null,
    val source: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

interface DictionaryRemoteDataSource {
    suspend fun lookup(word: String, normalizedWord: String): DictionaryWordDetails?
}

class CachedDictionaryRepository(
    private val cacheDao: DictionaryCacheDao,
    private val remoteDataSource: DictionaryRemoteDataSource,
) : DictionaryRepository {
    override suspend fun resolveWord(baseWord: Word): Word {
        if (!baseWord.needsLookupFallback()) return baseWord

        val normalizedWord = baseWord.text.normalizeWord()
        if (normalizedWord.isBlank()) return baseWord

        cacheDao.findByNormalizedWord(normalizedWord)
            ?.toDetails()
            ?.let { return baseWord.mergeLookupDetails(it) }

        val remoteDetails = remoteDataSource.lookup(
            word = baseWord.text.trim(),
            normalizedWord = normalizedWord,
        ) ?: return baseWord

        cacheDao.upsert(remoteDetails.toEntity(normalizedWord))
        return baseWord.mergeLookupDetails(remoteDetails)
    }
}

private fun Word.mergeLookupDetails(details: DictionaryWordDetails): Word {
    return copy(
        meaningZh = if (meaningZh.hasUsableMeaning()) meaningZh else details.meaningZh,
        phonetic = phonetic?.takeIf { it.isNotBlank() } ?: details.phonetic,
        audioUri = audioUri?.takeIf { it.isNotBlank() } ?: details.audioUri,
    )
}

private fun DictionaryCacheEntity.toDetails(): DictionaryWordDetails {
    return DictionaryWordDetails(
        meaningZh = meaningZh,
        phonetic = phonetic,
        audioUri = audioUri,
        source = source,
        updatedAt = updatedAt,
    )
}

private fun DictionaryWordDetails.toEntity(normalizedWord: String): DictionaryCacheEntity {
    return DictionaryCacheEntity(
        normalizedWord = normalizedWord,
        meaningZh = meaningZh,
        phonetic = phonetic,
        audioUri = audioUri,
        source = source,
        updatedAt = updatedAt,
    )
}
