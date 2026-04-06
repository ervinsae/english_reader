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
import com.ervinzhang.englishreader.core.model.User
import com.ervinzhang.englishreader.core.navigation.Destinations
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.auth.domain.NicknameUpdateResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun NicknameScreen(
    authRepository: AuthRepository,
    entryPoint: String,
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
    onComplete: () -> Unit,
) {
    val viewModel: NicknameViewModel = viewModel(
        key = "nickname-$entryPoint",
        factory = SimpleViewModelFactory { NicknameViewModel(authRepository) },
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                NicknameEvent.Complete -> onComplete()
                NicknameEvent.SessionExpired -> onSessionExpired()
            }
        }
    }

    val uiState = viewModel.uiState
    val isProfileEntry = entryPoint == Destinations.Nickname.profileEntry

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isProfileEntry) "编辑昵称" else "补填昵称") },
                navigationIcon = {
                    if (isProfileEntry) {
                        TextButton(onClick = onBack) {
                            Text("返回")
                        }
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
            Text(
                text = if (isProfileEntry) {
                    "当前昵称可随时更新"
                } else {
                    "昵称可稍后补填，你也可以现在直接完成"
                },
            )
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = viewModel::onNicknameChanged,
                label = { Text("昵称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.errorMessage != null,
                supportingText = {
                    Text(uiState.errorMessage ?: "会展示在“我的”页面")
                },
            )
            Button(
                onClick = viewModel::saveNickname,
                enabled = !uiState.isSaving && uiState.nickname.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "保存中..." else "完成")
            }
            if (!isProfileEntry) {
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("稍后再说")
                }
            }
        }
    }
}

private data class NicknameUiState(
    val currentUser: User? = null,
    val nickname: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val initialized: Boolean = false,
)

private sealed interface NicknameEvent {
    data object Complete : NicknameEvent
    data object SessionExpired : NicknameEvent
}

private class NicknameViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    var uiState by mutableStateOf(NicknameUiState())
        private set

    private val eventChannel = Channel<NicknameEvent>(capacity = Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                if (user == null) {
                    eventChannel.send(NicknameEvent.SessionExpired)
                } else {
                    uiState = uiState.copy(
                        currentUser = user,
                        nickname = if (uiState.initialized) uiState.nickname else user.nickname.orEmpty(),
                        initialized = true,
                    )
                }
            }
        }
    }

    fun onNicknameChanged(value: String) {
        uiState = uiState.copy(nickname = value, errorMessage = null)
    }

    fun saveNickname() {
        if (uiState.isSaving) return

        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true, errorMessage = null)

            when (val result = authRepository.updateNickname(uiState.nickname)) {
                is NicknameUpdateResult.Failure -> {
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage = authErrorMessage(result.error),
                    )
                    if (result.error == com.ervinzhang.englishreader.feature.auth.domain.AuthError.SESSION_NOT_FOUND) {
                        eventChannel.send(NicknameEvent.SessionExpired)
                    }
                }
                is NicknameUpdateResult.Success -> {
                    uiState = uiState.copy(
                        currentUser = result.user,
                        nickname = result.user.nickname.orEmpty(),
                        isSaving = false,
                    )
                    eventChannel.send(NicknameEvent.Complete)
                }
            }
        }
    }
}
