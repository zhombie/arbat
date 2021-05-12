package kz.zhombie.cinema

import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
import android.view.*
import androidx.core.os.HandlerCompat
import androidx.fragment.app.FragmentManager
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import kz.zhombie.cinema.logging.Logger
import kz.zhombie.cinema.model.Movie
import kz.zhombie.cinema.model.Params
import kz.zhombie.cinema.ui.ViewHolder
import kz.zhombie.cinema.ui.base.BaseDialogFragment

class CinemaDialogFragment private constructor(
) : BaseDialogFragment(R.layout.cinema_fragment_dialog), CinemaDialogFragmentListener {

    companion object {
        private val TAG: String = CinemaDialogFragment::class.java.simpleName

        fun init(isLoggingEnabled: Boolean) {
            Settings.setLoggingEnabled(isLoggingEnabled)
        }

        private fun newInstance(params: Params): CinemaDialogFragment {
            val fragment = CinemaDialogFragment()
            fragment.arguments = BundleManager.build(params)
            return fragment
        }
    }

    class Builder {
        private var movie: Movie? = null
        private var screenView: View? = null
        private var isFooterViewEnabled: Boolean = false
        private var callback: Callback? = null

        fun setMovie(movie: Movie): Builder {
            this.movie = movie
            return this
        }

        fun setScreenView(screenView: View): Builder {
            this.screenView = screenView
            return this
        }

        fun setFooterViewEnabled(isEnabled: Boolean): Builder {
            this.isFooterViewEnabled = isEnabled
            return this
        }

        fun setCallback(callback: Callback): Builder {
            this.callback = callback
            return this
        }

        fun build(): CinemaDialogFragment {
            return newInstance(
                Params(
                    movie = requireNotNull(movie) { "Cinema movie is mandatory value" },
                    isFooterViewEnabled = isFooterViewEnabled
                )
            ).apply {
                setScreenView(this@Builder.screenView)

                setCallback(this@Builder.callback)
            }
        }

        fun show(fragmentManager: FragmentManager): CinemaDialogFragment {
            val fragment = build()
            fragment.isCancelable = true
            fragment.show(fragmentManager, null)
            return fragment
        }
    }

    private val handler by lazy { HandlerCompat.createAsync(Looper.getMainLooper()) }

    // -------------------------------------------------

    private var screenView: View? = null

    private var params: Params? = null

    // -------------------------------------------------

    private var viewHolder: ViewHolder? = null

    private var player: SimpleExoPlayer? = null

    private var viewsTransitionAnimator: ViewsTransitionAnimator<Any>? = null

    private var controllerViewAnimation: ViewPropertyAnimator? = null

    private var isMovieShowCalled: Boolean = false

    private var isOverlayViewVisible: Boolean = true

    // -------------------------------------------------

    private var callback: Callback? = null

    private fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    override fun getTheme(): Int = R.style.Cinema_Dialog_Fullscreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        params = BundleManager.parse(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewHolder = ViewHolder(view)

        setupToolbar()
        setupBackgroundView()
        setupGestureFrameLayout()
        setupViewTransitionAnimator()
        setupInfo()
        setupPlayer()
        setupControllerView()
        setupFooterView()

        if (savedInstanceState == null) {
            viewsTransitionAnimator?.enterSingle(true)
        }

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(params?.movie?.uri)
                .setMimeType(MimeTypes.BASE_TYPE_VIDEO)
                .build()

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15 * 1000)
                .setReadTimeoutMs(15 * 1000)

            val mediaSource = DefaultMediaSourceFactory(requireContext())
                .setDrmHttpDataSourceFactory(httpDataSourceFactory)
                .createMediaSource(mediaItem)

            player?.setMediaSource(mediaSource)
            player?.prepare()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            eventListener.onPlayerError(ExoPlaybackException.createForUnexpected(e))
        }
    }

    override fun onPause() {
        super.onPause()
        viewHolder?.playerView?.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        viewHolder?.playerView?.onPause()
        releasePlayer()
    }

    override fun onCancel(dialog: DialogInterface) {
        dismiss()
    }

    override fun dismiss() {
        Logger.debug(TAG, "dismiss()")
        Logger.debug(TAG, "dismiss() -> ${viewsTransitionAnimator?.isLeaving}")

        if (viewsTransitionAnimator?.isLeaving == false) {
            if (screenView == null) {
                viewsTransitionAnimator?.exit(false)
            } else {
                viewsTransitionAnimator?.exit(true)
            }
        } else {
            super.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Logger.debug(TAG, "onDestroy()")

        if (!isMovieShowCalled) {
            isMovieShowCalled = true

            Logger.debug(TAG, "onDestroy() -> isMovieShowCalled: $isMovieShowCalled")

            screenView?.visibility = View.VISIBLE
        }

        controllerViewAnimation?.cancel()
        controllerViewAnimation = null

        viewsTransitionAnimator = null

        screenView = null

        viewHolder = null

        params = null

        callback?.onDestroy()
        callback = null
    }

    private fun setupToolbar() {
        viewHolder?.toolbar?.setNavigationOnClickListener { dismiss() }
    }

    private fun setupBackgroundView() {
        viewHolder?.backgroundView?.visibility = View.VISIBLE
    }

    private fun setupViewTransitionAnimator() = viewHolder?.let { viewHolder ->
        viewsTransitionAnimator = when {
            screenView != null -> {
                GestureTransitions.from<Any>(screenView!!)
                    .into(viewHolder.gestureFrameLayout)
            }
            else -> {
                GestureTransitions.fromNone<Any>()
                    .into(viewHolder.gestureFrameLayout)
            }
        }

        // Setting up and animating image transition
        viewsTransitionAnimator?.addPositionUpdateListener(::applyFullViewPagerState)
    }

    private fun setupGestureFrameLayout() {
        // Settings
        viewHolder?.gestureFrameLayout?.controller?.settings
            ?.setAnimationsDuration(225L)
            ?.setBoundsType(com.alexvasilkov.gestures.Settings.Bounds.NORMAL)
            ?.setDoubleTapEnabled(true)
            ?.setExitEnabled(true)
            ?.setExitType(com.alexvasilkov.gestures.Settings.ExitType.SCROLL)
            ?.setFillViewport(true)
            ?.setFitMethod(com.alexvasilkov.gestures.Settings.Fit.INSIDE)
            ?.setFlingEnabled(true)
            ?.setGravity(Gravity.CENTER)
            ?.setMaxZoom(2.5F)
            ?.setMinZoom(0F)
            ?.setPanEnabled(true)
            ?.isZoomEnabled = true

        // Click actions
        viewHolder?.gestureFrameLayout?.setOnClickListener {
            if (isOverlayViewVisible) {
                viewHolder?.toolbar?.animate()
                    ?.alpha(0.0F)
                    ?.setDuration(50L)
                    ?.withEndAction {
                        viewHolder?.toolbar?.visibility = View.INVISIBLE
                    }
                    ?.start()

                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                viewHolder?.controllerView?.visibility = View.INVISIBLE

                if (params?.isFooterViewEnabled == true) {
                    viewHolder?.footerView?.animate()
                        ?.alpha(0.0F)
                        ?.setDuration(50L)
                        ?.withEndAction {
                            viewHolder?.footerView?.visibility = View.INVISIBLE
                        }
                        ?.start()
                }

                isOverlayViewVisible = false
            } else {
                viewHolder?.toolbar?.animate()
                    ?.alpha(1.0F)
                    ?.setDuration(50L)
                    ?.withStartAction {
                        viewHolder?.toolbar?.visibility = View.VISIBLE
                    }
                    ?.start()

                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                viewHolder?.controllerView?.visibility = View.VISIBLE

                if (params?.isFooterViewEnabled == true) {
                    viewHolder?.footerView?.animate()
                        ?.alpha(1.0F)
                        ?.setDuration(50L)
                        ?.withStartAction {
                            viewHolder?.footerView?.visibility = View.VISIBLE
                        }
                        ?.start()
                }

                isOverlayViewVisible = true
            }
        }
    }

    private fun setupInfo() {
        viewHolder?.titleView?.text = params?.movie?.info?.title

        val subtitle = params?.movie?.info?.subtitle
        if (subtitle.isNullOrBlank()) {
            viewHolder?.subtitleView?.visibility = View.GONE
        } else {
            viewHolder?.subtitleView?.text = subtitle
            viewHolder?.subtitleView?.visibility = View.VISIBLE
        }
    }

    private fun setupPlayer() {
        if (player == null) {
            player = SimpleExoPlayer.Builder(requireContext())
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            viewHolder?.playerView?.player = player
            viewHolder?.playerView?.setShowPreviousButton(false)
            viewHolder?.playerView?.setShowNextButton(false)
            viewHolder?.playerView?.setShowRewindButton(false)
            viewHolder?.playerView?.setShowRewindButton(false)
            viewHolder?.playerView?.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            viewHolder?.playerView?.setShowFastForwardButton(false)
            viewHolder?.playerView?.setUseSensorRotation(false)
            viewHolder?.playerView?.useController = false
            viewHolder?.playerView?.controllerAutoShow = false

            player?.playWhenReady = true
            player?.pauseAtEndOfMediaItems = true
            player?.addListener(eventListener)
            player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
            player?.setWakeMode(C.WAKE_MODE_NONE)
        }
    }

    private fun setupControllerView() {
        viewHolder?.playOrPauseButton?.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
            } else {
                player?.play()
            }
        }
    }

    private fun setupFooterView() {
        if (params?.isFooterViewEnabled == true) {
            viewHolder?.footerView?.visibility = View.VISIBLE
        } else {
            viewHolder?.footerView?.visibility = View.GONE
        }
    }

    /**
     * Applying [View] image animation state: fading out toolbar, title and background.
     */
    private fun applyFullViewPagerState(position: Float, isLeaving: Boolean) = viewHolder?.let { viewHolder ->
        val isFinished = position == 0F && isLeaving

        viewHolder.toolbar.alpha = position
        viewHolder.backgroundView.alpha = position
        viewHolder.controllerView.alpha = position

        if (params?.isFooterViewEnabled == true) {
            viewHolder.footerView.alpha = position
        }

        if (isLeaving) {
            viewHolder.controllerView.visibility = View.INVISIBLE
        } else {
            viewHolder.controllerView.visibility = View.VISIBLE
        }

        if (isFinished) {
            viewHolder.toolbar.visibility = View.INVISIBLE
            viewHolder.backgroundView.visibility = View.INVISIBLE

            if (params?.isFooterViewEnabled == true) {
                viewHolder.footerView.visibility = View.INVISIBLE
            }
        } else {
            viewHolder.toolbar.visibility = View.VISIBLE
            viewHolder.backgroundView.visibility = View.VISIBLE

            if (params?.isFooterViewEnabled == true) {
                viewHolder.footerView.visibility = View.VISIBLE
            }
        }

        viewHolder.gestureFrameLayout.visibility = if (isFinished) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        if (isFinished) {
            if (!isMovieShowCalled) {
                isMovieShowCalled = true

                Logger.debug(TAG, "applyFullViewPagerState() -> isMovieShowCalled: $isMovieShowCalled")

                screenView?.visibility = View.VISIBLE
            }

            handler.postDelayed({ dismiss() }, 25L)
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
                Logger.debug(TAG, "onIsLoadingChanged() -> isLoading: $isLoading")
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    player?.seekTo(0)

                    viewHolder?.toolbar?.alpha = 1.0F
                    viewHolder?.toolbar?.visibility = View.VISIBLE

                    viewHolder?.controllerView?.alpha = 1.0F
                    viewHolder?.controllerView?.visibility = View.VISIBLE

                    if (params?.isFooterViewEnabled == true) {
                        viewHolder?.footerView?.alpha = 1.0F
                        viewHolder?.footerView?.visibility = View.VISIBLE
                    }
                }

                if (state == Player.STATE_BUFFERING) {
                    viewHolder?.playOrPauseButton?.visibility = View.INVISIBLE
                    viewHolder?.progressIndicator?.visibility = View.VISIBLE
                } else {
                    viewHolder?.progressIndicator?.visibility = View.INVISIBLE
                    viewHolder?.playOrPauseButton?.visibility = View.VISIBLE
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
                if (isPlaying) {
                    viewHolder?.playOrPauseButton?.setIconResource(R.drawable.exo_icon_pause)

                    controllerViewAnimation = viewHolder?.controllerView?.animate()
                        ?.setStartDelay(2500L)
                        ?.withStartAction {
                            viewHolder?.controllerView?.visibility = View.VISIBLE
                        }
                        ?.withEndAction {
                            viewHolder?.controllerView?.visibility = View.INVISIBLE
                        }
                    controllerViewAnimation?.start()
                } else {
                    viewHolder?.playOrPauseButton?.setIconResource(R.drawable.exo_icon_play)
                }
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
                error.printStackTrace()
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

    /**
     * [CinemaDialogFragmentListener] implementation
     */

    override fun setScreenView(view: View?): CinemaDialogFragment {
        this.screenView = view
        return this
    }

    interface Callback {
        fun onDestroy()
    }

}