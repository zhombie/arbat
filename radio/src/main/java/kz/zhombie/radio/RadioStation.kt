package kz.zhombie.radio

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
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

    override fun start(uri: String, playWhenReady: Boolean): Radio {
        start(uri = Uri.parse(uri), playWhenReady = playWhenReady)
        return this
    }

    override fun start(uri: Uri, playWhenReady: Boolean): Radio {
        setupPlayer(playWhenReady = playWhenReady)
        setUri(uri = uri)
        return this
    }

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun playOrPause() {
        togglePlayOrPause()
    }

    override fun release() {
        releasePlayer()
    }

    private fun setupPlayer(playWhenReady: Boolean): SimpleExoPlayer? {
        return if (player == null) {
            player = SimpleExoPlayer.Builder(context)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            player?.playWhenReady = playWhenReady
            player?.pauseAtEndOfMediaItems = true
            player?.addListener(eventListener)
            player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
            player?.setWakeMode(C.WAKE_MODE_NONE)
            player
        } else {
            player
        }
    }

    private fun setUri(uri: Uri) {
        if (player == null) {
            Log.w(TAG, "Player is not initialized!")
        } else {
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.BASE_TYPE_AUDIO)
                .build()

            val httpDataSourceFactory = DefaultHttpDataSourceFactory(
                ExoPlayerLibraryInfo.DEFAULT_USER_AGENT,
                20 * 1000,
                20 * 1000,
                true
            )

            val mediaSource = DefaultMediaSourceFactory(context)
                .setDrmHttpDataSourceFactory(httpDataSourceFactory)
                .createMediaSource(mediaItem)

            player?.setMediaSource(mediaSource)
            player?.prepare()
        }
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
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listener?.onPlayingStateChanged(isPlaying)
            }

            override fun onPlaybackStateChanged(state: Int) {
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

            override fun onPlayerError(error: ExoPlaybackException) {
                error.printStackTrace()
            }
        }
    }

}