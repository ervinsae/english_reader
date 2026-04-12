package com.ervinzhang.englishreader.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.core.model.User
import com.ervinzhang.englishreader.core.ui.StorybookBackdrop
import com.ervinzhang.englishreader.core.ui.StorybookCard
import com.ervinzhang.englishreader.core.ui.StorybookPrimaryButton
import com.ervinzhang.englishreader.core.ui.StorybookSectionTitle
import com.ervinzhang.englishreader.core.ui.StorybookTag
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onEditNickname: () -> Unit,
    onLogout: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = SimpleViewModelFactory { ProfileViewModel(authRepository) },
    )

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == ProfileEvent.NavigateToLogin) {
                onLogout()
            }
        }
    }

    val uiState = viewModel.uiState

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("我的") },
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
                StorybookTag(text = "My Story")
                Text(
                    text = "把账号信息整理成一张柔软的故事人物卡。",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                ProfileHeroCard(user = uiState.user)

                StorybookSectionTitle(title = "账户信息")
                StorybookCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        ProfileInfoRow(
                            label = "手机号",
                            value = uiState.user?.phone ?: "--",
                        )
                        ProfileInfoRow(
                            label = "昵称",
                            value = uiState.user?.nickname ?: "未设置",
                        )
                        ProfileInfoRow(
                            label = "加入时间",
                            value = uiState.user?.createdAt?.let(::formatProfileDate) ?: "--",
                        )
                    }
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
                        Text(
                            text = "账户操作",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        StorybookPrimaryButton(
                            text = if (uiState.user?.nickname.isNullOrBlank()) "补充昵称" else "编辑昵称",
                            onClick = onEditNickname,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            TextButton(
                                onClick = viewModel::logout,
                                enabled = !uiState.isLoggingOut,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = if (uiState.isLoggingOut) "退出中..." else "退出登录",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroCard(user: User?) {
    StorybookCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profileBadge(user),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = user?.nickname ?: "还没有昵称",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = user?.phone ?: "未登录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StorybookTag(
                    text = if (user?.nickname.isNullOrBlank()) "待补充昵称" else "昵称已设置",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                StorybookTag(
                    text = "账户正常",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun profileBadge(user: User?): String {
    val source = user?.nickname.orEmpty().ifBlank { user?.phone.orEmpty() }
    return source.take(1).ifBlank { "A" }
}

private fun formatProfileDate(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
    return formatter.format(
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate(),
    )
}

private data class ProfileUiState(
    val user: User? = null,
    val isLoggingOut: Boolean = false,
)

private enum class ProfileEvent {
    NavigateToLogin,
}

private class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    var uiState by mutableStateOf(ProfileUiState())
        private set

    private val eventChannel = Channel<ProfileEvent>(capacity = Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                uiState = uiState.copy(user = user)
            }
        }
    }

    fun logout() {
        if (uiState.isLoggingOut) return

        viewModelScope.launch {
            uiState = uiState.copy(isLoggingOut = true)
            authRepository.logout()
            uiState = uiState.copy(isLoggingOut = false)
            eventChannel.send(ProfileEvent.NavigateToLogin)
        }
    }
}
