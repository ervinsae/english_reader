package com.ervinzhang.englishreader.core.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AssetBookDataSource(
    private val context: Context,
) : BookContentSource {
    private val parser = BookPackageParser()

    override suspend fun loadBookContents(): List<BookContent> = withContext(Dispatchers.IO) {
        val bookDirectories = context.assets.list(BOOKS_ROOT).orEmpty().sorted()

        bookDirectories.mapNotNull { directoryName ->
            runCatching { loadBookContent(directoryName) }.getOrNull()
        }
    }

    private fun loadBookContent(directoryName: String): BookContent {
        val baseAssetPath = "$BOOKS_ROOT/$directoryName"
        return parser.parseBookContent(
            fallbackBookId = directoryName,
            bookJson = readJsonObject("$baseAssetPath/${BookPackageParser.BOOK_FILE_NAME}"),
            pagesJson = readJsonArray("$baseAssetPath/${BookPackageParser.PAGES_FILE_NAME}"),
            wordsJson = readJsonArray("$baseAssetPath/${BookPackageParser.WORDS_FILE_NAME}"),
        ) { rawPath ->
            resolveAssetPath(directoryName, rawPath)
        }
    }

    private fun resolveAssetPath(bookFolder: String, rawPath: String): String {
        val normalizedPath = rawPath.trim().removePrefix("/")
        val assetPath = if (normalizedPath.startsWith("$BOOKS_ROOT/")) {
            normalizedPath
        } else {
            "$BOOKS_ROOT/$bookFolder/$normalizedPath"
        }
        return ContentUri.asset(assetPath)
    }

    private fun readJsonObject(assetPath: String): JSONObject = JSONObject(readAsset(assetPath))

    private fun readJsonArray(assetPath: String): JSONArray = JSONArray(readAsset(assetPath))

    private fun readAsset(assetPath: String): String {
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val BOOKS_ROOT = "books"
    }
}
