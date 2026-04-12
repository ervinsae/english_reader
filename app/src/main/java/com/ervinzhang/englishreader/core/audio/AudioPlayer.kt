package com.ervinzhang.englishreader.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.io.ByteArrayInputStream
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

        val audioBytes = runCatching {
            appContext.assets.open(assetPath).use { input -> input.readBytes() }
        }.getOrNull() ?: return

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setDataSource(ByteArrayMediaDataSource(audioBytes))
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
        val languageResult = tts.setLanguage(Locale.US)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            return
        }

        tts.setSpeechRate(0.95f)
        isTtsReady = true
        pendingSpeech?.let { queuedText ->
            pendingSpeech = null
            speak(queuedText)
        }
    }

    private fun ensureTextToSpeech(): TextToSpeech {
        return textToSpeech ?: TextToSpeech(appContext, this).also { created ->
            textToSpeech = created
        }
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

private class ByteArrayMediaDataSource(
    data: ByteArray,
) : MediaDataSource() {
    private val input = ByteArrayInputStream(data)
    private val bytes = data

    override fun getSize(): Long = bytes.size.toLong()

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= bytes.size) return -1

        input.reset()
        input.skip(position)
        val bytesToRead = minOf(size, bytes.size - position.toInt())
        return input.read(buffer, offset, bytesToRead)
    }

    override fun close() = Unit
}
