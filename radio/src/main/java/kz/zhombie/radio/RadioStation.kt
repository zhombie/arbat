package kz.zhombie.radio

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.os.HandlerCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import kz.zhombie.radio.exoplayer.AbstractPlayerListener
import kz.zhombie.radio.logging.Logger

internal class RadioStation private constructor(
    private val context: Context,
    private val listener: Radio.Listener?
) : Radio {

    companion object {
        private val TAG = RadioStation::class.java.simpleName

        fun create(context: Context, listener: Radio.Listener? = null): Radio {
            return RadioStation(context, listener)
        }
    }

    private var exoPlayer: ExoPlayer? = null

    private val handler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())

    /**
     * [Radio] implementation
     */

    override fun start(uri: String, playWhenReady: Boolean): Radio {
        start(uri = Uri.parse(uri), playWhenReady = playWhenReady)
        return this
    }

    override fun start(uri: Uri, playWhenReady: Boolean): Radio {
        setupPlayer(playWhenReady = playWhenReady)
        setUri(uri = uri)
        return this
    }

    override fun isReleased(): Boolean {
        return exoPlayer == null
    }

    override fun release() {
        releasePlayer()
    }

    /**
     * [RemoteControl] implementation
     */

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun playOrPause() {
        togglePlayOrPause()
    }

    override fun stop(reset: Boolean) {
        exoPlayer?.stop()

        if (reset) {
            exoPlayer?.clearMediaItems()
        }
    }

    override fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }

    /**
     * [TrackInformation] implementation
     */

    override val isSourceLoading: Boolean
        get() = exoPlayer?.isLoading == true

    override val isPlaying: Boolean
        get() = exoPlayer?.isPlaying == true

    override val currentSource: Uri?
        get() = exoPlayer?.currentMediaItem?.localConfiguration?.uri

    override val duration: Long
        get() = exoPlayer?.duration ?: -1

    override val currentPosition: Long
        get() = exoPlayer?.currentPosition ?: -1

    override val currentPercentage: Float
        get() = if (duration > -1 && currentPosition > -1) {
            currentPosition * 100F / duration
        } else {
            0.0F
        }

    override val bufferedPosition: Long
        get() = exoPlayer?.bufferedPosition ?: -1

    override val bufferedPercentage: Int
        get() = exoPlayer?.bufferedPercentage ?: -1

    override val totalBufferedDuration: Long
        get() = exoPlayer?.totalBufferedDuration ?: -1

    /**
     * Internal methods
     */

    private fun setupPlayer(playWhenReady: Boolean): ExoPlayer? {
        return if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            exoPlayer?.playWhenReady = playWhenReady
            exoPlayer?.pauseAtEndOfMediaItems = true
            exoPlayer?.setHandleAudioBecomingNoisy(true)
            exoPlayer?.addListener(eventListener)
            exoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_OFF
            exoPlayer?.setWakeMode(C.WAKE_MODE_NONE)
            exoPlayer
        } else {
            exoPlayer
        }
    }

    private fun setUri(uri: Uri) = try {
        if (exoPlayer == null) {
            Log.w(TAG, "Player is not initialized!")
        } else {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            }

            if ((exoPlayer?.mediaItemCount ?: 0) > 0) {
                exoPlayer?.clearMediaItems()
            }

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .build()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15 * 1000)
                .setReadTimeoutMs(15 * 1000)

            val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
            drmSessionManagerProvider.setDrmHttpDataSourceFactory(httpDataSourceFactory)

            val mediaSource = DefaultMediaSourceFactory(context)
                .setDrmSessionManagerProvider(drmSessionManagerProvider)
                .createMediaSource(mediaItem)

            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        listener?.onPlayerError(e)
    }

    private fun togglePlayOrPause() {
        if (exoPlayer == null) {
            Log.w(TAG, "Player is not initialized!")
        } else {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            } else {
                exoPlayer?.play()
            }
        }
    }

    private fun releasePlayer() {
        exoPlayer?.clearMediaItems()
        exoPlayer?.removeListener(eventListener)
        exoPlayer?.release()
        exoPlayer = null
    }

    /**
     * [Player.Listener] implementation
     */

    private val eventListener by lazy {
        object : AbstractPlayerListener() {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
                listener?.onIsSourceLoadingChanged(isLoading)
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                when (state) {
                    Player.STATE_IDLE ->
                        listener?.onPlaybackStateChanged(Radio.PlaybackState.IDLE)
                    Player.STATE_BUFFERING ->
                        listener?.onPlaybackStateChanged(Radio.PlaybackState.BUFFERING)
                    Player.STATE_READY ->
                        listener?.onPlaybackStateChanged(Radio.PlaybackState.READY)
                    Player.STATE_ENDED -> {
                        exoPlayer?.seekTo(0)
                        listener?.onPlaybackStateChanged(Radio.PlaybackState.ENDED)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                updateCurrentPlayerPosition()
                listener?.onIsPlayingStateChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                listener?.onPlayerError(error.cause)
            }
        }
    }

    private fun updateCurrentPlayerPosition() {
        val position = currentPosition
        if (position > -1) {
            listener?.onPlaybackPositionChanged(position)
        }

        if (exoPlayer?.isPlaying == true) {
            HandlerCompat.postDelayed(
                handler,
                this::updateCurrentPlayerPosition,
                "timer",
                500L
            )
        } else {
            handler.removeCallbacksAndMessages("timer")
        }
    }

    /**
     * [androidx.lifecycle.DefaultLifecycleObserver] implementation
     */

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Logger.debug(TAG, "onResume()")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Logger.debug(TAG, "onDestroy()")
        release()
    }

}