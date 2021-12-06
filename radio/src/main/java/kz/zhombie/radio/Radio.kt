package kz.zhombie.radio

import android.content.Context
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver

interface Radio : RemoteControl, TrackInformation, DefaultLifecycleObserver {
    companion object {
        internal const val TAG = "Radio"
    }

    data class Configuration constructor(
        val isLoggingEnabled: Boolean
    )

    interface Factory {
        fun getRadioConfiguration(): Configuration
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

