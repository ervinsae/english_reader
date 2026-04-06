package com.ervinzhang.englishreader.feature.auth.domain

import com.ervinzhang.englishreader.core.model.User

enum class AuthError {
    INVALID_PHONE,
    INVALID_CODE,
    INVALID_INVITE_FORMAT,
    INVITE_NOT_FOUND,
    INVITE_ALREADY_USED,
    USER_ALREADY_EXISTS,
    EMPTY_NICKNAME,
    SESSION_NOT_FOUND,
}

enum class AppStartDestination {
    LOGIN,
    BOOKSHELF,
}

sealed interface AuthActionResult {
    data object Success : AuthActionResult
    data class Failure(val error: AuthError) : AuthActionResult
}

sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data class NeedsInvite(val phone: String) : LoginResult
    data class Failure(val error: AuthError) : LoginResult
}

sealed interface RegistrationResult {
    data class Success(val user: User) : RegistrationResult
    data class Failure(val error: AuthError) : RegistrationResult
}

sealed interface NicknameUpdateResult {
    data class Success(val user: User) : NicknameUpdateResult
    data class Failure(val error: AuthError) : NicknameUpdateResult
}
