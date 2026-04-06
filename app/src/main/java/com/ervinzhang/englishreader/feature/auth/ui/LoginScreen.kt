package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.feature.auth.domain.AuthActionResult
import com.ervinzhang.englishreader.feature.auth.domain.AuthError
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.auth.domain.LoginResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLogin: () -> Unit,
    onNeedInvite: (String) -> Unit,
) {
    val viewModel: LoginViewModel = viewModel(
        factory = SimpleViewModelFactory { LoginViewModel(authRepository) },
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.NavigateToBookshelf -> onLogin()
                is LoginEvent.NavigateToInvite -> onNeedInvite(event.phone)
            }
        }
    }

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("手机号登录") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "第一版验证码固定为 123456")
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChanged,
                label = { Text("手机号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.phoneError != null,
                supportingText = {
                    Text(uiState.phoneError ?: "请输入 11 位手机号")
                },
            )
            OutlinedTextField(
                value = uiState.code,
                onValueChange = viewModel::onCodeChanged,
                label = { Text("测试验证码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.codeError != null,
                supportingText = {
                    Text(uiState.codeError ?: "点击获取验证码后，输入固定测试码")
                },
            )
            TextButton(
                onClick = viewModel::sendCode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("获取验证码")
            }
            if (uiState.message != null) {
                Text(text = uiState.message)
            }
            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSubmitting) "处理中..." else "登录 / 下一步")
            }
        }
    }
}

private data class LoginUiState(
    val phone: String = "",
    val code: String = "",
    val phoneError: String? = null,
    val codeError: String? = null,
    val message: String? = null,
    val isSubmitting: Boolean = false,
)

private sealed interface LoginEvent {
    data object NavigateToBookshelf : LoginEvent
    data class NavigateToInvite(val phone: String) : LoginEvent
}

private class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    var uiState by mutableStateOf(LoginUiState())
        private set

    private val eventChannel = Channel<LoginEvent>(capacity = Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onPhoneChanged(value: String) {
        uiState = uiState.copy(phone = value.filter(Char::isDigit), phoneError = null, message = null)
    }

    fun onCodeChanged(value: String) {
        uiState = uiState.copy(code = value.filter(Char::isDigit), codeError = null, message = null)
    }

    fun sendCode() {
        viewModelScope.launch {
            when (val result = authRepository.sendCode(uiState.phone)) {
                AuthActionResult.Success -> {
                    uiState = uiState.copy(
                        phoneError = null,
                        message = "验证码已发送。当前测试验证码固定为 123456",
                    )
                }
                is AuthActionResult.Failure -> {
                    uiState = uiState.copy(
                        phoneError = authErrorMessage(result.error),
                        message = null,
                    )
                }
            }
        }
    }

    fun submit() {
        if (uiState.isSubmitting) return

        viewModelScope.launch {
            uiState = uiState.copy(
                isSubmitting = true,
                phoneError = null,
                codeError = null,
                message = null,
            )

            when (val result = authRepository.login(uiState.phone, uiState.code)) {
                is LoginResult.Failure -> {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        phoneError = if (result.error == AuthError.INVALID_PHONE) {
                            authErrorMessage(result.error)
                        } else {
                            null
                        },
                        codeError = if (result.error == AuthError.INVALID_CODE) {
                            authErrorMessage(result.error)
                        } else {
                            null
                        },
                        message = if (
                            result.error != AuthError.INVALID_PHONE &&
                            result.error != AuthError.INVALID_CODE
                        ) {
                            authErrorMessage(result.error)
                        } else {
                            null
                        },
                    )
                }
                is LoginResult.NeedsInvite -> {
                    uiState = uiState.copy(isSubmitting = false)
                    eventChannel.send(LoginEvent.NavigateToInvite(result.phone))
                }
                is LoginResult.Success -> {
                    uiState = uiState.copy(isSubmitting = false)
                    eventChannel.send(LoginEvent.NavigateToBookshelf)
                }
            }
        }
    }
}

internal fun authErrorMessage(error: AuthError): String {
    return when (error) {
        AuthError.INVALID_PHONE -> "请输入正确的 11 位手机号"
        AuthError.INVALID_CODE -> "验证码错误，请输入固定测试码 123456"
        AuthError.INVALID_INVITE_FORMAT -> "邀请码格式错误"
        AuthError.INVITE_NOT_FOUND -> "邀请码不存在"
        AuthError.INVITE_ALREADY_USED -> "邀请码已被使用"
        AuthError.USER_ALREADY_EXISTS -> "该手机号已注册，请直接登录"
        AuthError.EMPTY_NICKNAME -> "请输入昵称"
        AuthError.SESSION_NOT_FOUND -> "登录状态已失效，请重新登录"
    }
}
