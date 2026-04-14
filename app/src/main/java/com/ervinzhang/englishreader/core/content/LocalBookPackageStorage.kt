package com.ervinzhang.englishreader.core.content

import android.content.Context
import java.io.File
import org.json.JSONObject

data class InstalledBookPackageRecord(
    val bookId: String,
    val version: String? = null,
    val installedAt: Long,
    val source: String,
    val archiveName: String? = null,
)

class LocalBookPackageStorage(
    context: Context,
) {
    private val appContext = context.applicationContext

    val rootDirectory: File = File(appContext.filesDir, ROOT_DIRECTORY_NAME).apply { mkdirs() }
    private val installedRootDirectory = File(rootDirectory, INSTALLED_DIRECTORY_NAME).apply { mkdirs() }
    private val stagingRootDirectory = File(rootDirectory, STAGING_DIRECTORY_NAME).apply { mkdirs() }
    private val downloadsRootDirectory = File(rootDirectory, DOWNLOADS_DIRECTORY_NAME).apply { mkdirs() }
    private val inboxRootDirectory = File(rootDirectory, INBOX_DIRECTORY_NAME).apply { mkdirs() }
    private val configRootDirectory = File(rootDirectory, CONFIG_DIRECTORY_NAME).apply { mkdirs() }

    fun listInstalledPackageDirectories(): List<File> {
        return installedRootDirectory.listFiles()
            .orEmpty()
            .map { File(it, PACKAGE_DIRECTORY_NAME) }
            .filter(File::isDirectory)
    }

    fun listInboxArchives(): List<File> {
        return inboxRootDirectory.listFiles()
            .orEmpty()
            .filter { file -> file.isFile && file.extension.equals(ZIP_EXTENSION, ignoreCase = true) }
            .sortedBy { it.name }
    }

    fun isInstalled(bookId: String, version: String? = null): Boolean {
        val installedDirectory = installedPackageDirectory(bookId)
        if (!installedDirectory.isDirectory) return false
        if (version.isNullOrBlank()) return true
        return readInstalledPackageRecord(bookId)?.version == version
    }

    fun installedPackageDirectory(bookId: String): File {
        return File(installedBookRootDirectory(bookId), PACKAGE_DIRECTORY_NAME)
    }

    fun installedMetadataFile(bookId: String): File {
        return File(installedBookRootDirectory(bookId), METADATA_FILE_NAME)
    }

    fun createStagingDirectory(prefix: String): File {
        return File(
            stagingRootDirectory,
            "${sanitizeSegment(prefix)}-${System.currentTimeMillis()}",
        ).apply { mkdirs() }
    }

    fun downloadArchiveFile(bookId: String, version: String?): File {
        val versionSuffix = version?.takeIf(String::isNotBlank)?.let { "-$it" }.orEmpty()
        return File(downloadsRootDirectory, "${sanitizeSegment(bookId)}$versionSuffix.$ZIP_EXTENSION")
    }

    fun configFile(fileName: String): File {
        return File(configRootDirectory, fileName)
    }

    fun inboxDirectory(): File = inboxRootDirectory

    fun commitInstalledPackage(
        bookId: String,
        preparedPackageDirectory: File,
        record: InstalledBookPackageRecord,
    ) {
        val targetRootDirectory = installedBookRootDirectory(bookId)
        val targetPackageDirectory = File(targetRootDirectory, PACKAGE_DIRECTORY_NAME)

        if (targetRootDirectory.exists()) {
            targetRootDirectory.deleteRecursively()
        }
        targetRootDirectory.mkdirs()

        if (!preparedPackageDirectory.renameTo(targetPackageDirectory)) {
            preparedPackageDirectory.copyRecursively(targetPackageDirectory, overwrite = true)
            preparedPackageDirectory.deleteRecursively()
        }

        writeInstalledPackageRecord(bookId = bookId, record = record)
    }

    fun readInstalledPackageRecord(bookId: String): InstalledBookPackageRecord? {
        val metadataFile = installedMetadataFile(bookId)
        if (!metadataFile.isFile) return null

        return runCatching {
            val json = JSONObject(metadataFile.readText())
            InstalledBookPackageRecord(
                bookId = json.getString(BOOK_ID_KEY),
                version = json.optString(VERSION_KEY).takeIf { it.isNotBlank() },
                installedAt = json.optLong(INSTALLED_AT_KEY, 0L),
                source = json.optString(SOURCE_KEY).ifBlank { SOURCE_MANUAL },
                archiveName = json.optString(ARCHIVE_NAME_KEY).takeIf { it.isNotBlank() },
            )
        }.getOrNull()
    }

    private fun writeInstalledPackageRecord(
        bookId: String,
        record: InstalledBookPackageRecord,
    ) {
        val json = JSONObject()
            .put(BOOK_ID_KEY, record.bookId)
            .put(VERSION_KEY, record.version)
            .put(INSTALLED_AT_KEY, record.installedAt)
            .put(SOURCE_KEY, record.source)
            .put(ARCHIVE_NAME_KEY, record.archiveName)
        installedMetadataFile(bookId).writeText(json.toString(2))
    }

    private fun installedBookRootDirectory(bookId: String): File {
        return File(installedRootDirectory, sanitizeSegment(bookId))
    }

    private fun sanitizeSegment(raw: String): String {
        return raw.trim()
            .ifBlank { "package" }
            .replace(SEGMENT_SANITIZE_REGEX, "_")
    }

    private companion object {
        const val ROOT_DIRECTORY_NAME = "book-packages"
        const val INSTALLED_DIRECTORY_NAME = "installed"
        const val STAGING_DIRECTORY_NAME = "staging"
        const val DOWNLOADS_DIRECTORY_NAME = "downloads"
        const val INBOX_DIRECTORY_NAME = "inbox"
        const val CONFIG_DIRECTORY_NAME = "config"
        const val PACKAGE_DIRECTORY_NAME = "package"
        const val METADATA_FILE_NAME = "install.json"
        const val ZIP_EXTENSION = "zip"
        const val BOOK_ID_KEY = "bookId"
        const val VERSION_KEY = "version"
        const val INSTALLED_AT_KEY = "installedAt"
        const val SOURCE_KEY = "source"
        const val ARCHIVE_NAME_KEY = "archiveName"
        const val SOURCE_MANUAL = "manual"
        val SEGMENT_SANITIZE_REGEX = Regex("[^A-Za-z0-9._-]+")
    }
}
