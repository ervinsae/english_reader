package com.ervinzhang.englishreader.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

interface AudioPlayer {
    fun play(assetPath: String)
    fun speak(text: String)
    fun stop()
}

object NoOpAudioPlayer : AudioPlayer {
    override fun play(assetPath: String) = Unit
    override fun speak(text: String) = Unit
    override fun stop() = Unit
}

class AndroidAudioPlayer(
    context: Context,
) : AudioPlayer, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeech: String? = null

    override fun play(assetPath: String) {
        stopMediaPlayback()
        textToSpeech?.stop()

        val cachedAudioFile = cacheAssetToFile(assetPath) ?: return

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setDataSource(cachedAudioFile.absolutePath)
            setOnPreparedListener { it.start() }
            setOnCompletionListener { completedPlayer ->
                completedPlayer.release()
                if (mediaPlayer === completedPlayer) {
                    mediaPlayer = null
                }
            }
            setOnErrorListener { failedPlayer, _, _ ->
                failedPlayer.release()
                if (mediaPlayer === failedPlayer) {
                    mediaPlayer = null
                }
                true
            }
            prepareAsync()
        }
    }

    override fun speak(text: String) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return

        stopMediaPlayback()

        val tts = ensureTextToSpeech()
        if (!isTtsReady) {
            pendingSpeech = normalizedText
            return
        }

        pendingSpeech = null
        tts.stop()
        tts.speak(
            normalizedText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "storybook-tts-${SystemClock.uptimeMillis()}",
        )
    }

    override fun stop() {
        stopMediaPlayback()
        textToSpeech?.stop()
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        val tts = textToSpeech ?: return
        val selectedLocale = chooseAvailableLocale(tts) ?: return
        tts.language = selectedLocale
        tts.setSpeechRate(0.95f)
        tts.setPitch(1.0f)
        isTtsReady = true
        pendingSpeech?.let { queuedText ->
            pendingSpeech = null
            speak(queuedText)
        }
    }

    private fun chooseAvailableLocale(tts: TextToSpeech): Locale? {
        val preferredLocales = listOf(
            Locale.US,
            Locale.UK,
            Locale.ENGLISH,
            Locale.getDefault(),
        ).distinct()

        return preferredLocales.firstOrNull { locale ->
            val result = tts.isLanguageAvailable(locale)
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun ensureTextToSpeech(): TextToSpeech {
        return textToSpeech ?: TextToSpeech(appContext, this).also { created ->
            textToSpeech = created
        }
    }

    private fun cacheAssetToFile(assetPath: String): File? {
        val targetDirectory = File(appContext.cacheDir, "storybook-audio").apply { mkdirs() }
        val fileName = assetPath.replace('/', '_')
        val targetFile = File(targetDirectory, fileName)

        return runCatching {
            if (!targetFile.exists() || targetFile.length() == 0L) {
                appContext.assets.open(assetPath).use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            targetFile
        }.getOrNull()
    }

    private fun stopMediaPlayback() {
        mediaPlayer?.runCatching {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
