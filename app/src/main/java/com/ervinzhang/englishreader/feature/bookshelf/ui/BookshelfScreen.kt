package com.ervinzhang.englishreader.feature.bookshelf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.core.content.BookPackageSyncManager
import com.ervinzhang.englishreader.core.content.BookshelfBookOpenStatus
import com.ervinzhang.englishreader.core.content.BookshelfBookPreview
import com.ervinzhang.englishreader.core.content.BookshelfRepository
import com.ervinzhang.englishreader.core.content.EnsureBookOpenableResult
import com.ervinzhang.englishreader.core.model.BookshelfItem
import com.ervinzhang.englishreader.core.model.ReadingProgress
import com.ervinzhang.englishreader.core.reading.ReadingProgressRepository
import com.ervinzhang.englishreader.core.ui.ContentImage
import com.ervinzhang.englishreader.core.ui.ProgressCaterpillar
import com.ervinzhang.englishreader.core.ui.StorybookBackdrop
import com.ervinzhang.englishreader.core.ui.StorybookCard
import com.ervinzhang.englishreader.core.ui.StorybookPrimaryButton
import com.ervinzhang.englishreader.core.ui.StorybookSectionTitle
import com.ervinzhang.englishreader.core.ui.StorybookTag
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    bookshelfRepository: BookshelfRepository,
    bookPackageSyncManager: BookPackageSyncManager,
    readingProgressRepository: ReadingProgressRepository,
    authRepository: AuthRepository,
    onOpenBook: (String) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val viewModel: BookshelfViewModel = viewModel(
        factory = SimpleViewModelFactory {
            BookshelfViewModel(
                bookshelfRepository = bookshelfRepository,
                bookPackageSyncManager = bookPackageSyncManager,
                readingProgressRepository = readingProgressRepository,
                authRepository = authRepository,
            )
        },
    )
    val uiState = viewModel.uiState
    val featuredBook = uiState.recentBooks.firstOrNull() ?: uiState.books.firstOrNull()
    val isBusyOpening = uiState.openingBookId != null

    LaunchedEffect(uiState.bookToOpen) {
        val bookId = uiState.bookToOpen ?: return@LaunchedEffect
        onOpenBook(bookId)
        viewModel.consumePendingOpen()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("故事书架")
                        Text(
                            text = uiState.userName?.let { "$it，继续阅读" } ?: "今天读哪一本？",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::syncContentPackages,
                        enabled = !uiState.isSyncing && !isBusyOpening,
                    ) {
                        Text(
                            when {
                                uiState.syncProgressPercent != null -> "同步中：${uiState.syncProgressPercent}%"
                                uiState.syncSuccess -> "同步成功"
                                uiState.syncUpToDate -> "已是最新"
                                else -> "同步内容"
                            },
                        )
                    }
                    TextButton(onClick = onOpenProfile, enabled = !isBusyOpening) {
                        Text("我的")
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
                    BookshelfMessageCard(message = "正在加载书架...")
                }

                uiState.errorMessage != null -> {
                    BookshelfMessageCard(message = uiState.errorMessage)
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                StorybookTag(
                                    text = "Home Screen",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = "把今天的阅读安排得像翻阅一张温暖的故事地图。",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        if (!uiState.openFailureMessage.isNullOrBlank()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                BookshelfInlineMessage(message = uiState.openFailureMessage)
                            }
                        }

                        if (featuredBook != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                FeaturedBookCard(
                                    book = featuredBook,
                                    isOpening = uiState.openingBookId == featuredBook.bookId,
                                    openActionText = buildOpenActionText(featuredBook, uiState),
                                    statusText = buildBookStatusText(featuredBook, uiState),
                                    enabled = uiState.openingBookId == null || uiState.openingBookId == featuredBook.bookId,
                                    onOpenBook = { viewModel.openBook(featuredBook.bookId) },
                                )
                            }
                        }

                        if (uiState.recentBooks.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                RecentReadingSection(
                                    recentBooks = uiState.recentBooks,
                                    onOpenBook = viewModel::openBook,
                                    openingBookId = uiState.openingBookId,
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            StorybookSectionTitle(title = "绘本书架")
                        }

                        itemsIndexed(uiState.books, key = { _, book -> book.bookId }) { index, book ->
                            BookCard(
                                book = book,
                                rotation = if (index % 2 == 0) -2f else 2f,
                                isOpening = uiState.openingBookId == book.bookId,
                                statusText = buildBookStatusText(book, uiState),
                                enabled = uiState.openingBookId == null || uiState.openingBookId == book.bookId,
                                onClick = { viewModel.openBook(book.bookId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookshelfMessageCard(message: String?) {
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
            Text(
                text = message.orEmpty(),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun BookshelfInlineMessage(message: String?) {
    StorybookCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
    ) {
        Text(
            text = message.orEmpty(),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun FeaturedBookCard(
    book: BookshelfItem,
    isOpening: Boolean,
    openActionText: String,
    statusText: String,
    enabled: Boolean,
    onOpenBook: () -> Unit,
) {
    StorybookCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onOpenBook)
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContentImage(
                contentUri = book.coverUri,
                contentDescription = "${book.title} 封面",
                modifier = Modifier
                    .weight(0.42f)
                    .aspectRatio(0.76f)
                    .rotate(-2f)
                    .clip(MaterialTheme.shapes.large),
                fallbackText = "封面占位",
            )
            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StorybookTag(
                    text = when {
                        isOpening -> "正在准备"
                        book.lastReadPage != null -> "继续阅读"
                        book.isLocalContentReady -> "推荐开始"
                        else -> "云端可读"
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "分级 ${book.level} · 共 ${book.pageCount} 页",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProgressCaterpillar(progress = book.readProgress)
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                StorybookPrimaryButton(
                    text = openActionText,
                    onClick = onOpenBook,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun BookCard(
    book: BookshelfItem,
    rotation: Float,
    isOpening: Boolean,
    statusText: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    StorybookCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ContentImage(
                contentUri = book.coverUri,
                contentDescription = "${book.title} 封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .rotate(rotation)
                    .clip(MaterialTheme.shapes.large),
                fallbackText = "封面占位",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StorybookTag(
                    text = book.level,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                if (!book.isLocalContentReady) {
                    StorybookTag(
                        text = if (isOpening) "下载中" else "云端",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ProgressCaterpillar(progress = book.readProgress)
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentReadingSection(
    recentBooks: List<BookshelfItem>,
    onOpenBook: (String) -> Unit,
    openingBookId: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StorybookSectionTitle(title = "最近阅读")
        recentBooks.forEach { book ->
            StorybookCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = openingBookId == null || openingBookId == book.bookId,
                    ) { onOpenBook(book.bookId) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${book.readProgress}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = buildBookStatusText(book = book, uiState = null),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ProgressCaterpillar(progress = book.readProgress)
                }
            }
        }
    }
}

private data class BookshelfUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val syncProgressPercent: Int? = null,
    val syncSuccess: Boolean = false,
    val syncUpToDate: Boolean = false,
    val userName: String? = null,
    val books: List<BookshelfItem> = emptyList(),
    val recentBooks: List<BookshelfItem> = emptyList(),
    val openingBookId: String? = null,
    val openingStageLabel: String? = null,
    val openingProgressPercent: Int? = null,
    val bookToOpen: String? = null,
    val openFailureMessage: String? = null,
    val errorMessage: String? = null,
)

private class BookshelfViewModel(
    private val bookshelfRepository: BookshelfRepository,
    private val bookPackageSyncManager: BookPackageSyncManager,
    private val readingProgressRepository: ReadingProgressRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    var uiState by mutableStateOf(BookshelfUiState())
        private set
    private var currentUserName: String? = null
    private var currentBooks: List<BookshelfBookPreview> = emptyList()
    private var latestProgressList: List<ReadingProgress> = emptyList()

    init {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            runCatching {
                currentUserName = currentUser?.nickname
                refreshBooks(forceRefresh = true)
                if (currentUser != null) {
                    readingProgressRepository.observeAll(currentUser.id).collect { progressList ->
                        latestProgressList = progressList
                        publishUiState()
                    }
                }
            }.onFailure {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "绘本内容加载失败",
                )
            }
        }
    }

    fun syncContentPackages() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isSyncing = true,
                syncProgressPercent = null,
                syncSuccess = false,
                syncUpToDate = false,
                openFailureMessage = null,
            )
            val syncResult = runCatching {
                bookPackageSyncManager.sync { progress ->
                    withContext(Dispatchers.Main) {
                        uiState = uiState.copy(
                            isSyncing = true,
                            syncProgressPercent = progress.coerceIn(0, 100),
                        )
                    }
                }
            }.getOrNull()

            if (syncResult != null) {
                refreshBooks(forceRefresh = true)
                if (syncResult.hasChanges) {
                    showSyncSuccess()
                } else {
                    showUpToDateState()
                }
            } else {
                clearSyncState()
            }
        }
    }

    fun openBook(bookId: String) {
        if (uiState.openingBookId != null && uiState.openingBookId != bookId) return

        val targetBook = currentBooks.firstOrNull { it.bookId == bookId }
        if (targetBook?.isLocalContentReady == true) {
            uiState = uiState.copy(
                bookToOpen = bookId,
                openFailureMessage = null,
            )
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(
                openingBookId = bookId,
                openingStageLabel = "准备中",
                openingProgressPercent = null,
                openFailureMessage = null,
            )
            val openResult = runCatching {
                bookshelfRepository.ensureBookOpenable(bookId) { status ->
                    withContext(Dispatchers.Main) {
                        applyOpenStatus(bookId = bookId, status = status)
                    }
                }
            }.getOrElse {
                EnsureBookOpenableResult.Failure("下载失败，请稍后再试")
            }

            when (openResult) {
                EnsureBookOpenableResult.Ready -> {
                    refreshBooks(forceRefresh = true)
                    uiState = uiState.copy(
                        openingBookId = null,
                        openingStageLabel = null,
                        openingProgressPercent = null,
                        bookToOpen = bookId,
                        openFailureMessage = null,
                    )
                }

                is EnsureBookOpenableResult.Failure -> {
                    showOpenFailure(openResult.message)
                }
            }
        }
    }

    fun consumePendingOpen() {
        uiState = uiState.copy(bookToOpen = null)
    }

    private suspend fun refreshBooks(forceRefresh: Boolean) {
        if (forceRefresh) {
            bookshelfRepository.refresh()
        }
        currentBooks = bookshelfRepository.getBooks()
        publishUiState()
    }

    private fun publishUiState() {
        uiState = uiState.copy(
            isLoading = false,
            userName = currentUserName,
            books = currentBooks.mapWithProgress(latestProgressList),
            recentBooks = currentBooks.mapRecent(latestProgressList),
            errorMessage = null,
        )
    }

    private fun applyOpenStatus(
        bookId: String,
        status: BookshelfBookOpenStatus,
    ) {
        if (uiState.openingBookId != null && uiState.openingBookId != bookId) return

        uiState = when (status) {
            BookshelfBookOpenStatus.CheckingLocal -> uiState.copy(
                openingBookId = bookId,
                openingStageLabel = "准备中",
                openingProgressPercent = null,
            )

            is BookshelfBookOpenStatus.Downloading -> uiState.copy(
                openingBookId = bookId,
                openingStageLabel = "下载中",
                openingProgressPercent = status.progressPercent,
            )

            BookshelfBookOpenStatus.Installing -> uiState.copy(
                openingBookId = bookId,
                openingStageLabel = "安装中",
                openingProgressPercent = null,
            )
        }
    }

    private suspend fun showSyncSuccess() {
        uiState = uiState.copy(
            isSyncing = false,
            syncProgressPercent = null,
            syncSuccess = true,
            syncUpToDate = false,
        )
        delay(1800)
        uiState = uiState.copy(syncSuccess = false)
    }

    private suspend fun showUpToDateState() {
        uiState = uiState.copy(
            isSyncing = false,
            syncProgressPercent = null,
            syncSuccess = false,
            syncUpToDate = true,
        )
        delay(1500)
        uiState = uiState.copy(syncUpToDate = false)
    }

    private suspend fun showOpenFailure(message: String) {
        uiState = uiState.copy(
            openingBookId = null,
            openingStageLabel = null,
            openingProgressPercent = null,
            openFailureMessage = message,
        )
        delay(2200)
        if (uiState.openFailureMessage == message) {
            uiState = uiState.copy(openFailureMessage = null)
        }
    }

    private fun clearSyncState() {
        uiState = uiState.copy(
            isSyncing = false,
            syncProgressPercent = null,
            syncSuccess = false,
            syncUpToDate = false,
        )
    }
}

private fun List<BookshelfBookPreview>.mapWithProgress(progressList: List<ReadingProgress>): List<BookshelfItem> {
    val progressByBookId = progressList.associateBy { it.bookId }
    return map { book -> book.toBookshelfItem(progressByBookId[book.bookId]) }
}

private fun List<BookshelfBookPreview>.mapRecent(progressList: List<ReadingProgress>): List<BookshelfItem> {
    val booksById = associateBy { it.bookId }
    return progressList
        .sortedByDescending { it.updatedAt }
        .mapNotNull { progress -> booksById[progress.bookId]?.toBookshelfItem(progress) }
        .take(3)
}

private fun BookshelfBookPreview.toBookshelfItem(progress: ReadingProgress?): BookshelfItem {
    val safePageCount = pageCount.coerceAtLeast(1)
    val lastReadPage = progress?.currentPage?.coerceIn(1, safePageCount)
    val readProgress = when {
        progress?.finished == true -> 100
        lastReadPage == null -> 0
        else -> (lastReadPage * 100) / safePageCount
    }

    return BookshelfItem(
        bookId = bookId,
        title = title,
        coverUri = coverUri,
        level = level,
        pageCount = pageCount,
        isLocalContentReady = isLocalContentReady,
        readProgress = readProgress,
        lastReadPage = lastReadPage,
        isFinished = progress?.finished == true,
    )
}

private fun buildBookStatusText(
    book: BookshelfItem,
    uiState: BookshelfUiState?,
): String {
    val isOpening = uiState?.openingBookId == book.bookId
    if (isOpening) {
        val progress = uiState.openingProgressPercent
        return if (progress != null) {
            "下载中 ${progress.coerceIn(0, 100)}%"
        } else {
            uiState.openingStageLabel ?: "准备中"
        }
    }

    return when {
        book.isFinished -> "已读完"
        book.lastReadPage != null -> "读到第 ${book.lastReadPage} / ${book.pageCount} 页"
        book.isLocalContentReady -> "未开始"
        else -> "云端预览，点开后下载"
    }
}

private fun buildOpenActionText(
    book: BookshelfItem,
    uiState: BookshelfUiState,
): String {
    if (uiState.openingBookId == book.bookId) {
        val progress = uiState.openingProgressPercent
        return if (progress != null) {
            "下载中 ${progress.coerceIn(0, 100)}%"
        } else {
            uiState.openingStageLabel ?: "准备中"
        }
    }

    return when {
        book.lastReadPage != null -> "继续"
        book.isLocalContentReady -> "开始阅读"
        else -> "下载后打开"
    }
}
