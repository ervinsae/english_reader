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
    suspend fun refresh()
}

class OfflineBookRepository(
    private val contentSources: List<BookContentSource>,
) : BookRepository {
    private var cachedContent: Map<String, BookContent>? = null

    override suspend fun getBooks(): List<Book> {
        return loadContent()
            .values
            .map { it.book }
            .sortedBy { it.title }
    }

    override suspend fun getBookContent(bookId: String): BookContent? = loadContent()[bookId]

    override suspend fun refresh() {
        cachedContent = null
    }

    private suspend fun loadContent(): Map<String, BookContent> {
        cachedContent?.let { return it }

        val loadedContent = linkedMapOf<String, BookContent>()
        contentSources.forEach { source ->
            source.loadBookContents()
                .filter { it.book.enabled }
                .forEach { content ->
                    loadedContent.putIfAbsent(content.book.id, content)
                }
        }

        cachedContent = loadedContent
        return loadedContent
    }
}
