package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ervinzhang.englishreader.core.model.User
import com.ervinzhang.englishreader.core.navigation.Destinations
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.auth.domain.NicknameUpdateResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isProfileEntry) "编辑昵称" else "补填昵称") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                StorybookTag(text = if (isProfileEntry) "Profile Edit" else "Last Step")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isProfileEntry) "给自己换一个更好记的名字" else "把名字写进这本故事书",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isProfileEntry) {
                            "昵称会显示在“我的”页面和书架问候语里，随时可以调整。"
                        } else {
                            "注册已经完成，补上昵称后就能直接进入书架；也可以先跳过。"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                StorybookCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            Text(
                                text = nicknameBadgeText(uiState.nickname, uiState.currentUser),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Text(
                            text = uiState.currentUser?.phone ?: "故事主角",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (uiState.nickname.isBlank()) {
                                "输入一个会在书架问候语里出现的昵称。"
                            } else {
                                "预览：欢迎回来，${uiState.nickname}。"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                        StorybookTextField(
                            value = uiState.nickname,
                            onValueChange = viewModel::onNicknameChanged,
                            label = "昵称",
                            supportingText = uiState.errorMessage ?: "会展示在“我的”页面与书架问候语",
                            isError = uiState.errorMessage != null,
                        )
                        StorybookPrimaryButton(
                            text = if (uiState.isSaving) "保存中..." else "完成",
                            onClick = viewModel::saveNickname,
                            enabled = !uiState.isSaving && uiState.nickname.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!isProfileEntry) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = MaterialTheme.shapes.large,
                            ) {
                                TextButton(
                                    onClick = onComplete,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "稍后再说",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun nicknameBadgeText(
    nickname: String,
    user: User?,
): String {
    val source = nickname.ifBlank {
        user?.nickname.orEmpty().ifBlank { user?.phone.orEmpty() }
    }
    return source.take(1).ifBlank { "A" }
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
