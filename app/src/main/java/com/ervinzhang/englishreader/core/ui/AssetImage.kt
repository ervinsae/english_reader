package com.ervinzhang.englishreader.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ervinzhang.englishreader.core.content.ContentUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ContentImage(
    contentUri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    matchAssetAspectRatio: Boolean = false,
    filterQuality: FilterQuality = FilterQuality.Low,
    fallbackText: String = "图片加载失败",
) {
    val context = LocalContext.current
    val imageBitmap = produceState<ImageBitmap?>(initialValue = null, contentUri, context) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ContentUri.open(context, contentUri).use { inputStream ->
                    BitmapFactory.decodeStream(
                        inputStream,
                        null,
                        BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inScaled = false
                        },
                    )?.asImageBitmap()
                }
            }
        }.getOrNull()
    }.value

    if (imageBitmap != null) {
        val resolvedModifier = if (matchAssetAspectRatio && imageBitmap.height > 0) {
            modifier.aspectRatio(imageBitmap.width / imageBitmap.height.toFloat())
        } else {
            modifier
        }
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = resolvedModifier,
            contentScale = contentScale,
            filterQuality = filterQuality,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = fallbackText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
fun AssetImage(
    assetPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    matchAssetAspectRatio: Boolean = false,
    filterQuality: FilterQuality = FilterQuality.Low,
    fallbackText: String = "图片加载失败",
) {
    ContentImage(
        contentUri = assetPath,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        matchAssetAspectRatio = matchAssetAspectRatio,
        filterQuality = filterQuality,
        fallbackText = fallbackText,
    )
}
