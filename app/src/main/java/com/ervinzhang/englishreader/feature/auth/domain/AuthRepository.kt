package com.ervinzhang.englishreader.feature.auth.domain

import com.ervinzhang.englishreader.core.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun sendCode(phone: String): AuthActionResult

    suspend fun login(phone: String, code: String): LoginResult

    suspend fun register(phone: String, inviteCode: String): RegistrationResult

    suspend fun updateNickname(nickname: String): NicknameUpdateResult

    suspend fun getCurrentUser(): User?

    fun observeCurrentUser(): Flow<User?>

    suspend fun getStartDestination(): AppStartDestination

    suspend fun logout()
}
