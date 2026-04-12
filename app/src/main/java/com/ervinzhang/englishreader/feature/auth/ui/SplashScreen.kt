package com.ervinzhang.englishreader.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ervinzhang.englishreader.core.ui.ProgressCaterpillar
import com.ervinzhang.englishreader.core.ui.StorybookBackdrop
import com.ervinzhang.englishreader.core.ui.StorybookCard
import com.ervinzhang.englishreader.core.ui.StorybookTag
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

    StorybookBackdrop {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            StorybookCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StorybookTag(
                        text = "Digital Pop-Up Book",
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "少儿英语绘本阅读",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "像翻立体书一样，轻轻打开今天的英文故事。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    ProgressCaterpillar(progress = 66)
                    CircularProgressIndicator(
                        modifier = Modifier.size(42.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
}
