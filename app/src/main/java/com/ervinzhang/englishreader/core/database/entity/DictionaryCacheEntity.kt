package com.ervinzhang.englishreader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary_cache")
data class DictionaryCacheEntity(
    @PrimaryKey val normalizedWord: String,
    val meaningZh: String,
    val phonetic: String? = null,
    val audioUri: String? = null,
    val source: String,
    val updatedAt: Long,
)
