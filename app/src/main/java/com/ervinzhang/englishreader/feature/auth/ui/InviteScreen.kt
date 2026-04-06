package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.auth.domain.RegistrationResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun InviteScreen(
    phone: String,
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
) {
    val viewModel: InviteViewModel = viewModel(
        key = "invite-$phone",
        factory = SimpleViewModelFactory { InviteViewModel(phone, authRepository) },
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == InviteEvent.NavigateToNickname) {
                onRegisterSuccess()
            }
        }
    }

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("邀请码注册") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "手机号 $phone 尚未注册，请输入邀请码完成注册")
            OutlinedTextField(
                value = uiState.inviteCode,
                onValueChange = viewModel::onInviteCodeChanged,
                label = { Text("邀请码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.errorMessage != null,
                supportingText = {
                    Text(uiState.errorMessage ?: "本地测试可使用 TREE-2026-AB12 / TREE-2026-CD34")
                },
            )
            Button(
                onClick = viewModel::submit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSubmitting) "注册中..." else "完成注册")
            }
        }
    }
}

private data class InviteUiState(
    val inviteCode: String = "",
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
)

private enum class InviteEvent {
    NavigateToNickname,
}

private class InviteViewModel(
    private val phone: String,
    private val authRepository: AuthRepository,
) : ViewModel() {
    var uiState by mutableStateOf(InviteUiState())
        private set

    private val eventChannel = Channel<InviteEvent>(capacity = Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun onInviteCodeChanged(value: String) {
        uiState = uiState.copy(
            inviteCode = value.uppercase(),
            errorMessage = null,
        )
    }

    fun submit() {
        if (uiState.isSubmitting) return

        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, errorMessage = null)

            when (val result = authRepository.register(phone, uiState.inviteCode)) {
                is RegistrationResult.Failure -> {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = authErrorMessage(result.error),
                    )
                }
                is RegistrationResult.Success -> {
                    uiState = uiState.copy(isSubmitting = false)
                    eventChannel.send(InviteEvent.NavigateToNickname)
                }
            }
        }
    }
}
