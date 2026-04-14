package com.ervinzhang.englishreader.core.content

import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface BookPackageInstallResult {
    data class Success(
        val record: InstalledBookPackageRecord,
    ) : BookPackageInstallResult

    data class Failure(
        val archiveName: String,
        val reason: String,
    ) : BookPackageInstallResult
}

class LocalBookPackageInstaller(
    private val packageStorage: LocalBookPackageStorage,
) {
    suspend fun installFromArchive(
        archiveFile: File,
        source: String = SOURCE_MANUAL,
        deleteArchiveOnSuccess: Boolean = false,
    ): BookPackageInstallResult = withContext(Dispatchers.IO) {
        if (!archiveFile.isFile) {
            return@withContext BookPackageInstallResult.Failure(
                archiveName = archiveFile.name,
                reason = "安装包不存在",
            )
        }

        val stagingDirectory = packageStorage.createStagingDirectory(archiveFile.nameWithoutExtension)

        runCatching {
            unzipArchive(archiveFile = archiveFile, targetDirectory = stagingDirectory)
            val packageDirectory = locatePackageRoot(stagingDirectory)
            validatePackageDirectory(packageDirectory)
            val metadata = readPackageMetadata(packageDirectory)
            val record = InstalledBookPackageRecord(
                bookId = metadata.bookId,
                version = metadata.version,
                installedAt = System.currentTimeMillis(),
                source = source,
                archiveName = archiveFile.name,
            )
            packageStorage.commitInstalledPackage(
                bookId = metadata.bookId,
                preparedPackageDirectory = packageDirectory,
                record = record,
            )
            if (deleteArchiveOnSuccess) {
                archiveFile.delete()
            }
            BookPackageInstallResult.Success(record)
        }.getOrElse { error ->
            stagingDirectory.deleteRecursively()
            BookPackageInstallResult.Failure(
                archiveName = archiveFile.name,
                reason = error.message ?: "安装失败",
            )
        }
    }

    private fun unzipArchive(
        archiveFile: File,
        targetDirectory: File,
    ) {
        ZipFile(archiveFile).use { zipFile ->
            zipFile.entries().asSequence().forEach { entry ->
                val targetFile = File(targetDirectory, entry.name)
                val normalizedTargetPath = targetFile.canonicalPath
                if (!normalizedTargetPath.startsWith(targetDirectory.canonicalPath)) {
                    error("压缩包包含非法路径")
                }

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }
    }

    private fun locatePackageRoot(stagingDirectory: File): File {
        if (containsPackageFiles(stagingDirectory)) {
            return stagingDirectory
        }

        val childDirectories = stagingDirectory.listFiles()
            .orEmpty()
            .filter(File::isDirectory)
        return childDirectories.singleOrNull(::containsPackageFiles)
            ?: error("内容包缺少标准目录结构")
    }

    private fun validatePackageDirectory(packageDirectory: File) {
        if (!containsPackageFiles(packageDirectory)) {
            error("内容包缺少 ${BookPackageParser.BOOK_FILE_NAME} / ${BookPackageParser.PAGES_FILE_NAME} / ${BookPackageParser.WORDS_FILE_NAME}")
        }
    }

    private fun containsPackageFiles(directory: File): Boolean {
        return File(directory, BookPackageParser.BOOK_FILE_NAME).isFile &&
            File(directory, BookPackageParser.PAGES_FILE_NAME).isFile &&
            File(directory, BookPackageParser.WORDS_FILE_NAME).isFile
    }

    private fun readPackageMetadata(packageDirectory: File): PackageMetadata {
        val bookJson = JSONObject(File(packageDirectory, BookPackageParser.BOOK_FILE_NAME).readText())
        return PackageMetadata(
            bookId = bookJson.optString(BOOK_ID_KEY).ifBlank { packageDirectory.name },
            version = bookJson.optString(VERSION_KEY).takeIf { it.isNotBlank() },
        )
    }

    private data class PackageMetadata(
        val bookId: String,
        val version: String?,
    )

    private companion object {
        const val BOOK_ID_KEY = "id"
        const val VERSION_KEY = "version"
        const val SOURCE_MANUAL = "manual"
    }
}
