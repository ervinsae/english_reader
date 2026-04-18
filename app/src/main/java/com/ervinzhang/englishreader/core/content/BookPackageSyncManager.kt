package com.ervinzhang.englishreader.core.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContentSyncResult(
    val installedFromInbox: Int,
    val downloadedFromCatalog: Int,
    val catalogPackagesSeen: Int,
    val issues: List<String> = emptyList(),
) {
    val summary: String
        get() = buildString {
            append("本地导入 ")
            append(installedFromInbox)
            append(" 个")
            append("，远程下载 ")
            append(downloadedFromCatalog)
            append(" 个")
            if (catalogPackagesSeen > 0) {
                append("，远程目录共 ")
                append(catalogPackagesSeen)
                append(" 个包")
            }
            if (issues.isNotEmpty()) {
                append("。")
                append(issues.first())
            }
        }
}

class BookPackageSyncManager(
    private val bookRepository: BookRepository,
    private val packageStorage: LocalBookPackageStorage,
    private val packageInstaller: LocalBookPackageInstaller,
    private val remoteContentCatalogSource: RemoteContentCatalogSource,
    private val bookPackageDownloader: HttpBookPackageDownloader,
) {
    suspend fun sync(
        onProgress: suspend (Int) -> Unit = {},
    ): ContentSyncResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<String>()
        onProgress(0)

        val installedFromInbox = packageStorage.listInboxArchives()
            .map { archive ->
                packageInstaller.installFromArchive(
                    archiveFile = archive,
                    source = SOURCE_INBOX,
                    deleteArchiveOnSuccess = true,
                )
            }
            .countSuccessful(issues)

        onProgress(5)
        val catalog = runCatching { remoteContentCatalogSource.fetchCatalog() }
            .getOrElse {
                issues += "远程目录读取失败"
                null
            }

        val pendingPackages = catalog?.packages.orEmpty()
            .filterNot { remotePackage ->
                packageStorage.isInstalled(remotePackage.bookId, remotePackage.version)
            }

        var downloadedFromCatalog = 0
        if (pendingPackages.isEmpty()) {
            onProgress(100)
        } else {
            val totalPackages = pendingPackages.size
            pendingPackages.forEachIndexed { index, remotePackage ->
                val segmentStart = (index * 100) / totalPackages
                val segmentEnd = ((index + 1) * 100) / totalPackages
                val downloadStart = maxOf(segmentStart, 5)
                val downloadEnd = maxOf(downloadStart, segmentStart + ((segmentEnd - segmentStart) * 9 / 10))

                val archiveFile = bookPackageDownloader.download(remotePackage) { percent ->
                    val mapped = downloadStart + ((downloadEnd - downloadStart) * percent / 100)
                    onProgress(mapped.coerceIn(0, 99))
                }
                if (archiveFile == null) {
                    issues += "远程包 ${remotePackage.bookId} 下载失败"
                    return@forEachIndexed
                }

                onProgress((segmentStart + ((segmentEnd - segmentStart) * 95 / 100)).coerceIn(0, 99))
                when (
                    val result = packageInstaller.installFromArchive(
                        archiveFile = archiveFile,
                        source = SOURCE_REMOTE,
                        deleteArchiveOnSuccess = true,
                    )
                ) {
                    is BookPackageInstallResult.Success -> downloadedFromCatalog += 1
                    is BookPackageInstallResult.Failure -> issues += "远程包 ${result.archiveName} 安装失败"
                }

                onProgress(segmentEnd.coerceIn(0, 100))
            }
        }

        if (installedFromInbox > 0 || downloadedFromCatalog > 0) {
            bookRepository.refresh()
        }

        onProgress(100)
        ContentSyncResult(
            installedFromInbox = installedFromInbox,
            downloadedFromCatalog = downloadedFromCatalog,
            catalogPackagesSeen = catalog?.packages?.size ?: 0,
            issues = issues,
        )
    }

    private fun List<BookPackageInstallResult>.countSuccessful(issues: MutableList<String>): Int {
        var successCount = 0
        forEach { result ->
            when (result) {
                is BookPackageInstallResult.Success -> successCount += 1
                is BookPackageInstallResult.Failure -> issues += "导入包 ${result.archiveName} 安装失败"
            }
        }
        return successCount
    }

    private companion object {
        const val SOURCE_INBOX = "inbox"
        const val SOURCE_REMOTE = "remote"
    }
}
