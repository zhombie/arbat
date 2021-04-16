package kz.zhombie.radio

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.core.os.HandlerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes

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

    private var player: SimpleExoPlayer? = null

    private val handler by lazy {
        HandlerCompat.createAsync(Looper.getMainLooper())
    }

    @Suppress("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        Logger.debug(TAG, "onResume()")
    }

    @Suppress("unused")
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Logger.debug(TAG, "onDestroy()")
        release()
    }

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
        return player == null
    }

    override fun release() {
        releasePlayer()
    }

    /**
     * [RemoteControl] implementation
     */

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun playOrPause() {
        togglePlayOrPause()
    }

    override fun stop(reset: Boolean) {
        player?.stop()

        if (reset) {
            player?.clearMediaItems()
        }
    }

    override fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    /**
     * [TrackInformation] implementation
     */

    override val isSourceLoading: Boolean
        get() = player?.isLoading == true

    override val isPlaying: Boolean
        get() = player?.isPlaying == true

    override val currentSource: Uri?
        get() = player?.currentMediaItem?.playbackProperties?.uri

    override val duration: Long
        get() = player?.duration ?: -1

    override val currentPosition: Long
        get() = player?.currentPosition ?: -1

    override val currentPercentage: Float
        get() = if (duration > -1 && currentPosition > -1) {
            currentPosition * 100F / duration
        } else {
            0.0F
        }

    override val bufferedPosition: Long
        get() = player?.bufferedPosition ?: -1

    override val bufferedPercentage: Int
        get() = player?.bufferedPercentage ?: -1

    override val totalBufferedDuration: Long
        get() = player?.totalBufferedDuration ?: -1

    /**
     * Internal methods
     */

    private fun setupPlayer(playWhenReady: Boolean): SimpleExoPlayer? {
        return if (player == null) {
            player = SimpleExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            player?.playWhenReady = playWhenReady
            player?.pauseAtEndOfMediaItems = true
            player?.setHandleAudioBecomingNoisy(true)
            player?.addListener(eventListener)
            player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
            player?.setWakeMode(C.WAKE_MODE_NONE)
            player
        } else {
            player
        }
    }

    private fun setUri(uri: Uri) = try {
        if (player == null) {
            Log.w(TAG, "Player is not initialized!")
        } else {
            if (player?.isPlaying == true) {
                player?.pause()
            }

            if ((player?.mediaItemCount ?: 0) > 0) {
                player?.clearMediaItems()
            }

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .build()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15 * 1000)
                .setReadTimeoutMs(15 * 1000)

            val mediaSource = DefaultMediaSourceFactory(context)
                .setDrmHttpDataSourceFactory(httpDataSourceFactory)
                .createMediaSource(mediaItem)

            player?.setMediaSource(mediaSource)
            player?.prepare()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        listener?.onPlayerError(e)
    }

    private fun togglePlayOrPause() {
        if (player == null) {
            Log.w(TAG, "Player is not initialized!")
        } else {
            if (player?.isPlaying == true) {
                player?.pause()
            } else {
                player?.play()
            }
        }
    }

    private fun releasePlayer() {
        player?.clearMediaItems()
        player?.removeListener(eventListener)
        player?.release()
        player = null
    }

    /**
     * [Player.EventListener] implementation
     */

    private val eventListener by lazy {
        object : Player.EventListener {
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                super.onTimelineChanged(timeline, reason)
                Logger.debug(TAG, "onTimelineChanged() -> timeline: $timeline, reason: $reason")
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                Logger.debug(TAG, "onMediaItemTransition() -> mediaItem: $mediaItem, reason: $reason")
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
                super.onTracksChanged(trackGroups, trackSelections)
                Logger.debug(TAG, "onTracksChanged() -> trackGroups: $trackGroups, trackSelections: $trackSelections")
            }

            override fun onStaticMetadataChanged(metadataList: MutableList<Metadata>) {
                super.onStaticMetadataChanged(metadataList)
                Logger.debug(TAG, "onStaticMetadataChanged() -> metadataList: $metadataList")
            }

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
                        player?.seekTo(0)
                        listener?.onPlaybackStateChanged(Radio.PlaybackState.ENDED)
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                Logger.debug(TAG, "onPlayWhenReadyChanged() -> playWhenReady: $playWhenReady, reason: $reason")
            }

            override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                super.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
                Logger.debug(TAG, "onPlaybackSuppressionReasonChanged() -> playbackSuppressionReason: $playbackSuppressionReason")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                Logger.debug(TAG, "onIsPlayingChanged() -> isPlaying: $isPlaying")
                updateCurrentPlayerPosition()
                listener?.onIsPlayingStateChanged(isPlaying)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                Logger.debug(TAG, "onRepeatModeChanged() -> repeatMode: $repeatMode")
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                Logger.debug(TAG, "onShuffleModeEnabledChanged() -> shuffleModeEnabled: $shuffleModeEnabled")
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                Logger.debug(TAG, "onPlayerError() -> error: $error")
                listener?.onPlayerError(error.cause)
            }

            override fun onPositionDiscontinuity(reason: Int) {
                super.onPositionDiscontinuity(reason)
                Logger.debug(TAG, "onPositionDiscontinuity() -> reason: $reason")
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                super.onPlaybackParametersChanged(playbackParameters)
                Logger.debug(TAG, "onPlaybackParametersChanged() -> playbackParameters: $playbackParameters")
            }

            override fun onEvents(player: Player, events: Player.Events) {
                super.onEvents(player, events)
                Logger.debug(TAG, "onEvents() -> events: $events")
            }
        }
    }

    private fun updateCurrentPlayerPosition() {
        val position = currentPosition
        if (position > -1) {
            listener?.onPlaybackPositionChanged(position)
        }

        if (player?.isPlaying == true) {
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

}