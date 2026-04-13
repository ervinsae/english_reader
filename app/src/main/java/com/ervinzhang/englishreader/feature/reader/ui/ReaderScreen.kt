package com.ervinzhang.englishreader.feature.reader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.ervinzhang.englishreader.feature.vocabulary.data.AddVocabularyResult
import com.ervinzhang.englishreader.feature.vocabulary.data.VocabularyRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    bookRepository: BookRepository,
    readingProgressRepository: ReadingProgressRepository,
    vocabularyRepository: VocabularyRepository,
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
                vocabularyRepository = vocabularyRepository,
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
        bottomBar = {
            if (!uiState.isLoading && uiState.errorMessage == null && currentPage != null && bookContent != null) {
                ReaderPageNavigationBar(
                    currentPageNo = currentPage.pageNo,
                    totalPages = bookContent.pages.size,
                    canGoToPreviousPage = uiState.currentPageIndex > 0,
                    canGoToNextPage = uiState.currentPageIndex < bookContent.pages.lastIndex,
                    onPreviousPage = viewModel::goToPreviousPage,
                    onNextPage = viewModel::goToNextPage,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    Text(
                        text = "正在加载绘本内容...",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage.orEmpty(),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }

                currentPage == null || bookContent == null -> {
                    Text(
                        text = "未找到当前绘本内容",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }

                else -> {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            tonalElevation = 2.dp,
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            ReaderPageTurnSurface(
                                modifier = Modifier.fillMaxWidth(),
                                canGoToPreviousPage = uiState.currentPageIndex > 0,
                                canGoToNextPage = uiState.currentPageIndex < bookContent.pages.lastIndex,
                                onPreviousPage = viewModel::goToPreviousPage,
                                onNextPage = viewModel::goToNextPage,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AssetImage(
                                        assetPath = currentPage.imageAsset,
                                        contentDescription = "第 ${currentPage.pageNo} 页",
                                        modifier = Modifier.fillMaxWidth(),
                                        contentScale = ContentScale.Fit,
                                        matchAssetAspectRatio = true,
                                        filterQuality = FilterQuality.High,
                                        fallbackText = "页面图片占位",
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                IndexedEnglishText(
                                    text = currentPage.englishText,
                                    wordRefs = currentPage.words,
                                    selectedWordId = uiState.selectedWordId,
                                    onWordTap = viewModel::selectWord,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                SentencePlaybackControl(
                                    isPlaying = uiState.isSentencePlaybackActive,
                                    isPaused = uiState.isSentencePlaybackPaused,
                                    enabled = currentPage.sentenceAudioAsset != null || currentPage.englishText.isNotBlank(),
                                    onToggle = viewModel::toggleSentencePlayback,
                                )

                                if (currentWords.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(
                                            items = currentWords,
                                            key = { item -> item.ref.wordId },
                                        ) { wordUiModel ->
                                            val isSelected = wordUiModel.ref.wordId == uiState.selectedWordId
                                            OutlinedButton(
                                                onClick = { viewModel.selectWord(wordUiModel.ref.wordId) },
                                                shape = RoundedCornerShape(20.dp),
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.outlineVariant
                                                    },
                                                ),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isSelected) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    },
                                                    contentColor = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                ),
                                            ) {
                                                Text(wordUiModel.ref.text)
                                            }
                                        }
                                    }
                                }

                                if (!uiState.vocabularyMessage.isNullOrBlank()) {
                                    Text(
                                        text = uiState.vocabularyMessage,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedWord != null) {
        SelectedWordBottomSheet(
            word = selectedWord,
            vocabularyMessage = uiState.vocabularyMessage,
            onDismiss = viewModel::dismissSelectedWord,
            onPlay = viewModel::playSelectedWordAudio,
            onAddToVocabulary = viewModel::addSelectedWordToVocabulary,
        )
    }
}

@Composable
private fun IndexedEnglishText(
    text: String,
    wordRefs: List<PageWordRef>,
    selectedWordId: String?,
    onWordTap: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val selectedRange = wordRefs
        .firstOrNull { it.wordId == selectedWordId }
        ?.normalizedRange(text.length)
    val selectedBackgroundColor = MaterialTheme.colorScheme.primaryContainer
    val selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
    val annotatedText = remember(text, wordRefs, selectedRange, selectedBackgroundColor, selectedTextColor) {
        buildAnnotatedString {
            append(text)
            selectedRange?.let { range ->
                addStyle(
                    style = SpanStyle(
                        background = selectedBackgroundColor,
                        color = selectedTextColor,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    start = range.first,
                    end = range.last + 1,
                )
            }
        }
    }

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = textAlign,
        modifier = modifier.pointerInput(wordRefs, textLayoutResult) {
            detectTapGestures { tapOffset ->
                val layoutResult = textLayoutResult ?: return@detectTapGestures
                val characterOffset = layoutResult.getOffsetForPosition(tapOffset)
                wordRefs
                    .asSequence()
                    .mapNotNull { ref -> ref.normalizedRange(text.length)?.let { range -> ref to range } }
                    .filter { (_, range) -> characterOffset in range }
                    .minByOrNull { (_, range) -> range.last - range.first }
                    ?.first
                    ?.wordId
                    ?.let(onWordTap)
            }
        },
        onTextLayout = { textLayoutResult = it },
    )
}

private fun PageWordRef.normalizedRange(textLength: Int): IntRange? {
    if (textLength <= 0) return null

    val safeStart = startIndex.coerceIn(0, textLength)
    val safeEndExclusive = endIndex.coerceIn(0, textLength)
    if (safeEndExclusive <= safeStart) return null

    return safeStart until safeEndExclusive
}

@Composable
private fun ReaderPageTurnSurface(
    modifier: Modifier = Modifier,
    canGoToPreviousPage: Boolean,
    canGoToNextPage: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(canGoToPreviousPage, canGoToNextPage) {
                detectTapGestures { offset ->
                    val containerWidth = size.width.toFloat()
                    val zoneWidth = containerWidth / 3f
                    when {
                        offset.x <= zoneWidth && canGoToPreviousPage -> onPreviousPage()
                        offset.x >= containerWidth - zoneWidth && canGoToNextPage -> onNextPage()
                    }
                }
            }
            .pointerInput(canGoToPreviousPage, canGoToNextPage) {
                var totalDragX = 0f
                val swipeThreshold = viewConfiguration.touchSlop * 3

                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        totalDragX += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            totalDragX <= -swipeThreshold && canGoToNextPage -> onNextPage()
                            totalDragX >= swipeThreshold && canGoToPreviousPage -> onPreviousPage()
                        }
                        totalDragX = 0f
                    },
                    onDragCancel = {
                        totalDragX = 0f
                    },
                )
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun SentencePlaybackControl(
    isPlaying: Boolean,
    isPaused: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val buttonColor = if (isPlaying) Color(0xFF1FB28A) else MaterialTheme.colorScheme.primary
    val symbol = if (isPlaying) "⏸" else "▶"
    val label = when {
        isPlaying -> "暂停朗读"
        isPaused -> "继续朗读"
        else -> "播放朗读"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onToggle,
            enabled = enabled,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.size(88.dp),
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReaderPageNavigationBar(
    currentPageNo: Int,
    totalPages: Int,
    canGoToPreviousPage: Boolean,
    canGoToNextPage: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Surface(shadowElevation = 6.dp, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onPreviousPage,
                enabled = canGoToPreviousPage,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("‹ 上一页")
            }
            Text(
                text = "Page $currentPageNo · $totalPages",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onNextPage,
                enabled = canGoToNextPage,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("下一页 ›")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedWordBottomSheet(
    word: Word,
    vocabularyMessage: String?,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToVocabulary: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (!word.phonetic.isNullOrBlank()) {
                Text(
                    text = word.phonetic,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = word.meaningZh,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!vocabularyMessage.isNullOrBlank()) {
                Text(
                    text = vocabularyMessage,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onPlay,
                    enabled = word.audioAsset != null || word.text.isNotBlank(),
                ) {
                    Text("播放单词")
                }
                Button(onClick = onAddToVocabulary) {
                    Text("加入生词本")
                }
            }
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    }
}

private data class ReaderUiState(
    val isLoading: Boolean = true,
    val bookContent: BookContent? = null,
    val currentPageIndex: Int = 0,
    val isSentencePlaybackActive: Boolean = false,
    val isSentencePlaybackPaused: Boolean = false,
    val selectedWordId: String? = null,
    val hasFinishedBook: Boolean = false,
    val vocabularyMessage: String? = null,
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
    private val vocabularyRepository: VocabularyRepository,
    private val authRepository: AuthRepository,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {
    var uiState by mutableStateOf(ReaderUiState())
        private set
    private var currentUserId: String? = null
    private var shouldAutoPlaySentenceOnPageChange: Boolean = false

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
        shouldAutoPlaySentenceOnPageChange = false
        audioPlayer.stop()
        uiState = uiState.copy(
            selectedWordId = if (uiState.selectedWordId == wordId) null else wordId,
            isSentencePlaybackActive = false,
            isSentencePlaybackPaused = false,
            vocabularyMessage = null,
        )
    }

    fun dismissSelectedWord() {
        uiState = uiState.copy(selectedWordId = null)
    }

    fun addSelectedWordToVocabulary() {
        val userId = currentUserId ?: run {
            uiState = uiState.copy(vocabularyMessage = "请先登录后再加入生词本")
            return
        }
        val selectedWordId = uiState.selectedWordId ?: return
        val selectedWord = uiState.bookContent?.words?.get(selectedWordId) ?: return

        viewModelScope.launch {
            val result = vocabularyRepository.addWord(
                userId = userId,
                word = selectedWord,
            )
            uiState = uiState.copy(
                vocabularyMessage = when (result) {
                    AddVocabularyResult.Added -> "已加入生词本"
                    AddVocabularyResult.AlreadyExists -> "该单词已在生词本中"
                },
            )
        }
    }

    fun toggleSentencePlayback() {
        when {
            uiState.isSentencePlaybackActive -> pauseSentencePlayback()
            uiState.isSentencePlaybackPaused -> resumeSentencePlayback()
            else -> playSentenceAudio()
        }
    }

    fun playSentenceAudio() {
        shouldAutoPlaySentenceOnPageChange = true
        playCurrentPageSentenceAudio()
    }

    fun pauseSentencePlayback() {
        shouldAutoPlaySentenceOnPageChange = false
        val paused = audioPlayer.pause()
        uiState = uiState.copy(
            isSentencePlaybackActive = false,
            isSentencePlaybackPaused = paused,
        )
    }

    fun resumeSentencePlayback() {
        shouldAutoPlaySentenceOnPageChange = true
        val resumed = audioPlayer.resume()
        uiState = uiState.copy(
            isSentencePlaybackActive = resumed,
            isSentencePlaybackPaused = false,
        )
        if (!resumed) {
            playCurrentPageSentenceAudio()
        }
    }

    fun playSelectedWordAudio() {
        shouldAutoPlaySentenceOnPageChange = false
        uiState = uiState.copy(
            isSentencePlaybackActive = false,
            isSentencePlaybackPaused = false,
        )
        val selectedWordId = uiState.selectedWordId ?: return
        val selectedWord = uiState.bookContent
            ?.words
            ?.get(selectedWordId)
            ?: return

        val wordAudioAsset = selectedWord.audioAsset
        if (wordAudioAsset != null) {
            audioPlayer.play(wordAudioAsset)
            return
        }

        audioPlayer.speak(selectedWord.text)
    }

    private fun updateCurrentPage(targetPageIndex: Int) {
        val currentContent = uiState.bookContent ?: return
        if (currentContent.pages.isEmpty()) return

        val nextPageIndex = targetPageIndex.coerceIn(0, currentContent.pages.lastIndex)
        if (nextPageIndex == uiState.currentPageIndex) return

        val resumeSentencePlayback = shouldAutoPlaySentenceOnPageChange
        audioPlayer.stop()

        val hasFinishedBook = uiState.hasFinishedBook || nextPageIndex == currentContent.pages.lastIndex
        val currentPageNo = currentContent.pages[nextPageIndex].pageNo

        uiState = uiState.copy(
            currentPageIndex = nextPageIndex,
            isSentencePlaybackActive = resumeSentencePlayback,
            isSentencePlaybackPaused = false,
            selectedWordId = null,
            hasFinishedBook = hasFinishedBook,
            vocabularyMessage = null,
        )

        saveReadingProgress(
            currentPage = currentPageNo,
            finished = hasFinishedBook,
        )

        if (resumeSentencePlayback) {
            playCurrentPageSentenceAudio()
        }
    }

    private fun playCurrentPageSentenceAudio() {
        val currentPage = uiState.bookContent
            ?.pages
            ?.getOrNull(uiState.currentPageIndex)
            ?: return

        uiState = uiState.copy(
            isSentencePlaybackActive = true,
            isSentencePlaybackPaused = false,
        )

        val sentenceAudioAsset = currentPage.sentenceAudioAsset
        if (sentenceAudioAsset != null) {
            audioPlayer.play(sentenceAudioAsset)
            return
        }

        audioPlayer.speak(currentPage.englishText)
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
