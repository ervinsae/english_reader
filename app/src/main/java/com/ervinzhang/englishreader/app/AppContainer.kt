package com.ervinzhang.englishreader.app

import android.app.Application
import androidx.room.Room
import com.ervinzhang.englishreader.core.audio.AndroidAudioPlayer
import com.ervinzhang.englishreader.core.audio.AudioPlayer
import com.ervinzhang.englishreader.core.content.BookshelfRepository
import com.ervinzhang.englishreader.core.content.BookRepository
import com.ervinzhang.englishreader.core.content.BookshelfRefreshManager
import com.ervinzhang.englishreader.core.content.DefaultBookshelfRepository
import com.ervinzhang.englishreader.core.content.DefaultRemoteBookshelfManifestSource
import com.ervinzhang.englishreader.core.content.DefaultRemoteContentConfigSource
import com.ervinzhang.englishreader.core.content.HttpBookPackageDownloader
import com.ervinzhang.englishreader.core.content.LocalBookPackageDataSource
import com.ervinzhang.englishreader.core.content.LocalBookPackageInstaller
import com.ervinzhang.englishreader.core.content.LocalBookPackageStorage
import com.ervinzhang.englishreader.core.content.OfflineBookRepository
import com.ervinzhang.englishreader.core.content.RemoteContentAssetReader
import com.ervinzhang.englishreader.core.database.AppDatabase
import com.ervinzhang.englishreader.core.datastore.SessionStore
import com.ervinzhang.englishreader.core.reading.ReadingProgressRepository
import com.ervinzhang.englishreader.core.reading.RoomReadingProgressRepository
import com.ervinzhang.englishreader.feature.auth.data.AuthRepositoryImpl
import com.ervinzhang.englishreader.feature.auth.data.FakeAuthRemoteDataSource
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
import com.ervinzhang.englishreader.feature.dictionary.data.CachedDictionaryRepository
import com.ervinzhang.englishreader.feature.dictionary.data.DictionaryRepository
import com.ervinzhang.englishreader.feature.dictionary.data.PublicDictionaryRemoteDataSource
import com.ervinzhang.englishreader.feature.vocabulary.data.RoomVocabularyRepository
import com.ervinzhang.englishreader.feature.vocabulary.data.VocabularyRepository

class AppContainer(
    application: Application,
) {
    val sessionStore: SessionStore = SessionStore(application)

    val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "english_reader.db",
    ).fallbackToDestructiveMigration().build()

    val localBookPackageStorage: LocalBookPackageStorage = LocalBookPackageStorage(application)
    val localBookPackageDataSource: LocalBookPackageDataSource = LocalBookPackageDataSource(
        packageStorage = localBookPackageStorage,
    )
    val bookRepository: BookRepository = OfflineBookRepository(
        contentSources = listOf(localBookPackageDataSource),
    )
    val bookPackageInstaller: LocalBookPackageInstaller = LocalBookPackageInstaller(
        packageStorage = localBookPackageStorage,
    )
    val remoteContentConfigSource: DefaultRemoteContentConfigSource = DefaultRemoteContentConfigSource(
        context = application,
        packageStorage = localBookPackageStorage,
    )
    val remoteContentAssetReader: RemoteContentAssetReader = RemoteContentAssetReader(application)
    val remoteBookshelfManifestSource: DefaultRemoteBookshelfManifestSource = DefaultRemoteBookshelfManifestSource(
        configSource = remoteContentConfigSource,
        remoteAssetReader = remoteContentAssetReader,
    )
    val bookPackageDownloader: HttpBookPackageDownloader = HttpBookPackageDownloader(
        packageStorage = localBookPackageStorage,
    )
    val bookshelfRepository: BookshelfRepository = DefaultBookshelfRepository(
        bookRepository = bookRepository,
        remoteBookshelfManifestSource = remoteBookshelfManifestSource,
        packageStorage = localBookPackageStorage,
        bookPackageDownloader = bookPackageDownloader,
        packageInstaller = bookPackageInstaller,
    )
    val bookshelfRefreshManager: BookshelfRefreshManager = BookshelfRefreshManager(
        bookshelfRepository = bookshelfRepository,
    )
    val readingProgressRepository: ReadingProgressRepository = RoomReadingProgressRepository(
        database.readingProgressDao(),
    )
    val dictionaryRepository: DictionaryRepository = CachedDictionaryRepository(
        cacheDao = database.dictionaryCacheDao(),
        remoteDataSource = PublicDictionaryRemoteDataSource(),
    )
    val vocabularyRepository: VocabularyRepository = RoomVocabularyRepository(
        database.vocabularyDao(),
    )

    val authRepository: AuthRepository = AuthRepositoryImpl(
        remoteDataSource = FakeAuthRemoteDataSource(database),
        sessionStore = sessionStore,
    )

    val audioPlayer: AudioPlayer = AndroidAudioPlayer(application)
}
