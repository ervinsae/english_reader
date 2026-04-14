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
    suspend fun sync(): ContentSyncResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<String>()

        val installedFromInbox = packageStorage.listInboxArchives()
            .map { archive ->
                packageInstaller.installFromArchive(
                    archiveFile = archive,
                    source = SOURCE_INBOX,
                    deleteArchiveOnSuccess = true,
                )
            }
            .countSuccessful(issues)

        val catalog = runCatching { remoteContentCatalogSource.fetchCatalog() }
            .getOrElse {
                issues += "远程目录读取失败"
                null
            }

        var downloadedFromCatalog = 0
        catalog?.packages.orEmpty().forEach { remotePackage ->
            if (packageStorage.isInstalled(remotePackage.bookId, remotePackage.version)) {
                return@forEach
            }

            val archiveFile = bookPackageDownloader.download(remotePackage)
            if (archiveFile == null) {
                issues += "远程包 ${remotePackage.bookId} 下载失败"
                return@forEach
            }

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
