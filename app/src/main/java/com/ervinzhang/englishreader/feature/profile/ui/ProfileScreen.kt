package com.ervinzhang.englishreader.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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
        topBar = {
            TopAppBar(
                title = { Text("我的") },
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
            Text(text = "手机号：${uiState.user?.phone ?: "--"}")
            Text(text = "昵称：${uiState.user?.nickname ?: "未设置"}")
            TextButton(
                onClick = onEditNickname,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.user?.nickname.isNullOrBlank()) "补充昵称" else "编辑昵称")
            }
            Button(
                onClick = viewModel::logout,
                enabled = !uiState.isLoggingOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isLoggingOut) "退出中..." else "退出登录")
            }
        }
    }
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
