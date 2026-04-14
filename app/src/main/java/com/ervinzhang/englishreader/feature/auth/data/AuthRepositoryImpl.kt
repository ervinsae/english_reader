package com.ervinzhang.englishreader.feature.auth.data

import com.ervinzhang.englishreader.core.datastore.SessionStore
import com.ervinzhang.englishreader.core.model.User
import com.ervinzhang.englishreader.feature.auth.domain.AppStartDestination
import com.ervinzhang.englishreader.feature.auth.domain.AuthActionResult
import com.ervinzhang.englishreader.feature.auth.domain.AuthError
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.auth.domain.LoginResult
import com.ervinzhang.englishreader.feature.auth.domain.NicknameUpdateResult
import com.ervinzhang.englishreader.feature.auth.domain.RegistrationResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionStore: SessionStore,
) : AuthRepository {
    override suspend fun sendCode(phone: String): AuthActionResult {
        return remoteDataSource.sendCode(phone.trim())
    }

    override suspend fun login(phone: String, code: String): LoginResult {
        return when (val result = remoteDataSource.login(phone.trim(), code.trim())) {
            is RemoteLoginResult.Failure -> LoginResult.Failure(result.error)
            is RemoteLoginResult.Success -> {
                sessionStore.saveSession(result.session)
                LoginResult.Success(result.user)
            }
            RemoteLoginResult.Unregistered -> LoginResult.NeedsInvite(phone.trim())
        }
    }

    override suspend fun register(phone: String, inviteCode: String): RegistrationResult {
        return when (
            val result = remoteDataSource.register(
                phone = phone.trim(),
                inviteCode = inviteCode.trim().uppercase(),
            )
        ) {
            is RemoteAuthResult.Failure -> RegistrationResult.Failure(result.error)
            is RemoteAuthResult.Success -> {
                sessionStore.saveSession(result.session)
                RegistrationResult.Success(result.user)
            }
        }
    }

    override suspend fun updateNickname(nickname: String): NicknameUpdateResult {
        val session = sessionStore.session.first()
            ?: return NicknameUpdateResult.Failure(
                error = AuthError.SESSION_NOT_FOUND,
            )

        return when (val result = remoteDataSource.updateNickname(session.userId, nickname)) {
            is RemoteUserResult.Failure -> NicknameUpdateResult.Failure(result.error)
            is RemoteUserResult.Success -> NicknameUpdateResult.Success(result.user)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val session = sessionStore.session.first() ?: return null
        if (session.expiredAt < System.currentTimeMillis()) {
            sessionStore.clearSession()
            return null
        }

        return remoteDataSource.getUser(session.userId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<User?> {
        return sessionStore.session.transformLatest { session ->
            when {
                session == null -> emit(null)
                session.expiredAt < System.currentTimeMillis() -> {
                    sessionStore.clearSession()
                    emit(null)
                }
                else -> emitAll(remoteDataSource.observeUser(session.userId))
            }
        }
    }

    override suspend fun getStartDestination(): AppStartDestination {
        val session = sessionStore.session.first() ?: return AppStartDestination.LOGIN
        if (session.expiredAt < System.currentTimeMillis()) {
            sessionStore.clearSession()
            return AppStartDestination.LOGIN
        }

        return if (remoteDataSource.getUser(session.userId) != null) {
            AppStartDestination.BOOKSHELF
        } else {
            sessionStore.clearSession()
            AppStartDestination.LOGIN
        }
    }

    override suspend fun logout() {
        sessionStore.clearSession()
    }
}
