package com.ervinzhang.englishreader.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["phone"], unique = true)],
)
data class UserEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val nickname: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
