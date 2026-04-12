package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.core.ui.StorybookBackdrop
import com.ervinzhang.englishreader.core.ui.StorybookCard
import com.ervinzhang.englishreader.core.ui.StorybookPrimaryButton
import com.ervinzhang.englishreader.core.ui.StorybookTag
import com.ervinzhang.englishreader.core.ui.StorybookTextField
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        StorybookBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                StorybookTag(text = "欢迎回来")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "打开今天的英语绘本冒险",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "沿用 Stitch 的温暖纸张质感，用手机号继续进入你的故事书架。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                StorybookCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "故事准备好了",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "输入手机号和验证码，马上继续阅读。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(3) { index ->
                                Spacer(
                                    modifier = Modifier
                                        .size(if (index == 1) 56.dp else 42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (index) {
                                                0 -> MaterialTheme.colorScheme.secondaryContainer
                                                1 -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.tertiaryContainer
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }

                StorybookCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        StorybookTag(
                            text = "测试验证码 123456",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        StorybookTextField(
                            value = uiState.phone,
                            onValueChange = viewModel::onPhoneChanged,
                            label = "手机号",
                            supportingText = uiState.phoneError ?: "请输入 11 位手机号",
                            isError = uiState.phoneError != null,
                            keyboardType = KeyboardType.Phone,
                        )
                        StorybookTextField(
                            value = uiState.code,
                            onValueChange = viewModel::onCodeChanged,
                            label = "测试验证码",
                            supportingText = uiState.codeError ?: "点击获取验证码后，输入固定测试码",
                            isError = uiState.codeError != null,
                            keyboardType = KeyboardType.Number,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            TextButton(
                                onClick = viewModel::sendCode,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "获取验证码",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        if (uiState.message != null) {
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        StorybookPrimaryButton(
                            text = if (uiState.isSubmitting) "处理中..." else "登录 / 下一步",
                            onClick = viewModel::submit,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "进入后仍沿用现有邀请与注册流程，不新增路由。",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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
