package com.ervinzhang.englishreader.core.content

import android.content.Context
import com.ervinzhang.englishreader.core.model.Book
import com.ervinzhang.englishreader.core.model.BookPage
import com.ervinzhang.englishreader.core.model.PageWordRef
import com.ervinzhang.englishreader.core.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AssetBookDataSource(
    private val context: Context,
) {
    suspend fun loadBookContents(): List<BookContent> = withContext(Dispatchers.IO) {
        val bookDirectories = context.assets.list(BOOKS_ROOT).orEmpty().sorted()

        bookDirectories.mapNotNull { directoryName ->
            runCatching { loadBookContent(directoryName) }.getOrNull()
        }
    }

    private fun loadBookContent(directoryName: String): BookContent {
        val baseAssetPath = "$BOOKS_ROOT/$directoryName"
        val bookJson = readJsonObject("$baseAssetPath/$BOOK_FILE_NAME")
        val words = parseWords(
            bookFolder = directoryName,
            wordsJson = readJsonArray("$baseAssetPath/$WORDS_FILE_NAME"),
        )
        val pages = parsePages(
            bookFolder = directoryName,
            defaultBookId = bookJson.optString(BOOK_ID_KEY).ifBlank { directoryName },
            pagesJson = readJsonArray("$baseAssetPath/$PAGES_FILE_NAME"),
        ).sortedBy { it.pageNo }

        val book = parseBook(
            directoryName = directoryName,
            bookJson = bookJson,
            fallbackPageCount = pages.size,
        )

        return BookContent(
            book = book,
            pages = pages,
            words = words.associateBy { it.id },
        )
    }

    private fun parseBook(
        directoryName: String,
        bookJson: JSONObject,
        fallbackPageCount: Int,
    ): Book {
        val pageCount = bookJson.optInt(PAGE_COUNT_KEY, fallbackPageCount).takeIf { it > 0 }
            ?: fallbackPageCount

        return Book(
            id = bookJson.optString(BOOK_ID_KEY).ifBlank { directoryName },
            title = bookJson.getString(TITLE_KEY),
            level = bookJson.optString(LEVEL_KEY).ifBlank { "L1" },
            coverAsset = resolveAssetPath(
                directoryName,
                bookJson.optString(COVER_ASSET_KEY).ifBlank { DEFAULT_COVER_ASSET },
            ),
            pageCount = pageCount,
            tags = bookJson.optJSONArray(TAGS_KEY)?.toStringList().orEmpty(),
            enabled = bookJson.optBoolean(ENABLED_KEY, true),
        )
    }

    private fun parsePages(
        bookFolder: String,
        defaultBookId: String,
        pagesJson: JSONArray,
    ): List<BookPage> {
        return buildList {
            for (index in 0 until pagesJson.length()) {
                val pageJson = pagesJson.getJSONObject(index)
                add(
                    BookPage(
                        bookId = pageJson.optString(BOOK_ID_KEY).ifBlank { defaultBookId },
                        pageNo = pageJson.getInt(PAGE_NO_KEY),
                        imageAsset = resolveAssetPath(bookFolder, pageJson.getString(IMAGE_ASSET_KEY)),
                        englishText = pageJson.getString(ENGLISH_TEXT_KEY),
                        sentenceAudioAsset = pageJson.optStringOrNull(SENTENCE_AUDIO_ASSET_KEY)
                            ?.let { resolveAssetPath(bookFolder, it) },
                        words = pageJson.optJSONArray(WORDS_KEY)?.toPageWordRefs().orEmpty(),
                    ),
                )
            }
        }
    }

    private fun parseWords(
        bookFolder: String,
        wordsJson: JSONArray,
    ): List<Word> {
        return buildList {
            for (index in 0 until wordsJson.length()) {
                val wordJson = wordsJson.getJSONObject(index)
                add(
                    Word(
                        id = wordJson.getString(ID_KEY),
                        text = wordJson.getString(TEXT_KEY),
                        meaningZh = wordJson.getString(MEANING_ZH_KEY),
                        phonetic = wordJson.optStringOrNull(PHONETIC_KEY),
                        audioAsset = wordJson.optStringOrNull(AUDIO_ASSET_KEY)
                            ?.let { resolveAssetPath(bookFolder, it) },
                    ),
                )
            }
        }
    }

    private fun JSONArray.toPageWordRefs(): List<PageWordRef> {
        return buildList {
            for (index in 0 until length()) {
                val wordRefJson = getJSONObject(index)
                add(
                    PageWordRef(
                        wordId = wordRefJson.getString(WORD_ID_KEY),
                        text = wordRefJson.getString(TEXT_KEY),
                        startIndex = wordRefJson.getInt(START_INDEX_KEY),
                        endIndex = wordRefJson.getInt(END_INDEX_KEY),
                    ),
                )
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> {
        return buildList {
            for (index in 0 until length()) {
                add(getString(index))
            }
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun resolveAssetPath(bookFolder: String, rawPath: String): String {
        val normalizedPath = rawPath.trim().removePrefix("/")
        if (normalizedPath.startsWith("$BOOKS_ROOT/")) {
            return normalizedPath
        }
        return "$BOOKS_ROOT/$bookFolder/$normalizedPath"
    }

    private fun readJsonObject(assetPath: String): JSONObject = JSONObject(readAsset(assetPath))

    private fun readJsonArray(assetPath: String): JSONArray = JSONArray(readAsset(assetPath))

    private fun readAsset(assetPath: String): String {
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val BOOKS_ROOT = "books"
        const val BOOK_FILE_NAME = "book.json"
        const val PAGES_FILE_NAME = "pages.json"
        const val WORDS_FILE_NAME = "words.json"
        const val DEFAULT_COVER_ASSET = "cover.png"

        const val ID_KEY = "id"
        const val BOOK_ID_KEY = "id"
        const val TITLE_KEY = "title"
        const val LEVEL_KEY = "level"
        const val COVER_ASSET_KEY = "coverAsset"
        const val PAGE_COUNT_KEY = "pageCount"
        const val TAGS_KEY = "tags"
        const val ENABLED_KEY = "enabled"

        const val PAGE_NO_KEY = "pageNo"
        const val IMAGE_ASSET_KEY = "imageAsset"
        const val ENGLISH_TEXT_KEY = "englishText"
        const val SENTENCE_AUDIO_ASSET_KEY = "sentenceAudioAsset"
        const val WORDS_KEY = "words"

        const val WORD_ID_KEY = "wordId"
        const val TEXT_KEY = "text"
        const val START_INDEX_KEY = "startIndex"
        const val END_INDEX_KEY = "endIndex"
        const val MEANING_ZH_KEY = "meaningZh"
        const val PHONETIC_KEY = "phonetic"
        const val AUDIO_ASSET_KEY = "audioAsset"
    }
}
