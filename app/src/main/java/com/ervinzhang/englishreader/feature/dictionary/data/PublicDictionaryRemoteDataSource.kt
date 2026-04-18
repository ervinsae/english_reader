package com.ervinzhang.englishreader.feature.dictionary.data

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PublicDictionaryRemoteDataSource : DictionaryRemoteDataSource {
    override suspend fun lookup(word: String, normalizedWord: String): DictionaryWordDetails? {
        return withContext(Dispatchers.IO) {
            val dictionaryEntry = fetchDictionaryEntry(normalizedWord)
            val translatedWordMeaning = fetchChineseTranslation(word)
            val translatedDefinitionMeaning = if (translatedWordMeaning == null) {
                dictionaryEntry?.englishMeaning?.let(::fetchChineseTranslation)
            } else {
                null
            }

            val meaning = translatedWordMeaning
                ?: translatedDefinitionMeaning
                ?: dictionaryEntry?.englishMeaning
                ?: return@withContext null

            DictionaryWordDetails(
                meaningZh = meaning,
                phonetic = dictionaryEntry?.phonetic,
                audioUri = dictionaryEntry?.audioUri,
                source = buildSource(
                    usedDictionary = dictionaryEntry != null,
                    usedTranslation = translatedWordMeaning != null || translatedDefinitionMeaning != null,
                ),
            )
        }
    }

    private fun fetchDictionaryEntry(normalizedWord: String): ParsedDictionaryEntry? {
        val url = "$DICTIONARY_API_BASE_URL/${encodeQuery(normalizedWord)}"
        val response = requestJsonObjectOrArray(url) ?: return null
        val entries = when (response) {
            is JSONArray -> response
            is JSONObject -> JSONArray().put(response)
            else -> return null
        }
        if (entries.length() == 0) return null

        var phonetic: String? = null
        var audioUri: String? = null
        val englishMeanings = mutableListOf<String>()

        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            if (phonetic.isNullOrBlank()) {
                phonetic = entry.optString(PHONETIC_KEY).trim().takeIf { it.isNotBlank() }
                    ?: parsePhoneticFromPhonetics(entry.optJSONArray(PHONETICS_KEY))
            }
            if (audioUri.isNullOrBlank()) {
                audioUri = parseAudioUri(entry.optJSONArray(PHONETICS_KEY))
            }
            collectMeaningSegments(
                meaningsJson = entry.optJSONArray(MEANINGS_KEY),
                into = englishMeanings,
            )
            if (phonetic != null && audioUri != null && englishMeanings.size >= MAX_MEANING_SEGMENTS) {
                break
            }
        }

        if (phonetic.isNullOrBlank() && audioUri.isNullOrBlank() && englishMeanings.isEmpty()) {
            return null
        }

        return ParsedDictionaryEntry(
            phonetic = phonetic,
            audioUri = audioUri,
            englishMeaning = englishMeanings
                .distinct()
                .take(MAX_MEANING_SEGMENTS)
                .joinToString(separator = "；")
                .trim()
                .takeIf { it.isNotBlank() },
        )
    }

    private fun fetchChineseTranslation(text: String): String? {
        val candidate = text.trim()
        if (candidate.isBlank()) return null

        val url = buildString {
            append(TRANSLATION_API_BASE_URL)
            append("?q=")
            append(encodeQuery(candidate))
            append("&langpair=en|zh-CN")
        }
        val response = requestJsonObjectOrArray(url) as? JSONObject ?: return null
        val translatedText = response
            .optJSONObject(RESPONSE_DATA_KEY)
            ?.optString(TRANSLATED_TEXT_KEY)
            ?.trim()
            .orEmpty()
            .takeIf { it.isNotBlank() }
            ?: return null

        return translatedText.takeIf { translated ->
            !translated.equals(candidate, ignoreCase = true) &&
                !translated.equals("null", ignoreCase = true)
        }
    }

    private fun collectMeaningSegments(
        meaningsJson: JSONArray?,
        into: MutableList<String>,
    ) {
        if (meaningsJson == null) return

        for (meaningIndex in 0 until meaningsJson.length()) {
            if (into.size >= MAX_MEANING_SEGMENTS) return

            val meaningJson = meaningsJson.optJSONObject(meaningIndex) ?: continue
            val definition = meaningJson
                .optJSONArray(DEFINITIONS_KEY)
                ?.optJSONObject(0)
                ?.optString(DEFINITION_KEY)
                ?.trim()
                .orEmpty()
            if (definition.isBlank()) continue

            val partOfSpeech = meaningJson.optString(PART_OF_SPEECH_KEY).trim()
            into += if (partOfSpeech.isBlank()) definition else "$partOfSpeech: $definition"
        }
    }

    private fun parsePhoneticFromPhonetics(phoneticsJson: JSONArray?): String? {
        if (phoneticsJson == null) return null
        for (index in 0 until phoneticsJson.length()) {
            val phoneticsItem = phoneticsJson.optJSONObject(index) ?: continue
            val text = phoneticsItem.optString(TEXT_KEY).trim()
            if (text.isNotBlank()) return text
        }
        return null
    }

    private fun parseAudioUri(phoneticsJson: JSONArray?): String? {
        if (phoneticsJson == null) return null
        for (index in 0 until phoneticsJson.length()) {
            val phoneticsItem = phoneticsJson.optJSONObject(index) ?: continue
            val rawAudio = phoneticsItem.optString(AUDIO_KEY).trim()
            if (rawAudio.isBlank()) continue
            return normalizeAudioUrl(rawAudio)
        }
        return null
    }

    private fun requestJsonObjectOrArray(url: String): Any? {
        val connection = runCatching {
            URL(url).openConnection() as HttpURLConnection
        }.getOrNull() ?: return null

        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) return null

            val body = connection.inputStream.bufferedReader().use { it.readText() }.trim()
            when {
                body.startsWith("[") -> JSONArray(body)
                body.startsWith("{") -> JSONObject(body)
                else -> null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun encodeQuery(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun normalizeAudioUrl(rawAudio: String): String {
        return when {
            rawAudio.startsWith("//") -> "https:$rawAudio"
            rawAudio.startsWith("http://") -> "https://${rawAudio.removePrefix("http://")}"
            else -> rawAudio
        }
    }

    private fun buildSource(
        usedDictionary: Boolean,
        usedTranslation: Boolean,
    ): String {
        return when {
            usedDictionary && usedTranslation -> "dictionaryapi+translation"
            usedDictionary -> "dictionaryapi"
            usedTranslation -> "translation"
            else -> "unknown"
        }
    }

    private data class ParsedDictionaryEntry(
        val phonetic: String?,
        val audioUri: String?,
        val englishMeaning: String?,
    )

    private companion object {
        const val DICTIONARY_API_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en"
        const val TRANSLATION_API_BASE_URL = "https://api.mymemory.translated.net/get"
        const val NETWORK_TIMEOUT_MS = 15_000
        const val USER_AGENT = "EnglishReader/0.1"
        const val MAX_MEANING_SEGMENTS = 2

        const val PHONETIC_KEY = "phonetic"
        const val PHONETICS_KEY = "phonetics"
        const val MEANINGS_KEY = "meanings"
        const val DEFINITIONS_KEY = "definitions"
        const val DEFINITION_KEY = "definition"
        const val PART_OF_SPEECH_KEY = "partOfSpeech"
        const val TEXT_KEY = "text"
        const val AUDIO_KEY = "audio"
        const val RESPONSE_DATA_KEY = "responseData"
        const val TRANSLATED_TEXT_KEY = "translatedText"
    }
}
