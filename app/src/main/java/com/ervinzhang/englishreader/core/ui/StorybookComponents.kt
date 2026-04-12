package com.ervinzhang.englishreader.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StorybookBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(180.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f)),
        )
        content()
    }
}

@Composable
fun StorybookCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.shadow(
            elevation = 18.dp,
            shape = MaterialTheme.shapes.large,
            ambientColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
            spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
        ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        content()
    }
}

@Composable
fun StorybookPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
fun StorybookTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun ProgressCaterpillar(
    progress: Int,
    modifier: Modifier = Modifier,
    segmentCount: Int = 6,
) {
    val activeSegments = ((progress.coerceIn(0, 100) / 100f) * segmentCount)
        .toInt()
        .coerceAtLeast(if (progress > 0) 1 else 0)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(segmentCount) { index ->
            val isActive = index < activeSegments
            Box(
                modifier = Modifier
                    .size(if (index == segmentCount - 1) 18.dp else 14.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        },
                    ),
            )
        }
    }
}

@Composable
fun StorybookSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
