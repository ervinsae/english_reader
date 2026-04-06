package com.ervinzhang.englishreader.feature.bookshelf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ervinzhang.englishreader.app.SimpleViewModelFactory
import com.ervinzhang.englishreader.core.content.BookRepository
import com.ervinzhang.englishreader.core.model.Book
import com.ervinzhang.englishreader.core.model.BookshelfItem
import com.ervinzhang.englishreader.core.model.ReadingProgress
import com.ervinzhang.englishreader.core.reading.ReadingProgressRepository
import com.ervinzhang.englishreader.core.ui.AssetImage
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    bookRepository: BookRepository,
    readingProgressRepository: ReadingProgressRepository,
    authRepository: AuthRepository,
    onOpenBook: (String) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val viewModel: BookshelfViewModel = viewModel(
        factory = SimpleViewModelFactory {
            BookshelfViewModel(
                bookRepository = bookRepository,
                readingProgressRepository = readingProgressRepository,
                authRepository = authRepository,
            )
        },
    )
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("书架") },
                actions = {
                    TextButton(onClick = onOpenProfile) {
                        Text("我的")
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
                    Text(text = "正在加载内置绘本...")
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                ) {
                    Text(text = uiState.errorMessage)
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.recentBooks.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RecentReadingSection(
                                recentBooks = uiState.recentBooks,
                                onOpenBook = onOpenBook,
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "全部绘本",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    items(uiState.books, key = { it.bookId }) { book ->
                        BookCard(book = book, onClick = { onOpenBook(book.bookId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: BookshelfItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AssetImage(
                assetPath = book.coverAsset,
                contentDescription = "${book.title} 封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f),
                fallbackText = "封面占位",
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(text = "分级：${book.level}")
            Text(text = "页数：${book.pageCount}")
            Text(
                text = buildProgressText(book),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun RecentReadingSection(
    recentBooks: List<BookshelfItem>,
    onOpenBook: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "最近阅读",
            style = MaterialTheme.typography.titleMedium,
        )
        recentBooks.forEach { book ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBook(book.bookId) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = buildProgressText(book),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private data class BookshelfUiState(
    val isLoading: Boolean = true,
    val books: List<BookshelfItem> = emptyList(),
    val recentBooks: List<BookshelfItem> = emptyList(),
    val errorMessage: String? = null,
)

private class BookshelfViewModel(
    private val bookRepository: BookRepository,
    private val readingProgressRepository: ReadingProgressRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    var uiState by mutableStateOf(BookshelfUiState())
        private set

    init {
        viewModelScope.launch {
            uiState = runCatching {
                val books = bookRepository.getBooks()
                val currentUser = authRepository.getCurrentUser()

                if (currentUser == null) {
                    BookshelfUiState(
                        isLoading = false,
                        books = books.map { it.toBookshelfItem(progress = null) },
                    )
                } else {
                    readingProgressRepository.observeAll(currentUser.id).collect { progressList ->
                        uiState = BookshelfUiState(
                            isLoading = false,
                            books = books.mapWithProgress(progressList),
                            recentBooks = books.mapRecent(progressList),
                        )
                    }
                    uiState
                }
            }.getOrElse {
                BookshelfUiState(
                    isLoading = false,
                    errorMessage = "内置绘本加载失败",
                )
            }
        }
    }
}

private fun List<Book>.mapWithProgress(progressList: List<ReadingProgress>): List<BookshelfItem> {
    val progressByBookId = progressList.associateBy { it.bookId }
    return map { book -> book.toBookshelfItem(progressByBookId[book.id]) }
}

private fun List<Book>.mapRecent(progressList: List<ReadingProgress>): List<BookshelfItem> {
    val booksById = associateBy { it.id }
    return progressList
        .mapNotNull { progress -> booksById[progress.bookId]?.toBookshelfItem(progress) }
        .take(3)
}

private fun Book.toBookshelfItem(progress: ReadingProgress?): BookshelfItem {
    val safePageCount = pageCount.coerceAtLeast(1)
    val lastReadPage = progress?.currentPage?.coerceIn(1, safePageCount)
    val readProgress = when {
        progress?.finished == true -> 100
        lastReadPage == null -> 0
        else -> (lastReadPage * 100) / safePageCount
    }

    return BookshelfItem(
        bookId = id,
        title = title,
        coverAsset = coverAsset,
        level = level,
        pageCount = pageCount,
        readProgress = readProgress,
        lastReadPage = lastReadPage,
        isFinished = progress?.finished == true,
    )
}

private fun buildProgressText(book: BookshelfItem): String {
    return when {
        book.isFinished -> "已读完"
        book.lastReadPage != null -> "进度：第 ${book.lastReadPage} / ${book.pageCount} 页 (${book.readProgress}%)"
        else -> "未开始"
    }
}
