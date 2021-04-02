package kz.zhombie.radio

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleObserver

interface Radio : LifecycleObserver {
    companion object {
        fun init(isLoggingEnabled: Boolean) {
            Settings.setLoggingEnabled(isLoggingEnabled)
        }
    }

    class Builder constructor(private val context: Context) {
        fun create(listener: Listener? = null): Radio {
            return RadioStation.create(context, listener)
        }
    }

    // Instance control
    fun start(uri: String, playWhenReady: Boolean = true): Radio
    fun start(uri: Uri, playWhenReady: Boolean = true): Radio

    fun isReleased(): Boolean
    fun release()

    // Media control
    fun play()
    fun pause()
    fun playOrPause()

    fun seekTo(position: Long)  // milliseconds

    // Information
    fun getCurrentSource(): Uri?

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
        fun onPlaybackPositionChanged(position: Long)
        fun onPlayerError(cause: Throwable?)
    }
}


