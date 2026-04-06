package com.ervinzhang.englishreader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ervinzhang.englishreader.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeById(userId: String): Flow<UserEntity?>

    @Query("UPDATE users SET nickname = :nickname, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateNickname(userId: String, nickname: String, updatedAt: Long): Int
}
