package com.ervinzhang.englishreader.core.content

import android.content.Context
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ContentUri {
    private const val ASSET_PREFIX = "asset://"
    private const val FILE_PREFIX = "file://"
    private const val HTTP_PREFIX = "http://"
    private const val HTTPS_PREFIX = "https://"

    fun asset(path: String): String {
        return "$ASSET_PREFIX${path.trim().removePrefix("/")}"
    }

    fun file(file: File): String {
        return "$FILE_PREFIX${file.absolutePath}"
    }

    fun open(context: Context, uri: String): InputStream {
        asRemoteUrl(uri)?.let { remoteUrl ->
            val connection = URL(remoteUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = NETWORK_TIMEOUT_MS
            connection.readTimeout = NETWORK_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            return connection.inputStream
        }

        val assetPath = asAssetPath(uri)
        if (assetPath != null) {
            return context.assets.open(assetPath)
        }

        val file = asFile(uri) ?: error("Unsupported content uri: $uri")
        return file.inputStream()
    }

    fun asAssetPath(uri: String): String? {
        return when {
            uri.startsWith(ASSET_PREFIX) -> uri.removePrefix(ASSET_PREFIX)
            uri.startsWith(HTTP_PREFIX) || uri.startsWith(HTTPS_PREFIX) -> null
            uri.startsWith(FILE_PREFIX) -> null
            else -> uri.trim().takeIf { it.isNotBlank() }
        }
    }

    fun asFile(uri: String): File? {
        return if (uri.startsWith(FILE_PREFIX)) {
            File(uri.removePrefix(FILE_PREFIX))
        } else {
            null
        }
    }

    fun asRemoteUrl(uri: String): String? {
        return uri.trim().takeIf {
            it.startsWith(HTTP_PREFIX) || it.startsWith(HTTPS_PREFIX)
        }
    }

    fun cacheKey(uri: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private const val NETWORK_TIMEOUT_MS = 15_000
}
