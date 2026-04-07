package com.ervinzhang.englishreader.feature.vocabulary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.core.audio.AudioPlayer
import com.ervinzhang.englishreader.core.model.VocabularyItem
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.vocabulary.data.VocabularyRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    vocabularyRepository: VocabularyRepository,
    authRepository: AuthRepository,
    audioPlayer: AudioPlayer,
    onBack: () -> Unit,
) {
    val viewModel: VocabularyViewModel = viewModel(
        factory = SimpleViewModelFactory {
            VocabularyViewModel(
                vocabularyRepository = vocabularyRepository,
                authRepository = authRepository,
                audioPlayer = audioPlayer,
            )
        },
    )
    val uiState = viewModel.uiState
    var pendingDelete by remember { mutableStateOf<VocabularyItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生词本") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                ) {
                    Text("正在加载生词本...")
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                ) {
                    Text(uiState.errorMessage.orEmpty())
                }
            }

            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                ) {
                    Text("还没有收藏的单词")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.items,
                        key = { item -> item.normalizedWord },
                    ) { item ->
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(text = item.word)
                                if (!item.phonetic.isNullOrBlank()) {
                                    Text(text = item.phonetic)
                                }
                                Text(text = item.meaningZh)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextButton(
                                        onClick = { viewModel.playWordAudio(item) },
                                        enabled = item.audioAsset != null,
                                    ) {
                                        Text("播放单词")
                                    }
                                    TextButton(onClick = { pendingDelete = item }) {
                                        Text("删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("确认删除") },
            text = { Text("要删除 ${pendingDelete?.word} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let(viewModel::deleteWord)
                        pendingDelete = null
                    },
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }
}

private data class VocabularyUiState(
    val isLoading: Boolean = true,
    val items: List<VocabularyItem> = emptyList(),
    val errorMessage: String? = null,
)

private class VocabularyViewModel(
    private val vocabularyRepository: VocabularyRepository,
    private val authRepository: AuthRepository,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {
    var uiState by mutableStateOf(VocabularyUiState())
        private set

    init {
        viewModelScope.launch {
            uiState = runCatching {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    VocabularyUiState(
                        isLoading = false,
                        errorMessage = "当前未登录，无法加载生词本",
                    )
                } else {
                    vocabularyRepository.observeAll(currentUser.id).collect { items ->
                        uiState = VocabularyUiState(
                            isLoading = false,
                            items = items,
                        )
                    }
                    uiState
                }
            }.getOrElse {
                VocabularyUiState(
                    isLoading = false,
                    errorMessage = "生词本加载失败",
                )
            }
        }
    }

    fun playWordAudio(item: VocabularyItem) {
        val audioAsset = item.audioAsset ?: return
        audioPlayer.play(audioAsset)
    }

    fun deleteWord(item: VocabularyItem) {
        viewModelScope.launch {
            vocabularyRepository.delete(item)
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}
