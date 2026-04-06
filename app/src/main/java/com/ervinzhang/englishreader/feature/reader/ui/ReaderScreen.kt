package com.ervinzhang.englishreader.feature.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.core.audio.AudioPlayer
import com.ervinzhang.englishreader.core.content.BookContent
import com.ervinzhang.englishreader.core.content.BookRepository
import com.ervinzhang.englishreader.core.model.PageWordRef
import com.ervinzhang.englishreader.core.model.ReadingProgress
import com.ervinzhang.englishreader.core.model.Word
import com.ervinzhang.englishreader.core.reading.ReadingProgressRepository
import com.ervinzhang.englishreader.core.ui.AssetImage
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    bookRepository: BookRepository,
    readingProgressRepository: ReadingProgressRepository,
    authRepository: AuthRepository,
    audioPlayer: AudioPlayer,
    onBack: () -> Unit,
    onOpenVocabulary: () -> Unit,
) {
    val viewModel: ReaderViewModel = viewModel(
        key = "reader-$bookId",
        factory = SimpleViewModelFactory {
            ReaderViewModel(
                bookId = bookId,
                bookRepository = bookRepository,
                readingProgressRepository = readingProgressRepository,
                authRepository = authRepository,
                audioPlayer = audioPlayer,
            )
        },
    )
    val uiState = viewModel.uiState
    val bookContent = uiState.bookContent
    val currentPage = bookContent?.pages?.getOrNull(uiState.currentPageIndex)
    val currentWords = currentPage
        ?.words
        .orEmpty()
        .map { ReaderWordUiModel(ref = it, word = bookContent?.words?.get(it.wordId)) }
    val selectedWord = currentWords.firstOrNull { it.ref.wordId == uiState.selectedWordId }?.word

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bookContent?.book?.title ?: "阅读中：$bookId") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenVocabulary) {
                        Text("生词本")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                uiState.isLoading -> {
                    Text(text = "正在加载绘本内容...")
                }

                uiState.errorMessage != null -> {
                    Text(text = uiState.errorMessage)
                }

                currentPage == null || bookContent == null -> {
                    Text(text = "未找到当前绘本内容")
                }

                else -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AssetImage(
                                assetPath = currentPage.imageAsset,
                                contentDescription = "第 ${currentPage.pageNo} 页",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentScale = ContentScale.FillBounds,
                                fallbackText = "页面图片占位",
                            )
                            Text(
                                text = "第 ${currentPage.pageNo} / ${bookContent.pages.size} 页",
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = currentPage.englishText)
                            TextButton(
                                onClick = viewModel::playSentenceAudio,
                                enabled = currentPage.sentenceAudioAsset != null,
                            ) {
                                Text("播放整句")
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(text = "页内单词")
                            if (currentWords.isEmpty()) {
                                Text(text = "当前页没有标注单词")
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(currentWords) { wordUiModel ->
                                        TextButton(
                                            onClick = { viewModel.selectWord(wordUiModel.ref.wordId) },
                                        ) {
                                            Text(wordUiModel.ref.text)
                                        }
                                    }
                                }
                            }

                            if (selectedWord != null) {
                                SelectedWordCard(
                                    word = selectedWord,
                                    onPlay = viewModel::playSelectedWordAudio,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Button(
                            onClick = viewModel::goToPreviousPage,
                            enabled = uiState.currentPageIndex > 0,
                        ) {
                            Text("上一页")
                        }
                        Button(
                            onClick = viewModel::goToNextPage,
                            enabled = uiState.currentPageIndex < bookContent.pages.lastIndex,
                        ) {
                            Text("下一页")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedWordCard(
    word: Word,
    onPlay: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = word.text)
            if (!word.phonetic.isNullOrBlank()) {
                Text(text = word.phonetic)
            }
            Text(text = word.meaningZh)
            TextButton(onClick = onPlay, enabled = word.audioAsset != null) {
                Text("播放单词")
            }
        }
    }
}

private data class ReaderUiState(
    val isLoading: Boolean = true,
    val bookContent: BookContent? = null,
    val currentPageIndex: Int = 0,
    val selectedWordId: String? = null,
    val hasFinishedBook: Boolean = false,
    val errorMessage: String? = null,
)

private data class ReaderWordUiModel(
    val ref: PageWordRef,
    val word: Word?,
)

private class ReaderViewModel(
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val readingProgressRepository: ReadingProgressRepository,
    private val authRepository: AuthRepository,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {
    var uiState by mutableStateOf(ReaderUiState())
        private set
    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            uiState = runCatching {
                currentUserId = authRepository.getCurrentUser()?.id
                val bookContent = bookRepository.getBookContent(bookId)
                if (bookContent == null) {
                    ReaderUiState(
                        isLoading = false,
                        errorMessage = "未找到绘本：$bookId",
                    )
                } else {
                    val savedProgress = currentUserId?.let { userId ->
                        readingProgressRepository.getProgress(userId = userId, bookId = bookId)
                    }
                    val initialPageIndex = savedProgress
                        ?.currentPage
                        ?.let { currentPage -> bookContent.pages.indexOfFirst { it.pageNo == currentPage } }
                        ?.takeIf { it >= 0 }
                        ?: 0
                    val hasFinishedBook = savedProgress?.finished == true ||
                        initialPageIndex == bookContent.pages.lastIndex

                    if (currentUserId != null && hasFinishedBook && savedProgress?.finished != true) {
                        saveReadingProgress(
                            currentPage = bookContent.pages[initialPageIndex].pageNo,
                            finished = true,
                        )
                    }

                    ReaderUiState(
                        isLoading = false,
                        bookContent = bookContent,
                        currentPageIndex = initialPageIndex,
                        hasFinishedBook = hasFinishedBook,
                    )
                }
            }.getOrElse {
                ReaderUiState(
                    isLoading = false,
                    errorMessage = "绘本内容加载失败",
                )
            }
        }
    }

    fun goToPreviousPage() {
        updateCurrentPage(uiState.currentPageIndex - 1)
    }

    fun goToNextPage() {
        updateCurrentPage(uiState.currentPageIndex + 1)
    }

    fun selectWord(wordId: String) {
        uiState = uiState.copy(
            selectedWordId = if (uiState.selectedWordId == wordId) null else wordId,
        )
    }

    fun playSentenceAudio() {
        val sentenceAudioAsset = uiState.bookContent
            ?.pages
            ?.getOrNull(uiState.currentPageIndex)
            ?.sentenceAudioAsset
            ?: return
        audioPlayer.play(sentenceAudioAsset)
    }

    fun playSelectedWordAudio() {
        val selectedWordId = uiState.selectedWordId ?: return
        val wordAudioAsset = uiState.bookContent
            ?.words
            ?.get(selectedWordId)
            ?.audioAsset
            ?: return
        audioPlayer.play(wordAudioAsset)
    }

    private fun updateCurrentPage(targetPageIndex: Int) {
        val currentContent = uiState.bookContent ?: return
        if (currentContent.pages.isEmpty()) return

        val nextPageIndex = targetPageIndex.coerceIn(0, currentContent.pages.lastIndex)
        if (nextPageIndex == uiState.currentPageIndex) return

        val hasFinishedBook = uiState.hasFinishedBook || nextPageIndex == currentContent.pages.lastIndex
        val currentPageNo = currentContent.pages[nextPageIndex].pageNo

        uiState = uiState.copy(
            currentPageIndex = nextPageIndex,
            selectedWordId = null,
            hasFinishedBook = hasFinishedBook,
        )

        saveReadingProgress(
            currentPage = currentPageNo,
            finished = hasFinishedBook,
        )
    }

    private fun saveReadingProgress(
        currentPage: Int,
        finished: Boolean,
    ) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            readingProgressRepository.saveProgress(
                ReadingProgress(
                    userId = userId,
                    bookId = bookId,
                    currentPage = currentPage,
                    finished = finished,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override fun onCleared() {
        audioPlayer.stop()
        super.onCleared()
    }
}
