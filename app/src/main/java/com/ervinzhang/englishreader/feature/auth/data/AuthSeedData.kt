package com.ervinzhang.englishreader.feature.auth.data

import com.ervinzhang.englishreader.core.database.entity.InviteCodeEntity
import com.ervinzhang.englishreader.core.database.entity.UserEntity

object AuthSeedData {
    private const val seedCreatedAt = 1_712_000_000_000L

    val users: List<UserEntity> = listOf(
        UserEntity(
            id = "seed-user-13800138000",
            phone = "13800138000",
            nickname = "Mia",
            createdAt = seedCreatedAt,
            updatedAt = seedCreatedAt,
        ),
    )

    val inviteCodes: List<InviteCodeEntity> = listOf(
        InviteCodeEntity(code = "TREE-2026-AB12", createdAt = seedCreatedAt),
        InviteCodeEntity(code = "TREE-2026-CD34", createdAt = seedCreatedAt),
        InviteCodeEntity(code = "TREE-2026-EF56", createdAt = seedCreatedAt),
    )
}
