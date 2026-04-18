package com.ervinzhang.englishreader.core.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContentSyncResult(
    val installedFromInbox: Int,
    val remoteBooksSeen: Int,
    val issues: List<String> = emptyList(),
) {
    val hasChanges: Boolean
        get() = installedFromInbox > 0

    val summary: String
        get() = buildString {
            append("本地导入 ")
            append(installedFromInbox)
            append(" 个")
            if (remoteBooksSeen > 0) {
                append("，远程书架共 ")
                append(remoteBooksSeen)
                append(" 本")
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
    private val remoteBookshelfManifestSource: RemoteBookshelfManifestSource,
) {
    suspend fun sync(
        onProgress: suspend (Int) -> Unit = {},
    ): ContentSyncResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<String>()
        val inboxArchives = packageStorage.listInboxArchives()
        val remoteManifest = runCatching { remoteBookshelfManifestSource.fetchManifest() }
            .getOrElse {
                issues += "远程书架读取失败"
                null
            }
        val totalWorkUnits = inboxArchives.size
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

        if (installedFromInbox > 0) {
            bookRepository.refresh()
        }

        ContentSyncResult(
            installedFromInbox = installedFromInbox,
            remoteBooksSeen = remoteManifest?.books?.size ?: 0,
            issues = issues,
        )
    }

    private companion object {
        const val SOURCE_INBOX = "inbox"
    }
}
