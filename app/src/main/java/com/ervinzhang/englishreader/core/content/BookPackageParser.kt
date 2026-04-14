package com.ervinzhang.englishreader.core.content

import com.ervinzhang.englishreader.core.model.Book
import com.ervinzhang.englishreader.core.model.BookPage
import com.ervinzhang.englishreader.core.model.PageWordRef
import com.ervinzhang.englishreader.core.model.Word
import org.json.JSONArray
import org.json.JSONObject

internal class BookPackageParser {
    fun parseBookContent(
        fallbackBookId: String,
        bookJson: JSONObject,
        pagesJson: JSONArray,
        wordsJson: JSONArray,
        resolveContentUri: (String) -> String,
    ): BookContent {
        val words = parseWords(
            wordsJson = wordsJson,
            resolveContentUri = resolveContentUri,
        )
        val bookId = bookJson.optString(BOOK_ID_KEY).ifBlank { fallbackBookId }
        val pages = parsePages(
            defaultBookId = bookId,
            pagesJson = pagesJson,
            resolveContentUri = resolveContentUri,
        ).sortedBy { it.pageNo }
        val book = parseBook(
            fallbackBookId = fallbackBookId,
            bookJson = bookJson,
            fallbackPageCount = pages.size,
            resolveContentUri = resolveContentUri,
        )

        return BookContent(
            book = book,
            pages = pages,
            words = words.associateBy { it.id },
        )
    }

    private fun parseBook(
        fallbackBookId: String,
        bookJson: JSONObject,
        fallbackPageCount: Int,
        resolveContentUri: (String) -> String,
    ): Book {
        val pageCount = bookJson.optInt(PAGE_COUNT_KEY, fallbackPageCount).takeIf { it > 0 }
            ?: fallbackPageCount

        return Book(
            id = bookJson.optString(BOOK_ID_KEY).ifBlank { fallbackBookId },
            title = bookJson.getString(TITLE_KEY),
            level = bookJson.optString(LEVEL_KEY).ifBlank { "L1" },
            coverUri = resolveContentUri(
                bookJson.optStringOrNull(COVER_URI_KEY)
                    ?: bookJson.optString(COVER_ASSET_KEY).ifBlank { DEFAULT_COVER_ASSET },
            ),
            pageCount = pageCount,
            tags = bookJson.optJSONArray(TAGS_KEY)?.toStringList().orEmpty(),
            enabled = bookJson.optBoolean(ENABLED_KEY, true),
        )
    }

    private fun parsePages(
        defaultBookId: String,
        pagesJson: JSONArray,
        resolveContentUri: (String) -> String,
    ): List<BookPage> {
        return buildList {
            for (index in 0 until pagesJson.length()) {
                val pageJson = pagesJson.getJSONObject(index)
                add(
                    BookPage(
                        bookId = pageJson.optString(PAGE_BOOK_ID_KEY).ifBlank { defaultBookId },
                        pageNo = pageJson.getInt(PAGE_NO_KEY),
                        imageUri = resolveContentUri(
                            pageJson.optStringOrNull(IMAGE_URI_KEY)
                                ?: pageJson.getString(IMAGE_ASSET_KEY),
                        ),
                        englishText = pageJson.getString(ENGLISH_TEXT_KEY),
                        sentenceAudioUri = (
                            pageJson.optStringOrNull(SENTENCE_AUDIO_URI_KEY)
                                ?: pageJson.optStringOrNull(SENTENCE_AUDIO_ASSET_KEY)
                            )
                            ?.let(resolveContentUri),
                        words = pageJson.optJSONArray(WORDS_KEY)?.toPageWordRefs().orEmpty(),
                    ),
                )
            }
        }
    }

    private fun parseWords(
        wordsJson: JSONArray,
        resolveContentUri: (String) -> String,
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
                        audioUri = (
                            wordJson.optStringOrNull(AUDIO_URI_KEY)
                                ?: wordJson.optStringOrNull(AUDIO_ASSET_KEY)
                            )
                            ?.let(resolveContentUri),
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

    companion object {
        const val BOOK_FILE_NAME = "book.json"
        const val PAGES_FILE_NAME = "pages.json"
        const val WORDS_FILE_NAME = "words.json"
        const val DEFAULT_COVER_ASSET = "cover.png"

        private const val ID_KEY = "id"
        private const val BOOK_ID_KEY = "id"
        private const val TITLE_KEY = "title"
        private const val LEVEL_KEY = "level"
        private const val COVER_URI_KEY = "coverUri"
        private const val COVER_ASSET_KEY = "coverAsset"
        private const val PAGE_COUNT_KEY = "pageCount"
        private const val TAGS_KEY = "tags"
        private const val ENABLED_KEY = "enabled"

        private const val PAGE_BOOK_ID_KEY = "bookId"
        private const val PAGE_NO_KEY = "pageNo"
        private const val IMAGE_URI_KEY = "imageUri"
        private const val IMAGE_ASSET_KEY = "imageAsset"
        private const val ENGLISH_TEXT_KEY = "englishText"
        private const val SENTENCE_AUDIO_URI_KEY = "sentenceAudioUri"
        private const val SENTENCE_AUDIO_ASSET_KEY = "sentenceAudioAsset"
        private const val WORDS_KEY = "words"

        private const val WORD_ID_KEY = "wordId"
        private const val TEXT_KEY = "text"
        private const val START_INDEX_KEY = "startIndex"
        private const val END_INDEX_KEY = "endIndex"
        private const val MEANING_ZH_KEY = "meaningZh"
        private const val PHONETIC_KEY = "phonetic"
        private const val AUDIO_URI_KEY = "audioUri"
        private const val AUDIO_ASSET_KEY = "audioAsset"
    }
}
