package com.ervinzhang.englishreader.app

import android.app.Application
import androidx.room.Room
import com.ervinzhang.englishreader.core.audio.AudioPlayer
import com.ervinzhang.englishreader.core.audio.NoOpAudioPlayer
import com.ervinzhang.englishreader.core.content.AssetBookDataSource
import com.ervinzhang.englishreader.core.content.BookRepository
import com.ervinzhang.englishreader.core.content.OfflineBookRepository
import com.ervinzhang.englishreader.core.database.AppDatabase
import com.ervinzhang.englishreader.core.datastore.SessionStore
import com.ervinzhang.englishreader.core.reading.ReadingProgressRepository
import com.ervinzhang.englishreader.core.reading.RoomReadingProgressRepository
import com.ervinzhang.englishreader.feature.auth.data.AuthRepositoryImpl
import com.ervinzhang.englishreader.feature.auth.data.FakeAuthRemoteDataSource
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository

class AppContainer(
    application: Application,
) {
    val sessionStore: SessionStore = SessionStore(application)

    val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "english_reader.db",
    ).fallbackToDestructiveMigration().build()

    val assetBookDataSource: AssetBookDataSource = AssetBookDataSource(application)
    val bookRepository: BookRepository = OfflineBookRepository(assetBookDataSource)
    val readingProgressRepository: ReadingProgressRepository = RoomReadingProgressRepository(
        database.readingProgressDao(),
    )

    val authRepository: AuthRepository = AuthRepositoryImpl(
        remoteDataSource = FakeAuthRemoteDataSource(database),
        sessionStore = sessionStore,
    )

    val audioPlayer: AudioPlayer = NoOpAudioPlayer
}
