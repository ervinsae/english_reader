package com.ervinzhang.englishreader.feature.vocabulary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.core.audio.AudioPlayer
import com.ervinzhang.englishreader.core.content.BookRepository
import com.ervinzhang.englishreader.core.model.VocabularyItem
import com.ervinzhang.englishreader.core.ui.StorybookBackdrop
import com.ervinzhang.englishreader.core.ui.StorybookCard
import com.ervinzhang.englishreader.core.ui.StorybookSectionTitle
import com.ervinzhang.englishreader.core.ui.StorybookTag
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.vocabulary.data.VocabularyRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    vocabularyRepository: VocabularyRepository,
    bookRepository: BookRepository,
    authRepository: AuthRepository,
    audioPlayer: AudioPlayer,
    onBack: () -> Unit,
) {
    val viewModel: VocabularyViewModel = viewModel(
        factory = SimpleViewModelFactory {
            VocabularyViewModel(
                vocabularyRepository = vocabularyRepository,
                bookRepository = bookRepository,
                authRepository = authRepository,
                audioPlayer = audioPlayer,
            )
        },
    )
    val uiState = viewModel.uiState
    var pendingDelete by remember { mutableStateOf<VocabularyItem?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("生词本") },
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
            when {
                uiState.isLoading -> {
                    VocabularyMessageCard(
                        title = "正在整理你的单词卡",
                        message = "稍等一下，温暖的故事纸页正在把收藏词汇摊开。",
                    )
                }

                uiState.errorMessage != null -> {
                    VocabularyMessageCard(
                        title = "生词本暂时打不开",
                        message = uiState.errorMessage.orEmpty(),
                    )
                }

                uiState.items.isEmpty() -> {
                    VocabularyMessageCard(
                        title = "还没有收藏的单词",
                        message = "在阅读页点按标注单词并加入生词本，这里就会长出第一张单词卡。",
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                StorybookTag(text = "Word Garden")
                                Text(
                                    text = "把故事里遇到的新词收进这片温暖的纸上花园。",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        item {
                            VocabularySummaryCard(items = uiState.items)
                        }

                        item {
                            StorybookSectionTitle(title = "收藏单词")
                        }

                        items(
                            items = uiState.items,
                            key = { item -> item.normalizedWord },
                        ) { item ->
                            VocabularyItemCard(
                                item = item,
                                onPlayWord = { viewModel.playWordAudio(item) },
                                onDeleteWord = { pendingDelete = item },
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("确认删除") },
            text = { Text("要删除 ${pendingDelete?.word} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete?.let(viewModel::deleteWord)
                        pendingDelete = null
                    },
                ) {
                    Text(
                        text = "确认",
                        color = MaterialTheme.colorScheme.error,
                    )
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

@Composable
private fun VocabularyMessageCard(
    title: String,
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        StorybookCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VocabularySummaryCard(
    items: List<VocabularyItem>,
) {
    val latestItem = items.maxByOrNull(VocabularyItem::createdAt)
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
                text = "${items.size} 个收藏",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = latestItem?.let { "最近收进来的词是 ${it.word}" } ?: "继续从阅读页收集新词",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = latestItem?.let {
                    "加入时间 ${formatStorybookDate(it.createdAt)}，点进每张卡片可以播放读音或删除。"
                } ?: "点按阅读页的单词，就能把它加入这本生词册。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VocabularyItemCard(
    item: VocabularyItem,
    onPlayWord: () -> Unit,
    onDeleteWord: () -> Unit,
) {
    StorybookCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.audioAsset != null || item.word.isNotBlank(), onClick = onPlayWord),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!item.phonetic.isNullOrBlank()) {
                        Text(
                            text = item.phonetic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                StorybookTag(
                    text = formatStorybookDate(item.createdAt),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = item.meaningZh,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    TextButton(
                        onClick = onPlayWord,
                        enabled = item.audioAsset != null || item.word.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "播放单词",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                ) {
                    TextButton(
                        onClick = onDeleteWord,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "删除",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

private fun formatStorybookDate(timestamp: Long): String {
    val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
    return formatter.format(
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate(),
    )
}

private data class VocabularyUiState(
    val isLoading: Boolean = true,
    val items: List<VocabularyItem> = emptyList(),
    val errorMessage: String? = null,
)

private class VocabularyViewModel(
    private val vocabularyRepository: VocabularyRepository,
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {
    var uiState by mutableStateOf(VocabularyUiState())
        private set
    private var fallbackAudioAssetsByWord: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            fallbackAudioAssetsByWord = loadFallbackAudioAssets()
        }

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
        val audioAsset = item.audioAsset ?: fallbackAudioAssetsByWord[item.normalizedWord]
        if (audioAsset != null) {
            audioPlayer.play(audioAsset)
            return
        }

        audioPlayer.speak(item.word)
    }

    fun deleteWord(item: VocabularyItem) {
        viewModelScope.launch {
            vocabularyRepository.delete(item)
        }
    }

    private suspend fun loadFallbackAudioAssets(): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for (book in bookRepository.getBooks()) {
            val content = bookRepository.getBookContent(book.id) ?: continue
            for (word in content.words.values) {
                val audioAsset = word.audioAsset ?: continue
                result.putIfAbsent(normalizeWord(word.text), audioAsset)
            }
        }
        return result
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}

private fun normalizeWord(raw: String): String {
    val trimmed = raw.trim()
    val normalized = trimmed
        .lowercase(Locale.ROOT)
        .replace(Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$"), "")
    return normalized.ifBlank { trimmed.lowercase(Locale.ROOT) }
}
