package com.ervinzhang.englishreader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invite_codes")
data class InviteCodeEntity(
    @PrimaryKey val code: String,
    val createdAt: Long,
    val usedAt: Long? = null,
    val usedByUserId: String? = null,
)
