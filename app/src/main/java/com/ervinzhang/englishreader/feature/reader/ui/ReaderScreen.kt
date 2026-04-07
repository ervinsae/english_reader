package com.ervinzhang.englishreader.feature.reader.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
                    Text(text = uiState.errorMessage.orEmpty())
                }

                currentPage == null || bookContent == null -> {
                    Text(text = "未找到当前绘本内容")
                }

                else -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ReaderPageTurnSurface(
                                canGoToPreviousPage = uiState.currentPageIndex > 0,
                                canGoToNextPage = uiState.currentPageIndex < bookContent.pages.lastIndex,
                                onPreviousPage = viewModel::goToPreviousPage,
                                onNextPage = viewModel::goToNextPage,
                            ) {
                                AssetImage(
                                    assetPath = currentPage.imageAsset,
                                    contentDescription = "第 ${currentPage.pageNo} 页",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    contentScale = ContentScale.FillBounds,
                                    fallbackText = "页面图片占位",
                                )
                            }
                            Text(
                                text = "轻扫图片或点按左右区域翻页",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "第 ${currentPage.pageNo} / ${bookContent.pages.size} 页",
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            IndexedEnglishText(
                                text = currentPage.englishText,
                                wordRefs = currentPage.words,
                                selectedWordId = uiState.selectedWordId,
                                onWordTap = viewModel::selectWord,
                            )
                            Text(
                                text = "点按英文中的标注单词查看释义",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
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
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.pointerInput(wordRefs, textLayoutResult) {
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
    canGoToPreviousPage: Boolean,
    canGoToNextPage: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
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
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
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
                TextButton(onClick = onPlay, enabled = word.audioAsset != null) {
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
            vocabularyMessage = null,
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
