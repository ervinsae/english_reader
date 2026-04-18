package com.ervinzhang.englishreader.core.content

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RemoteBookshelfBook(
    val bookId: String,
    val title: String,
    val level: String,
    val pageCount: Int,
    val coverUri: String,
    val tags: List<String> = emptyList(),
    val packageInfo: RemoteBookPackage? = null,
)

data class RemoteBookshelfManifest(
    val books: List<RemoteBookshelfBook>,
)

interface RemoteBookshelfManifestSource {
    suspend fun fetchManifest(): RemoteBookshelfManifest?
}

class RemoteContentAssetReader(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun readBundledAsset(fileName: String): String {
        return appContext.assets.open("${DefaultRemoteContentConfigSource.ASSET_CONTENT_DIRECTORY}/$fileName")
            .bufferedReader()
            .use { it.readText() }
    }
}

class DefaultRemoteBookshelfManifestSource(
    private val configSource: RemoteContentConfigSource,
    private val remoteAssetReader: RemoteContentAssetReader,
) : RemoteBookshelfManifestSource {
    override suspend fun fetchManifest(): RemoteBookshelfManifest? = withContext(Dispatchers.IO) {
        val config = configSource.loadConfig()
        val configuredBookshelfUrl = config.bookshelfUrl?.trim().orEmpty()
        if (configuredBookshelfUrl.isBlank()) {
            return@withContext loadBundledManifest()
        }

        runCatching { fetchManifestFromUrl(configuredBookshelfUrl) }
            .getOrElse { loadBundledManifest() }
    }

    private fun loadBundledManifest(): RemoteBookshelfManifest? {
        return runCatching {
            parseManifest(JSONObject(remoteAssetReader.readBundledAsset(REMOTE_BOOKSHELF_FILE_NAME)))
        }.getOrNull()
    }

    private fun fetchManifestFromUrl(bookshelfUrl: String): RemoteBookshelfManifest {
        val connection = URL(bookshelfUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        connection.instanceFollowRedirects = true

        return connection.inputStream.bufferedReader().use { reader ->
            parseManifest(JSONObject(reader.readText()))
        }
    }

    private fun parseManifest(json: JSONObject): RemoteBookshelfManifest {
        val booksJson = json.optJSONArray(BOOKS_KEY) ?: JSONArray()
        val books = buildList {
            for (index in 0 until booksJson.length()) {
                val bookJson = booksJson.optJSONObject(index) ?: continue
                val bookId = bookJson.optString(BOOK_ID_KEY).trim()
                val title = bookJson.optString(TITLE_KEY).trim()
                val level = bookJson.optString(LEVEL_KEY).trim()
                val pageCount = bookJson.optInt(PAGE_COUNT_KEY, 0)
                val coverUri = parseCoverUri(bookJson).trim()
                if (bookId.isBlank() || title.isBlank() || level.isBlank() || pageCount <= 0 || coverUri.isBlank()) {
                    continue
                }

                val packageInfo = parsePackageInfo(
                    bookId = bookId,
                    title = title,
                    packageJson = bookJson.optJSONObject(PACKAGE_KEY),
                )
                add(
                    RemoteBookshelfBook(
                        bookId = bookId,
                        title = title,
                        level = level,
                        pageCount = pageCount,
                        coverUri = coverUri,
                        tags = parseTags(bookJson.optJSONArray(TAGS_KEY)),
                        packageInfo = packageInfo,
                    ),
                )
            }
        }
        return RemoteBookshelfManifest(books = books)
    }

    private fun parseCoverUri(bookJson: JSONObject): String {
        val explicitCoverUri = bookJson.optString(COVER_URI_KEY).trim()
        if (explicitCoverUri.isNotBlank()) {
            return explicitCoverUri
        }

        val coverAsset = bookJson.optString(COVER_ASSET_KEY).trim().removePrefix("/")
        return coverAsset.takeIf { it.isNotBlank() }?.let(ContentUri::asset).orEmpty()
    }

    private fun parseTags(tagsJson: JSONArray?): List<String> {
        if (tagsJson == null) return emptyList()
        return buildList {
            for (index in 0 until tagsJson.length()) {
                val tag = tagsJson.optString(index).trim()
                if (tag.isNotBlank()) add(tag)
            }
        }
    }

    private fun parsePackageInfo(
        bookId: String,
        title: String,
        packageJson: JSONObject?,
    ): RemoteBookPackage? {
        if (packageJson == null) return null
        val downloadUrl = packageJson.optString(DOWNLOAD_URL_KEY).trim()
        if (downloadUrl.isBlank()) return null

        return RemoteBookPackage(
            bookId = bookId,
            title = title,
            version = packageJson.optString(VERSION_KEY).takeIf { it.isNotBlank() },
            downloadUrl = downloadUrl,
            sha256 = packageJson.optString(SHA256_KEY).takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val REMOTE_BOOKSHELF_FILE_NAME = "bookshelf.json"
        const val BOOKS_KEY = "books"
        const val BOOK_ID_KEY = "bookId"
        const val TITLE_KEY = "title"
        const val LEVEL_KEY = "level"
        const val PAGE_COUNT_KEY = "pageCount"
        const val TAGS_KEY = "tags"
        const val COVER_URI_KEY = "coverUri"
        const val COVER_ASSET_KEY = "coverAsset"
        const val PACKAGE_KEY = "package"
        const val VERSION_KEY = "version"
        const val DOWNLOAD_URL_KEY = "downloadUrl"
        const val SHA256_KEY = "sha256"
        const val NETWORK_TIMEOUT_MS = 15_000
    }
}
