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
import androidx.compose.ui.text.input.KeyboardType
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
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.auth.domain.RegistrationResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("邀请码注册") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
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
                StorybookTag(text = "Invite Screen")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "把邀请码贴进这张入场卡",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "手机号 $phone 还没有注册。输入邀请口令后，沿用现有流程继续补填昵称。",
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
                        StorybookTag(
                            text = "Phone Ready",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "邀请码验证通过后，会直接进入昵称补填页，不新增任何路由。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        InvitationDots()
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
                            value = uiState.inviteCode,
                            onValueChange = viewModel::onInviteCodeChanged,
                            label = "邀请码",
                            supportingText = uiState.errorMessage
                                ?: "本地测试可使用 TREE-2026-AB12 或 TREE-2026-CD34",
                            isError = uiState.errorMessage != null,
                            keyboardType = KeyboardType.Ascii,
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "测试邀请码",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                StorybookTag(
                                    text = "TREE-2026-AB12",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                StorybookTag(
                                    text = "TREE-2026-CD34",
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        StorybookPrimaryButton(
                            text = if (uiState.isSubmitting) "注册中..." else "完成注册",
                            onClick = viewModel::submit,
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationDots() {
    Box(modifier = Modifier.fillMaxWidth()) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .padding(start = (index * 44).dp)
                    .size(if (index == 1) 54.dp else 38.dp)
                    .background(
                        color = when (index) {
                            0 -> MaterialTheme.colorScheme.secondaryContainer
                            1 -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        },
                        shape = CircleShape,
                    ),
            )
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
