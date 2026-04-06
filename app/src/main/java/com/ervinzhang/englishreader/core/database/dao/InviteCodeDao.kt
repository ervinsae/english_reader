package com.ervinzhang.englishreader.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ervinzhang.englishreader.core.database.entity.InviteCodeEntity

@Dao
interface InviteCodeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<InviteCodeEntity>)

    @Query("SELECT * FROM invite_codes WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): InviteCodeEntity?

    @Query(
        """
        UPDATE invite_codes
        SET usedAt = :usedAt, usedByUserId = :userId
        WHERE code = :code AND usedAt IS NULL
        """,
    )
    suspend fun markUsed(code: String, usedAt: Long, userId: String): Int
}
