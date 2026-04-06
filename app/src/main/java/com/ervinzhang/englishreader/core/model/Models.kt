package com.ervinzhang.englishreader.core.model

data class User(
    val id: String,
    val phone: String,
    val nickname: String? = null,
)

data class Book(
    val id: String,
    val title: String,
    val level: String,
    val coverAsset: String,
    val pageCount: Int,
)

data class BookPage(
    val bookId: String,
    val pageNo: Int,
    val imageAsset: String,
    val englishText: String,
    val sentenceAudioAsset: String? = null,
    val words: List<PageWordRef> = emptyList(),
)

data class PageWordRef(
    val wordId: String,
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
)

data class Word(
    val id: String,
    val text: String,
    val meaningZh: String,
    val phonetic: String? = null,
    val audioAsset: String? = null,
)

data class ReadingProgress(
    val userId: String,
    val bookId: String,
    val currentPage: Int,
    val finished: Boolean,
    val updatedAt: Long,
)

data class VocabularyItem(
    val userId: String,
    val normalizedWord: String,
    val word: String,
    val meaningZh: String,
    val phonetic: String? = null,
    val audioAsset: String? = null,
    val createdAt: Long,
)
