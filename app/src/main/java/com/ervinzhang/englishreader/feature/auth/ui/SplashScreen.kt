package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ervinzhang.englishreader.feature.auth.domain.AppStartDestination
import com.ervinzhang.englishreader.feature.auth.domain.AuthRepository

@Composable
fun SplashScreen(
    authRepository: AuthRepository,
    onLoggedIn: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    LaunchedEffect(Unit) {
        when (authRepository.getStartDestination()) {
            AppStartDestination.BOOKSHELF -> onLoggedIn()
            AppStartDestination.LOGIN -> onLoggedOut()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
