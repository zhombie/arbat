package kz.zhombie.radio

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleObserver

interface Radio : RemoteControl, TrackInformation, LifecycleObserver {
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

    // ----- State -----

    enum class PlaybackState {
        IDLE,
        BUFFERING,
        READY,
        ENDED
    }

    interface Listener {
        fun onIsSourceLoadingChanged(isLoading: Boolean) {}
        fun onPlaybackStateChanged(state: PlaybackState) {}
        fun onIsPlayingStateChanged(isPlaying: Boolean) {}
        fun onPlaybackPositionChanged(position: Long) {}
        fun onPlayerError(cause: Throwable?) {}
    }
}

