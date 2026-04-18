package com.ervinzhang.englishreader.core.content

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RemoteContentCatalogConfig(
    val catalogUrl: String? = null,
    val autoSyncOnLaunch: Boolean = false,
)

data class RemoteBookPackage(
    val bookId: String,
    val title: String,
    val version: String? = null,
    val downloadUrl: String,
    val sha256: String? = null,
)

data class RemoteContentCatalog(
    val packages: List<RemoteBookPackage>,
)

interface RemoteContentCatalogSource {
    suspend fun fetchCatalog(): RemoteContentCatalog?
    suspend fun loadConfig(): RemoteContentCatalogConfig
}

class DefaultRemoteContentCatalogSource(
    context: Context,
    private val packageStorage: LocalBookPackageStorage,
) : RemoteContentCatalogSource {
    private val appContext = context.applicationContext

    override suspend fun fetchCatalog(): RemoteContentCatalog? = withContext(Dispatchers.IO) {
        val config = loadConfig()
        val configuredCatalogUrl = config.catalogUrl?.trim().orEmpty()
        if (configuredCatalogUrl.isBlank()) {
            return@withContext loadBundledCatalog()
        }

        runCatching { fetchCatalogFromUrl(configuredCatalogUrl) }
            .getOrElse { loadBundledCatalog() }
    }

    override suspend fun loadConfig(): RemoteContentCatalogConfig = withContext(Dispatchers.IO) {
        val overrideFile = packageStorage.configFile(CATALOG_CONFIG_FILE_NAME)
        val rawConfig = when {
            overrideFile.isFile -> overrideFile.readText()
            else -> appContext.assets.open("$ASSET_CONTENT_DIRECTORY/$CATALOG_CONFIG_FILE_NAME")
                .bufferedReader()
                .use { it.readText() }
        }
        val json = JSONObject(rawConfig)
        RemoteContentCatalogConfig(
            catalogUrl = json.optString(CATALOG_URL_KEY).takeIf { it.isNotBlank() },
            autoSyncOnLaunch = json.optBoolean(AUTO_SYNC_ON_LAUNCH_KEY, false),
        )
    }

    private fun loadBundledCatalog(): RemoteContentCatalog? {
        return runCatching {
            appContext.assets.open("$ASSET_CONTENT_DIRECTORY/$REMOTE_CATALOG_FILE_NAME")
                .bufferedReader()
                .use { reader -> parseCatalog(JSONObject(reader.readText())) }
        }.getOrNull()
    }

    private fun fetchCatalogFromUrl(catalogUrl: String): RemoteContentCatalog {
        val connection = URL(catalogUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        connection.instanceFollowRedirects = true

        return connection.inputStream.bufferedReader().use { reader ->
            parseCatalog(JSONObject(reader.readText()))
        }
    }

    private fun parseCatalog(json: JSONObject): RemoteContentCatalog {
        val packagesJson = json.optJSONArray(PACKAGES_KEY) ?: JSONArray()
        val packages = buildList {
            for (index in 0 until packagesJson.length()) {
                val packageJson = packagesJson.getJSONObject(index)
                val bookId = packageJson.optString(BOOK_ID_KEY).trim()
                val title = packageJson.optString(TITLE_KEY).trim()
                val downloadUrl = packageJson.optString(DOWNLOAD_URL_KEY).trim()
                if (bookId.isBlank() || title.isBlank() || downloadUrl.isBlank()) continue

                add(
                    RemoteBookPackage(
                        bookId = bookId,
                        title = title,
                        version = packageJson.optString(VERSION_KEY).takeIf { it.isNotBlank() },
                        downloadUrl = downloadUrl,
                        sha256 = packageJson.optString(SHA256_KEY).takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
        return RemoteContentCatalog(packages = packages)
    }

    private companion object {
        const val ASSET_CONTENT_DIRECTORY = "content"
        const val CATALOG_CONFIG_FILE_NAME = "catalog-config.json"
        const val REMOTE_CATALOG_FILE_NAME = "remote-catalog.json"
        const val CATALOG_URL_KEY = "catalogUrl"
        const val AUTO_SYNC_ON_LAUNCH_KEY = "autoSyncOnLaunch"
        const val PACKAGES_KEY = "packages"
        const val BOOK_ID_KEY = "bookId"
        const val TITLE_KEY = "title"
        const val VERSION_KEY = "version"
        const val DOWNLOAD_URL_KEY = "downloadUrl"
        const val SHA256_KEY = "sha256"
        const val NETWORK_TIMEOUT_MS = 15_000
    }
}

class HttpBookPackageDownloader(
    private val packageStorage: LocalBookPackageStorage,
) {
    suspend fun download(
        remoteBookPackage: RemoteBookPackage,
        onProgress: suspend (Int) -> Unit = {},
    ): File? = withContext(Dispatchers.IO) {
        val targetFile = packageStorage.downloadArchiveFile(
            bookId = remoteBookPackage.bookId,
            version = remoteBookPackage.version,
        )
        val connection = runCatching {
            URL(remoteBookPackage.downloadUrl).openConnection() as HttpURLConnection
        }.getOrNull() ?: return@withContext null

        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        connection.instanceFollowRedirects = true

        runCatching {
            val contentLength = connection.contentLengthLong.takeIf { it > 0L }
            var downloadedBytes = 0L
            var lastPercent = -1

            connection.inputStream.use { input ->
                targetFile.parentFile?.mkdirs()
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead <= 0) break
                        output.write(buffer, 0, bytesRead)
                        if (contentLength != null) {
                            downloadedBytes += bytesRead
                            val percent = ((downloadedBytes * 100) / contentLength)
                                .toInt()
                                .coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (contentLength == null) {
                onProgress(100)
            }

            if (!remoteBookPackage.sha256.isNullOrBlank()) {
                val actualChecksum = targetFile.sha256()
                if (!actualChecksum.equals(remoteBookPackage.sha256, ignoreCase = true)) {
                    targetFile.delete()
                    return@runCatching null
                }
            }

            targetFile
        }.getOrNull().also {
            connection.disconnect()
        }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead <= 0) break
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val NETWORK_TIMEOUT_MS = 15_000
    }
}
