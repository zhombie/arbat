package kz.zhombie.cinema

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.os.HandlerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import kz.zhombie.cinema.exoplayer.AbstractPlayerListener
import kz.zhombie.cinema.logging.Logger
import kz.zhombie.cinema.model.Movie
import kz.zhombie.cinema.model.Params
import kz.zhombie.cinema.ui.UIViewHolder
import kz.zhombie.cinema.ui.base.BaseDialogFragment

class CinemaDialogFragment private constructor(
) : BaseDialogFragment(R.layout.cinema_fragment_dialog), CinemaDialogFragmentDelegate {

    companion object {
        private val TAG: String = CinemaDialogFragment::class.java.simpleName

        private fun newInstance(params: Params): CinemaDialogFragment {
            val fragment = CinemaDialogFragment()
            fragment.arguments = BundleManager.build(params)
            return fragment
        }
    }

    class Builder {
        private var tag: String? = null
        private var movie: Movie? = null
        private var screenView: View? = null
        private var isFooterViewEnabled: Boolean = false
        private var callback: Callback? = null

        fun setTag(tag: String): Builder {
            this.tag = tag
            return this
        }

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
            fragment.show(fragmentManager, tag)
            return fragment
        }

        fun dismissPrevious(fragmentManager: FragmentManager): Fragment? {
            val fragment = fragmentManager.findFragmentByTag(TAG)
            if (fragment is CinemaDialogFragment) {
                fragment.dismiss()
            }
            return fragment
        }

        fun showSafely(fragmentManager: FragmentManager): CinemaDialogFragment {
            dismissPrevious(fragmentManager)
            setTag(TAG)
            return show(fragmentManager)
        }
    }

    private val handler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())

    // -------------------------------------------------

    private var screenView: View? = null

    private var params: Params? = null

    // -------------------------------------------------

    private var uiViewHolder: UIViewHolder? = null

    private var exoPlayer: ExoPlayer? = null

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

        val configuration = Cinema.getConfiguration(requireContext())
        Logger.debug(TAG, "configuration: $configuration")

        params = BundleManager.parse(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiViewHolder = UIViewHolder(view)

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

            val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
            drmSessionManagerProvider.setDrmHttpDataSourceFactory(httpDataSourceFactory)

            val mediaSource = DefaultMediaSourceFactory(requireContext())
                .setDrmSessionManagerProvider(drmSessionManagerProvider)
                .createMediaSource(mediaItem)

            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            eventListener.onPlayerError(
                ExoPlaybackException
                    .createForUnexpected(e, ExoPlaybackException.TYPE_UNEXPECTED)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        uiViewHolder?.playerView?.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        uiViewHolder?.playerView?.onPause()
        releasePlayer()
    }

    override fun onCancel(dialog: DialogInterface) {
        dismiss()
    }

    override fun dismiss() {
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

        uiViewHolder = null

        params = null

        callback?.onDestroy()
        callback = null
    }

    private fun setupToolbar() {
        uiViewHolder?.toolbar?.setNavigationOnClickListener { dismiss() }
    }

    private fun setupBackgroundView() {
        uiViewHolder?.backgroundView?.visibility = View.VISIBLE
    }

    private fun setupViewTransitionAnimator() = uiViewHolder?.let { viewHolder ->
        viewsTransitionAnimator = if (screenView == null) {
            GestureTransitions.fromNone<Any>()
                .into(viewHolder.gestureFrameLayout)
        } else {
            screenView?.let {
                GestureTransitions.from<Any>(it)
                    .into(viewHolder.gestureFrameLayout)
            }
        }

        // Setting up and animating image transition
        viewsTransitionAnimator?.addPositionUpdateListener(::applyFullViewPagerState)
    }

    private fun setupGestureFrameLayout() {
        // Settings
        uiViewHolder?.gestureFrameLayout?.controller?.settings
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
        uiViewHolder?.gestureFrameLayout?.setOnClickListener {
            if (isOverlayViewVisible) {
                uiViewHolder?.toolbar?.animate()
                    ?.alpha(0.0F)
                    ?.setDuration(50L)
                    ?.withEndAction {
                        uiViewHolder?.toolbar?.visibility = View.INVISIBLE
                    }
                    ?.start()

                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                uiViewHolder?.controllerView?.visibility = View.INVISIBLE

                if (params?.isFooterViewEnabled == true) {
                    uiViewHolder?.footerView?.animate()
                        ?.alpha(0.0F)
                        ?.setDuration(50L)
                        ?.withEndAction {
                            uiViewHolder?.footerView?.visibility = View.INVISIBLE
                        }
                        ?.start()
                }

                isOverlayViewVisible = false
            } else {
                uiViewHolder?.toolbar?.animate()
                    ?.alpha(1.0F)
                    ?.setDuration(50L)
                    ?.withStartAction {
                        uiViewHolder?.toolbar?.visibility = View.VISIBLE
                    }
                    ?.start()

                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                uiViewHolder?.controllerView?.visibility = View.VISIBLE

                if (params?.isFooterViewEnabled == true) {
                    uiViewHolder?.footerView?.animate()
                        ?.alpha(1.0F)
                        ?.setDuration(50L)
                        ?.withStartAction {
                            uiViewHolder?.footerView?.visibility = View.VISIBLE
                        }
                        ?.start()
                }

                isOverlayViewVisible = true
            }
        }
    }

    private fun setupInfo() {
        uiViewHolder?.titleView?.text = params?.movie?.info?.title

        val subtitle = params?.movie?.info?.subtitle
        if (subtitle.isNullOrBlank()) {
            uiViewHolder?.subtitleView?.visibility = View.GONE
        } else {
            uiViewHolder?.subtitleView?.text = subtitle
            uiViewHolder?.subtitleView?.visibility = View.VISIBLE
        }
    }

    private fun setupPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(requireContext())
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            uiViewHolder?.playerView?.player = exoPlayer
            uiViewHolder?.playerView?.setShowPreviousButton(false)
            uiViewHolder?.playerView?.setShowNextButton(false)
            uiViewHolder?.playerView?.setShowRewindButton(false)
            uiViewHolder?.playerView?.setShowRewindButton(false)
            uiViewHolder?.playerView?.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            uiViewHolder?.playerView?.setShowFastForwardButton(false)
            uiViewHolder?.playerView?.setShowShuffleButton(false)
            uiViewHolder?.playerView?.useController = false
            uiViewHolder?.playerView?.controllerAutoShow = false

            exoPlayer?.playWhenReady = true
            exoPlayer?.pauseAtEndOfMediaItems = true
            exoPlayer?.addListener(eventListener)
            exoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_OFF
            exoPlayer?.setWakeMode(C.WAKE_MODE_NONE)
        }
    }

    private fun setupControllerView() {
        uiViewHolder?.playOrPauseButton?.setOnClickListener {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            } else {
                exoPlayer?.play()
            }
        }
    }

    private fun setupFooterView() {
        if (params?.isFooterViewEnabled == true) {
            uiViewHolder?.footerView?.visibility = View.VISIBLE
        } else {
            uiViewHolder?.footerView?.visibility = View.GONE
        }
    }

    /**
     * Applying [View] image animation state: fading out toolbar, title and background.
     */
    private fun applyFullViewPagerState(position: Float, isLeaving: Boolean) = uiViewHolder?.let { viewHolder ->
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
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                if (state == Player.STATE_ENDED) {
                    exoPlayer?.seekTo(0)

                    uiViewHolder?.toolbar?.alpha = 1.0F
                    uiViewHolder?.toolbar?.visibility = View.VISIBLE

                    uiViewHolder?.controllerView?.alpha = 1.0F
                    uiViewHolder?.controllerView?.visibility = View.VISIBLE

                    if (params?.isFooterViewEnabled == true) {
                        uiViewHolder?.footerView?.alpha = 1.0F
                        uiViewHolder?.footerView?.visibility = View.VISIBLE
                    }
                }

                if (state == Player.STATE_BUFFERING) {
                    uiViewHolder?.playOrPauseButton?.visibility = View.INVISIBLE
                    uiViewHolder?.progressIndicator?.visibility = View.VISIBLE
                } else {
                    uiViewHolder?.progressIndicator?.visibility = View.INVISIBLE
                    uiViewHolder?.playOrPauseButton?.visibility = View.VISIBLE
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                if (isPlaying) {
                    uiViewHolder?.playOrPauseButton?.setIconResource(R.drawable.cinema_ic_pause)

                    controllerViewAnimation = uiViewHolder?.controllerView?.animate()
                        ?.setStartDelay(2500L)
                        ?.withStartAction {
                            uiViewHolder?.controllerView?.visibility = View.VISIBLE
                        }
                        ?.withEndAction {
                            uiViewHolder?.controllerView?.visibility = View.INVISIBLE
                        }
                    controllerViewAnimation?.start()
                } else {
                    uiViewHolder?.playOrPauseButton?.setIconResource(R.drawable.cinema_ic_play)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                error.printStackTrace()
            }
        }
    }

    /**
     * [CinemaDialogFragmentDelegate] implementation
     */

    override fun setScreenView(view: View?): CinemaDialogFragment {
        this.screenView = view
        return this
    }

    interface Callback {
        fun onDestroy()
    }

}