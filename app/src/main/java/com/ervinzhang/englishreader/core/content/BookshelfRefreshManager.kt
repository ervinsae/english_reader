package com.ervinzhang.englishreader.core.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BookshelfRefreshResult(
    val refreshedBooks: List<BookshelfBookPreview>,
    val hasChanges: Boolean,
)

class BookshelfRefreshManager(
    private val bookshelfRepository: BookshelfRepository,
) {
    suspend fun refresh(previousBooks: List<BookshelfBookPreview>): BookshelfRefreshResult =
        withContext(Dispatchers.IO) {
            bookshelfRepository.refresh()
            val refreshedBooks = bookshelfRepository.getBooks()
            BookshelfRefreshResult(
                refreshedBooks = refreshedBooks,
                hasChanges = previousBooks != refreshedBooks,
            )
        }
}
