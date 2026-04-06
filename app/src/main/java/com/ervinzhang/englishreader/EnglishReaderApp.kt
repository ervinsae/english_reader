package com.ervinzhang.englishreader

import android.app.Application
import com.ervinzhang.englishreader.app.AppContainer

class EnglishReaderApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
