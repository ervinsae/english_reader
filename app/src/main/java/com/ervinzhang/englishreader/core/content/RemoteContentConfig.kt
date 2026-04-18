package com.ervinzhang.englishreader.core.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class RemoteContentConfig(
    val catalogUrl: String? = null,
    val bookshelfUrl: String? = null,
    val autoSyncOnLaunch: Boolean = false,
)

interface RemoteContentConfigSource {
    suspend fun loadConfig(): RemoteContentConfig
}

class DefaultRemoteContentConfigSource(
    context: Context,
    private val packageStorage: LocalBookPackageStorage,
) : RemoteContentConfigSource {
    private val appContext = context.applicationContext

    override suspend fun loadConfig(): RemoteContentConfig = withContext(Dispatchers.IO) {
        val overrideFile = packageStorage.configFile(CATALOG_CONFIG_FILE_NAME)
        val rawConfig = when {
            overrideFile.isFile -> overrideFile.readText()
            else -> appContext.assets.open("$ASSET_CONTENT_DIRECTORY/$CATALOG_CONFIG_FILE_NAME")
                .bufferedReader()
                .use { it.readText() }
        }
        val json = JSONObject(rawConfig)
        RemoteContentConfig(
            catalogUrl = json.optString(CATALOG_URL_KEY).takeIf { it.isNotBlank() },
            bookshelfUrl = json.optString(BOOKSHELF_URL_KEY).takeIf { it.isNotBlank() },
            autoSyncOnLaunch = json.optBoolean(AUTO_SYNC_ON_LAUNCH_KEY, false),
        )
    }

    companion object {
        const val ASSET_CONTENT_DIRECTORY = "content"
        const val CATALOG_CONFIG_FILE_NAME = "catalog-config.json"
        const val CATALOG_URL_KEY = "catalogUrl"
        const val BOOKSHELF_URL_KEY = "bookshelfUrl"
        const val AUTO_SYNC_ON_LAUNCH_KEY = "autoSyncOnLaunch"
    }
}
