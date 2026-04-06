package com.ervinzhang.englishreader.core.content

import com.ervinzhang.englishreader.core.model.Book
import com.ervinzhang.englishreader.core.model.BookPage
import com.ervinzhang.englishreader.core.model.Word

data class BookContent(
    val book: Book,
    val pages: List<BookPage>,
    val words: Map<String, Word>,
)

interface BookRepository {
    suspend fun getBooks(): List<Book>
    suspend fun getBookContent(bookId: String): BookContent?
}

class OfflineBookRepository(
    private val assetBookDataSource: AssetBookDataSource,
) : BookRepository {
    private var cachedContent: Map<String, BookContent>? = null

    override suspend fun getBooks(): List<Book> {
        return loadContent()
            .values
            .map { it.book }
            .sortedBy { it.title }
    }

    override suspend fun getBookContent(bookId: String): BookContent? = loadContent()[bookId]

    private suspend fun loadContent(): Map<String, BookContent> {
        cachedContent?.let { return it }

        val loadedContent = assetBookDataSource
            .loadBookContents()
            .filter { it.book.enabled }
            .associateBy { it.book.id }

        cachedContent = loadedContent
        return loadedContent
    }
}
