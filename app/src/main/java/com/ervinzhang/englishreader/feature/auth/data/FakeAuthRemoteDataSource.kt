package com.ervinzhang.englishreader.feature.auth.data

import androidx.room.withTransaction
import com.ervinzhang.englishreader.core.database.AppDatabase
import com.ervinzhang.englishreader.core.database.entity.UserEntity
import com.ervinzhang.englishreader.core.model.Session
import com.ervinzhang.englishreader.core.model.User
import com.ervinzhang.englishreader.feature.auth.domain.AuthActionResult
import com.ervinzhang.englishreader.feature.auth.domain.AuthError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeAuthRemoteDataSource(
    private val database: AppDatabase,
) : AuthRemoteDataSource {
    private val inviteCodeDao = database.inviteCodeDao()
    private val userDao = database.userDao()

    override suspend fun sendCode(phone: String): AuthActionResult {
        return if (isValidPhone(phone)) {
            AuthActionResult.Success
        } else {
            AuthActionResult.Failure(AuthError.INVALID_PHONE)
        }
    }

    override suspend fun login(phone: String, code: String): RemoteLoginResult {
        ensureSeedData()

        if (!isValidPhone(phone)) {
            return RemoteLoginResult.Failure(AuthError.INVALID_PHONE)
        }
        if (code != TEST_CODE) {
            return RemoteLoginResult.Failure(AuthError.INVALID_CODE)
        }

        val user = userDao.getByPhone(phone)?.toModel() ?: return RemoteLoginResult.Unregistered
        return RemoteLoginResult.Success(
            user = user,
            session = buildSession(user.id),
        )
    }

    override suspend fun verifyInvite(code: String): InviteVerificationResult {
        ensureSeedData()

        if (!isValidInviteCode(code)) {
            return InviteVerificationResult.Failure(AuthError.INVALID_INVITE_FORMAT)
        }

        val inviteCode = inviteCodeDao.getByCode(code)
            ?: return InviteVerificationResult.Failure(AuthError.INVITE_NOT_FOUND)

        return if (inviteCode.usedAt == null) {
            InviteVerificationResult.Valid
        } else {
            InviteVerificationResult.Failure(AuthError.INVITE_ALREADY_USED)
        }
    }

    override suspend fun register(phone: String, inviteCode: String): RemoteAuthResult {
        ensureSeedData()

        if (!isValidPhone(phone)) {
            return RemoteAuthResult.Failure(AuthError.INVALID_PHONE)
        }

        when (val inviteResult = verifyInvite(inviteCode)) {
            is InviteVerificationResult.Failure -> return RemoteAuthResult.Failure(inviteResult.error)
            InviteVerificationResult.Valid -> Unit
        }

        if (userDao.getByPhone(phone) != null) {
            return RemoteAuthResult.Failure(AuthError.USER_ALREADY_EXISTS)
        }

        val now = System.currentTimeMillis()
        val newUser = UserEntity(
            id = "user-$phone",
            phone = phone,
            nickname = null,
            createdAt = now,
            updatedAt = now,
        )

        val registrationSucceeded = database.withTransaction {
            val inserted = userDao.insertIgnore(newUser) != -1L
            val inviteMarked = inviteCodeDao.markUsed(
                code = inviteCode,
                usedAt = now,
                userId = newUser.id,
            ) == 1
            inserted && inviteMarked
        }

        if (!registrationSucceeded) {
            val latestInviteStatus = inviteCodeDao.getByCode(inviteCode)
            val error = when {
                latestInviteStatus == null -> AuthError.INVITE_NOT_FOUND
                latestInviteStatus.usedAt != null -> AuthError.INVITE_ALREADY_USED
                else -> AuthError.USER_ALREADY_EXISTS
            }
            return RemoteAuthResult.Failure(error)
        }

        return RemoteAuthResult.Success(
            user = newUser.toModel(),
            session = buildSession(newUser.id),
        )
    }

    override suspend fun getUser(userId: String): User? {
        ensureSeedData()
        return userDao.getById(userId)?.toModel()
    }

    override fun observeUser(userId: String): Flow<User?> {
        return userDao.observeById(userId).map { it?.toModel() }
    }

    override suspend fun updateNickname(userId: String, nickname: String): RemoteUserResult {
        ensureSeedData()

        val normalizedNickname = nickname.trim()
        if (normalizedNickname.isBlank()) {
            return RemoteUserResult.Failure(AuthError.EMPTY_NICKNAME)
        }

        val updatedAt = System.currentTimeMillis()
        val updated = userDao.updateNickname(
            userId = userId,
            nickname = normalizedNickname,
            updatedAt = updatedAt,
        ) == 1

        if (!updated) {
            return RemoteUserResult.Failure(AuthError.SESSION_NOT_FOUND)
        }

        val user = userDao.getById(userId)?.toModel()
            ?: return RemoteUserResult.Failure(AuthError.SESSION_NOT_FOUND)

        return RemoteUserResult.Success(user)
    }

    private suspend fun ensureSeedData() {
        inviteCodeDao.insertAll(AuthSeedData.inviteCodes)
        AuthSeedData.users.forEach { user ->
            userDao.insertIgnore(user)
        }
    }

    private fun buildSession(userId: String): Session {
        val now = System.currentTimeMillis()
        return Session(
            token = "local-session-$userId-$now",
            userId = userId,
            expiredAt = now + SESSION_DURATION_MS,
        )
    }

    private fun UserEntity.toModel(): User {
        return User(
            id = id,
            phone = phone,
            nickname = nickname,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun isValidPhone(phone: String): Boolean = PHONE_REGEX.matches(phone)

    private fun isValidInviteCode(code: String): Boolean = INVITE_CODE_REGEX.matches(code)

    private companion object {
        const val TEST_CODE = "123456"
        const val SESSION_DURATION_MS = 30L * 24L * 60L * 60L * 1000L
        val PHONE_REGEX = Regex("^1\\d{10}$")
        val INVITE_CODE_REGEX = Regex("^[A-Z0-9-]{6,20}$")
    }
}
