package com.ervinzhang.englishreader.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary",
    indices = [Index(value = ["userId", "normalizedWord"], unique = true)],
)
data class VocabularyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val normalizedWord: String,
    val word: String,
    val meaningZh: String,
    val phonetic: String? = null,
    val audioAsset: String? = null,
    val createdAt: Long,
)
