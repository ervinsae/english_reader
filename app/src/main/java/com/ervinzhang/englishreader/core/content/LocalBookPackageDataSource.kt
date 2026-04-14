package com.ervinzhang.englishreader.core.content

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class LocalBookPackageDataSource(
    private val packageStorage: LocalBookPackageStorage,
) : BookContentSource {
    private val parser = BookPackageParser()

    override suspend fun loadBookContents(): List<BookContent> = withContext(Dispatchers.IO) {
        packageStorage.listInstalledPackageDirectories()
            .sortedBy { it.parentFile?.name.orEmpty() }
            .mapNotNull { directory ->
                runCatching { loadBookContent(directory) }.getOrNull()
            }
    }

    private fun loadBookContent(packageDirectory: File): BookContent {
        return parser.parseBookContent(
            fallbackBookId = packageDirectory.parentFile?.name ?: packageDirectory.name,
            bookJson = readJsonObject(File(packageDirectory, BookPackageParser.BOOK_FILE_NAME)),
            pagesJson = readJsonArray(File(packageDirectory, BookPackageParser.PAGES_FILE_NAME)),
            wordsJson = readJsonArray(File(packageDirectory, BookPackageParser.WORDS_FILE_NAME)),
        ) { rawPath ->
            resolveFileUri(packageDirectory, rawPath)
        }
    }

    private fun resolveFileUri(packageDirectory: File, rawPath: String): String {
        val normalizedPath = rawPath.trim().removePrefix("/")
        return ContentUri.file(File(packageDirectory, normalizedPath))
    }

    private fun readJsonObject(file: File): JSONObject = JSONObject(file.readText())

    private fun readJsonArray(file: File): JSONArray = JSONArray(file.readText())
}
