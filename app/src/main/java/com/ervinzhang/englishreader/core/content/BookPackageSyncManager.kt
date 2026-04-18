package com.ervinzhang.englishreader.core.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContentSyncResult(
    val installedFromInbox: Int,
    val downloadedFromCatalog: Int,
    val catalogPackagesSeen: Int,
    val issues: List<String> = emptyList(),
) {
    val hasChanges: Boolean
        get() = installedFromInbox > 0 || downloadedFromCatalog > 0

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
        val inboxArchives = packageStorage.listInboxArchives()
        val catalog = runCatching { remoteContentCatalogSource.fetchCatalog() }
            .getOrElse {
                issues += "远程目录读取失败"
                null
            }

        val pendingPackages = catalog?.packages.orEmpty()
            .filterNot { remotePackage ->
                packageStorage.isInstalled(remotePackage.bookId, remotePackage.version)
            }

        val totalWorkUnits = inboxArchives.size + pendingPackages.size
        var lastReportedProgress = -1
        suspend fun reportProgress(completedUnits: Int, currentUnitPercent: Int = 0) {
            if (totalWorkUnits == 0) return
            val boundedUnitPercent = currentUnitPercent.coerceIn(0, 100)
            val percent = (((completedUnits * 100) + boundedUnitPercent) / totalWorkUnits)
                .coerceIn(0, 100)
            if (percent != lastReportedProgress) {
                lastReportedProgress = percent
                onProgress(percent)
            }
        }

        var installedFromInbox = 0
        var completedUnits = 0
        inboxArchives.forEach { archive ->
            reportProgress(completedUnits)
            when (
                val result = packageInstaller.installFromArchive(
                    archiveFile = archive,
                    source = SOURCE_INBOX,
                    deleteArchiveOnSuccess = true,
                )
            ) {
                is BookPackageInstallResult.Success -> installedFromInbox += 1
                is BookPackageInstallResult.Failure -> issues += "导入包 ${result.archiveName} 安装失败"
            }
            completedUnits += 1
            reportProgress(completedUnits)
        }

        var downloadedFromCatalog = 0
        pendingPackages.forEach { remotePackage ->
            reportProgress(completedUnits)
            val archiveFile = bookPackageDownloader.download(remotePackage) { percent ->
                reportProgress(
                    completedUnits = completedUnits,
                    currentUnitPercent = (percent.coerceIn(0, 100) * REMOTE_DOWNLOAD_SHARE_PERCENT) / 100,
                )
            }
            if (archiveFile == null) {
                issues += "远程包 ${remotePackage.bookId} 下载失败"
                completedUnits += 1
                reportProgress(completedUnits)
                return@forEach
            }

            reportProgress(
                completedUnits = completedUnits,
                currentUnitPercent = REMOTE_DOWNLOAD_SHARE_PERCENT,
            )
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

            completedUnits += 1
            reportProgress(completedUnits)
        }

        if (installedFromInbox > 0 || downloadedFromCatalog > 0) {
            bookRepository.refresh()
        }

        ContentSyncResult(
            installedFromInbox = installedFromInbox,
            downloadedFromCatalog = downloadedFromCatalog,
            catalogPackagesSeen = catalog?.packages?.size ?: 0,
            issues = issues,
        )
    }

    private companion object {
        const val SOURCE_INBOX = "inbox"
        const val SOURCE_REMOTE = "remote"
        const val REMOTE_DOWNLOAD_SHARE_PERCENT = 90
    }
}
