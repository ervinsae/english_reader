package com.ervinzhang.englishreader.app

import android.app.Application
import androidx.room.Room
import com.ervinzhang.englishreader.core.audio.AudioPlayer
import com.ervinzhang.englishreader.core.audio.NoOpAudioPlayer
import com.ervinzhang.englishreader.core.content.AssetBookDataSource
import com.ervinzhang.englishreader.core.database.AppDatabase
import com.ervinzhang.englishreader.core.datastore.SessionStore

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

    val audioPlayer: AudioPlayer = NoOpAudioPlayer
}
