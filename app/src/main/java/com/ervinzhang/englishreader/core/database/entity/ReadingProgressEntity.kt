package com.ervinzhang.englishreader.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "reading_progress",
    primaryKeys = ["userId", "bookId"],
)
data class ReadingProgressEntity(
    val userId: String,
    val bookId: String,
    val currentPage: Int,
    val finished: Boolean,
    val updatedAt: Long,
)
