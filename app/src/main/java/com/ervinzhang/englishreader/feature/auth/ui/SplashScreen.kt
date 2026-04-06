package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ervinzhang.englishreader.core.datastore.SessionStore
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    sessionStore: SessionStore,
    onLoggedIn: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (sessionStore.isLoggedIn.first()) onLoggedIn() else onLoggedOut()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
