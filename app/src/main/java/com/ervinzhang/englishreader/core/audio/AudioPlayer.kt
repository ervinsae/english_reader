package com.ervinzhang.englishreader.core.audio

interface AudioPlayer {
    fun play(assetPath: String)
    fun stop()
}

object NoOpAudioPlayer : AudioPlayer {
    override fun play(assetPath: String) = Unit
    override fun stop() = Unit
}
