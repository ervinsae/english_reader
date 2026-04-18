package com.ervinzhang.englishreader.core.content

import com.ervinzhang.englishreader.core.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BookshelfBookPreview(
    val bookId: String,
    val title: String,
    val level: String,
    val coverUri: String,
    val pageCount: Int,
    val tags: List<String> = emptyList(),
    val isLocalContentReady: Boolean,
    val remotePackage: RemoteBookPackage? = null,
)

sealed interface BookshelfBookOpenStatus {
    data object CheckingLocal : BookshelfBookOpenStatus
    data class Downloading(val progressPercent: Int?) : BookshelfBookOpenStatus
    data object Installing : BookshelfBookOpenStatus
}

sealed interface EnsureBookOpenableResult {
    data object Ready : EnsureBookOpenableResult
    data class Failure(val message: String) : EnsureBookOpenableResult
}

interface BookshelfRepository {
    suspend fun getBooks(): List<BookshelfBookPreview>
    suspend fun refresh()
    suspend fun ensureBookOpenable(
        bookId: String,
        onStatus: suspend (BookshelfBookOpenStatus) -> Unit = {},
    ): EnsureBookOpenableResult
}

class DefaultBookshelfRepository(
    private val bookRepository: BookRepository,
    private val remoteBookshelfManifestSource: RemoteBookshelfManifestSource,
    private val packageStorage: LocalBookPackageStorage,
    private val bookPackageDownloader: HttpBookPackageDownloader,
    private val packageInstaller: LocalBookPackageInstaller,
) : BookshelfRepository {
    private var cachedRemoteBooksById: Map<String, RemoteBookshelfBook>? = null

    override suspend fun getBooks(): List<BookshelfBookPreview> = withContext(Dispatchers.IO) {
        val localBooks = bookRepository.getBooks()
        val remoteBooksById = loadRemoteBooksById()
        buildMergedBooks(localBooks = localBooks, remoteBooksById = remoteBooksById)
    }

    override suspend fun refresh() {
        cachedRemoteBooksById = null
        bookRepository.refresh()
    }

    override suspend fun ensureBookOpenable(
        bookId: String,
        onStatus: suspend (BookshelfBookOpenStatus) -> Unit,
    ): EnsureBookOpenableResult = withContext(Dispatchers.IO) {
        onStatus(BookshelfBookOpenStatus.CheckingLocal)
        if (bookRepository.getBookContent(bookId) != null) {
            return@withContext EnsureBookOpenableResult.Ready
        }

        val remotePackage = loadRemoteBooksById()[bookId]?.packageInfo
            ?: return@withContext EnsureBookOpenableResult.Failure("这本书暂时还不能下载")

        if (packageStorage.isInstalled(bookId = bookId, version = remotePackage.version)) {
            bookRepository.refresh()
            if (bookRepository.getBookContent(bookId) != null) {
                return@withContext EnsureBookOpenableResult.Ready
            }
        }

        onStatus(BookshelfBookOpenStatus.Downloading(progressPercent = 0))
        val archiveFile = bookPackageDownloader.download(remotePackage) { percent ->
            onStatus(BookshelfBookOpenStatus.Downloading(progressPercent = percent.coerceIn(0, 100)))
        } ?: return@withContext EnsureBookOpenableResult.Failure("下载失败，请稍后再试")

        onStatus(BookshelfBookOpenStatus.Installing)
        when (
            val installResult = packageInstaller.installFromArchive(
                archiveFile = archiveFile,
                source = SOURCE_REMOTE,
                deleteArchiveOnSuccess = true,
            )
        ) {
            is BookPackageInstallResult.Success -> {
                bookRepository.refresh()
                if (bookRepository.getBookContent(bookId) != null) {
                    EnsureBookOpenableResult.Ready
                } else {
                    EnsureBookOpenableResult.Failure("安装完成，但内容暂时无法打开")
                }
            }

            is BookPackageInstallResult.Failure -> {
                EnsureBookOpenableResult.Failure(installResult.reason.ifBlank { "安装失败，请重试" })
            }
        }
    }

    private suspend fun loadRemoteBooksById(): Map<String, RemoteBookshelfBook> {
        cachedRemoteBooksById?.let { return it }

        val remoteBooksById = remoteBookshelfManifestSource.fetchManifest()
            ?.books
            .orEmpty()
            .associateBy { it.bookId }
        cachedRemoteBooksById = remoteBooksById
        return remoteBooksById
    }

    private fun buildMergedBooks(
        localBooks: List<Book>,
        remoteBooksById: Map<String, RemoteBookshelfBook>,
    ): List<BookshelfBookPreview> {
        val localBooksById = localBooks.associateBy { it.id }
        val remoteOrder = remoteBooksById.keys.toList()
        val mergedBooks = linkedMapOf<String, BookshelfBookPreview>()

        remoteOrder.forEach { bookId ->
            val localBook = localBooksById[bookId]
            val remoteBook = remoteBooksById.getValue(bookId)
            when {
                localBook != null -> {
                    mergedBooks[bookId] = localBook.toBookshelfPreview(remoteBook.packageInfo)
                }

                remoteBook.packageInfo != null -> {
                    mergedBooks[bookId] = remoteBook.toBookshelfPreview()
                }
            }
        }

        localBooks
            .filterNot { mergedBooks.containsKey(it.id) }
            .sortedBy { it.title }
            .forEach { localBook ->
                mergedBooks[localBook.id] = localBook.toBookshelfPreview(
                    remotePackage = remoteBooksById[localBook.id]?.packageInfo,
                )
            }

        return mergedBooks.values.toList()
    }

    private fun Book.toBookshelfPreview(remotePackage: RemoteBookPackage?): BookshelfBookPreview {
        return BookshelfBookPreview(
            bookId = id,
            title = title,
            level = level,
            coverUri = coverUri,
            pageCount = pageCount,
            tags = tags,
            isLocalContentReady = true,
            remotePackage = remotePackage,
        )
    }

    private fun RemoteBookshelfBook.toBookshelfPreview(): BookshelfBookPreview {
        return BookshelfBookPreview(
            bookId = bookId,
            title = title,
            level = level,
            coverUri = coverUri,
            pageCount = pageCount,
            tags = tags,
            isLocalContentReady = false,
            remotePackage = packageInfo,
        )
    }

    private companion object {
        const val SOURCE_REMOTE = "remote"
    }
}
