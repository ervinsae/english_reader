package com.ervinzhang.englishreader.core.content

interface BookContentSource {
    suspend fun loadBookContents(): List<BookContent>
}
