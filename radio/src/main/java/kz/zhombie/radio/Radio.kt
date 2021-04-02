package kz.zhombie.radio

import android.content.Context
import android.net.Uri

interface Radio {
    class Builder constructor(private val context: Context) {
        fun create(listener: Listener? = null): Radio {
            return RadioStation.create(context, listener)
        }
    }

    // Instance control
    fun start(uri: String, playWhenReady: Boolean = true): Radio
    fun start(uri: Uri, playWhenReady: Boolean = true): Radio

    fun release()

    // Media control
    fun play()
    fun pause()
    fun playOrPause()

    fun seekTo(position: Long)  // milliseconds

    // Information
    fun getCurrentPosition(): Long
    fun getDuration(): Long

    fun getBufferedPosition(): Long
    fun getBufferedTotalPosition(): Long
    fun getBufferedPercentage(): Int

    // ----- State -----

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


