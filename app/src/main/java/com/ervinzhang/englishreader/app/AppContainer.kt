package com.ervinzhang.englishreader.app

import android.app.Application
import androidx.room.Room
import com.ervinzhang.englishreader.core.audio.AndroidAudioPlayer
import com.ervinzhang.englishreader.core.audio.AudioPlayer
import com.ervinzhang.englishreader.core.content.AssetBookDataSource
import com.ervinzhang.englishreader.core.content.BookPackageSyncManager
import com.ervinzhang.englishreader.core.content.BookRepository
import com.ervinzhang.englishreader.core.content.DefaultRemoteContentCatalogSource
import com.ervinzhang.englishreader.core.content.HttpBookPackageDownloader
import com.ervinzhang.englishreader.core.content.LocalBookPackageDataSource
import com.ervinzhang.englishreader.core.content.LocalBookPackageInstaller
import com.ervinzhang.englishreader.core.content.LocalBookPackageStorage
import com.ervinzhang.englishreader.core.content.OfflineBookRepository
import com.ervinzhang.englishreader.core.database.AppDatabase
import com.ervinzhang.englishreader.core.datastore.SessionStore
import com.ervinzhang.englishreader.core.reading.ReadingProgressRepository
import com.ervinzhang.englishreader.core.reading.RoomReadingProgressRepository
import com.ervinzhang.englishreader.feature.auth.data.AuthRepositoryImpl
import com.ervinzhang.englishreader.feature.auth.data.FakeAuthRemoteDataSource
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository
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
    val assetBookDataSource: AssetBookDataSource = AssetBookDataSource(application)
    val localBookPackageDataSource: LocalBookPackageDataSource = LocalBookPackageDataSource(
        packageStorage = localBookPackageStorage,
    )
    val bookRepository: BookRepository = OfflineBookRepository(
        contentSources = listOf(localBookPackageDataSource, assetBookDataSource),
    )
    val bookPackageInstaller: LocalBookPackageInstaller = LocalBookPackageInstaller(
        packageStorage = localBookPackageStorage,
    )
    val remoteContentCatalogSource: DefaultRemoteContentCatalogSource = DefaultRemoteContentCatalogSource(
        context = application,
        packageStorage = localBookPackageStorage,
    )
    val bookPackageDownloader: HttpBookPackageDownloader = HttpBookPackageDownloader(
        packageStorage = localBookPackageStorage,
    )
    val bookPackageSyncManager: BookPackageSyncManager = BookPackageSyncManager(
        bookRepository = bookRepository,
        packageStorage = localBookPackageStorage,
        packageInstaller = bookPackageInstaller,
        remoteContentCatalogSource = remoteContentCatalogSource,
        bookPackageDownloader = bookPackageDownloader,
    )
    val readingProgressRepository: ReadingProgressRepository = RoomReadingProgressRepository(
        database.readingProgressDao(),
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
