package com.ervinzhang.englishreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.ervinzhang.englishreader.app.AppNavHost
import com.ervinzhang.englishreader.core.ui.theme.EnglishReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as EnglishReaderApp

        setContent {
            EnglishReaderTheme {
                Surface {
                    AppNavHost(appContainer = app.appContainer)
                }
            }
        }
    }
}
