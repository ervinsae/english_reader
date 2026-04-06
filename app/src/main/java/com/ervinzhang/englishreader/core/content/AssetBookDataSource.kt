package com.ervinzhang.englishreader.core.content

import android.content.Context
import com.ervinzhang.englishreader.core.model.Book

class AssetBookDataSource(
    private val context: Context,
) {
    suspend fun loadBooks(): List<Book> {
        // TODO: 按 spec-002 解析 assets/books 下的 book.json / pages.json / words.json
        context.assets
        return emptyList()
    }
}
