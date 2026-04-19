package com.ervinzhang.englishreader.core.content

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RemoteBookPackage(
    val bookId: String,
    val title: String,
    val version: String? = null,
    val downloadUrl: String,
    val sha256: String? = null,
)

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
