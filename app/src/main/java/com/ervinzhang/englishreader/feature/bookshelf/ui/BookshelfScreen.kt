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
import com.ervinzhang.englishreader.core.ui.AssetImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    bookRepository: BookRepository,
    onOpenBook: (String) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val viewModel: BookshelfViewModel = viewModel(
        factory = SimpleViewModelFactory { BookshelfViewModel(bookRepository) },
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
                    items(uiState.books) { book ->
                        BookCard(book = book, onClick = { onOpenBook(book.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
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
        }
    }
}

private data class BookshelfUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val errorMessage: String? = null,
)

private class BookshelfViewModel(
    private val bookRepository: BookRepository,
) : ViewModel() {
    var uiState by mutableStateOf(BookshelfUiState())
        private set

    init {
        viewModelScope.launch {
            uiState = runCatching {
                BookshelfUiState(
                    isLoading = false,
                    books = bookRepository.getBooks(),
                )
            }.getOrElse {
                BookshelfUiState(
                    isLoading = false,
                    errorMessage = "内置绘本加载失败",
                )
            }
        }
    }
}
