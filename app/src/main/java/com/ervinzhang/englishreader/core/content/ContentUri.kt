package com.ervinzhang.englishreader.core.content

import android.content.Context
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object ContentUri {
    private const val ASSET_PREFIX = "asset://"
    private const val FILE_PREFIX = "file://"

    fun asset(path: String): String {
        return "$ASSET_PREFIX${path.trim().removePrefix("/")}"
    }

    fun file(file: File): String {
        return "$FILE_PREFIX${file.absolutePath}"
    }

    fun open(context: Context, uri: String): InputStream {
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

    fun cacheKey(uri: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
