package kz.zhombie.radio

import android.content.Context
import android.net.Uri

interface Radio {
    class Builder constructor(private val context: Context) {
        fun create(listener: Listener? = null): Radio {
            return RadioStation.create(context, listener)
        }
    }

    fun start(uri: String, playWhenReady: Boolean = true): Radio
    fun start(uri: Uri, playWhenReady: Boolean = true): Radio

    fun play()
    fun pause()
    fun playOrPause()

    fun release()

    enum class PlaybackState {
        IDLE,
        BUFFERING,
        READY,
        ENDED
    }

    interface Listener {
        fun onPlayingStateChanged(isPlaying: Boolean)
        fun onPlaybackStateChanged(state: PlaybackState)
    }
}


