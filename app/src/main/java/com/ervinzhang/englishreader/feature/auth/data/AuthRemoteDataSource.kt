package com.ervinzhang.englishreader.feature.auth.data

import com.ervinzhang.englishreader.core.model.Session
import com.ervinzhang.englishreader.core.model.User
import com.ervinzhang.englishreader.feature.auth.domain.AuthActionResult
import com.ervinzhang.englishreader.feature.auth.domain.AuthError
import kotlinx.coroutines.flow.Flow

interface AuthRemoteDataSource {
    suspend fun sendCode(phone: String): AuthActionResult

    suspend fun login(phone: String, code: String): RemoteLoginResult

    suspend fun verifyInvite(code: String): InviteVerificationResult

    suspend fun register(phone: String, inviteCode: String): RemoteAuthResult

    suspend fun getUser(userId: String): User?

    fun observeUser(userId: String): Flow<User?>

    suspend fun updateNickname(userId: String, nickname: String): RemoteUserResult
}

sealed interface RemoteLoginResult {
    data class Success(
        val user: User,
        val session: Session,
    ) : RemoteLoginResult

    data object Unregistered : RemoteLoginResult

    data class Failure(val error: AuthError) : RemoteLoginResult
}

sealed interface InviteVerificationResult {
    data object Valid : InviteVerificationResult
    data class Failure(val error: AuthError) : InviteVerificationResult
}

sealed interface RemoteAuthResult {
    data class Success(
        val user: User,
        val session: Session,
    ) : RemoteAuthResult

    data class Failure(val error: AuthError) : RemoteAuthResult
}

sealed interface RemoteUserResult {
    data class Success(val user: User) : RemoteUserResult
    data class Failure(val error: AuthError) : RemoteUserResult
}
