package kz.zhombie.cinema

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import com.alexvasilkov.gestures.animation.ViewPosition
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView

class CinemaDialogFragment private constructor() : BaseDialogFragment(R.layout.cinema_fragment_dialog),
    CinemaDialogFragmentListener {

    companion object {
        private val TAG: String = CinemaDialogFragment::class.java.simpleName

        private fun newInstance(
            uri: Uri,
            title: String? = null,
            subtitle: String? = null,
            startViewPosition: ViewPosition,
            isFooterViewEnabled: Boolean
        ): CinemaDialogFragment {
            val fragment = CinemaDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(BundleKey.URI, uri.toString())
                putString(BundleKey.TITLE, title)
                if (!subtitle.isNullOrBlank()) putString(BundleKey.SUBTITLE, subtitle)
                putString(BundleKey.START_VIEW_POSITION, startViewPosition.pack())
                putBoolean(BundleKey.IS_FOOTER_VIEW_ENABLED, isFooterViewEnabled)
            }
            return fragment
        }
    }

    class Builder {
        private var screenView: View? = null
        private var uri: Uri? = null
        private var title: String? = null
        private var subtitle: String? = null
        private var viewPosition: ViewPosition? = null
        private var isFooterViewEnabled: Boolean = false
        private var callback: Callback? = null

        fun setScreenView(screenView: View): Builder {
            this.screenView = screenView
            return this
        }

        fun setUri(uri: Uri): Builder {
            this.uri = uri
            return this
        }

        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setSubtitle(subtitle: String?): Builder {
            this.subtitle = subtitle
            return this
        }

        fun setStartViewPosition(view: View): Builder {
            this.viewPosition = ViewPosition.from(view)
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
                uri = requireNotNull(uri) { "Cinema movie uri is mandatory value" },
                title = title,
                subtitle = subtitle,
                startViewPosition = requireNotNull(viewPosition) {
                    "Cinema movie needs start view position, in order to make smooth transition animation"
                },
                isFooterViewEnabled = isFooterViewEnabled
            ).apply {
                this@Builder.screenView?.let { setScreenView(it) }

                this@Builder.callback?.let { setCallback(it) }
            }
        }

        fun show(fragmentManager: FragmentManager): CinemaDialogFragment {
            val fragment = build()
//            val transaction = fragmentManager.beginTransaction()
//            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//            transaction
//                .add(android.R.id.content, fragment)
//                .commit()
            fragment.isCancelable = true
            fragment.show(fragmentManager, null)
            return fragment
        }
    }

    private object BundleKey {
        const val URI = "uri"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val START_VIEW_POSITION = "start_view_position"
        const val IS_FOOTER_VIEW_ENABLED = "is_footer_view_enabled"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var backgroundView: View
    private lateinit var gestureFrameLayout: GestureFrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var controllerView: FrameLayout
    private lateinit var playOrPauseButton: MaterialButton
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var footerView: LinearLayout
    private lateinit var titleView: MaterialTextView
    private lateinit var subtitleView: MaterialTextView

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private var isMovieShowCalled: Boolean = false

    private var isOverlayViewVisible: Boolean = true

    private var screenView: View? = null
        set(value) {
            field = value

            if (value != null) {
                if (onGlobalLayoutListener == null) {
                    onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                        onTrackViewPosition(value)
                    }

                    if (value.viewTreeObserver.isAlive) {
                        value.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
                    }
                }
            }
        }

    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var player: SimpleExoPlayer? = null

    private lateinit var uri: Uri
    private var title: String? = null
    private var subtitle: String? = null
    private lateinit var startViewPosition: ViewPosition
    private var isFooterViewEnabled: Boolean = false

    private var controllerViewAnimation: ViewPropertyAnimator? = null

    override fun getTheme(): Int {
        return R.style.Cinema_Dialog_Fullscreen
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, theme)

        val arguments = arguments
        require(arguments != null) { "Provide arguments!" }
        uri = Uri.parse(requireNotNull(arguments.getString(BundleKey.URI)))
        title = arguments.getString(BundleKey.TITLE)
        subtitle = arguments.getString(BundleKey.SUBTITLE)
        startViewPosition = ViewPosition.unpack(arguments.getString(BundleKey.START_VIEW_POSITION))
        isFooterViewEnabled = arguments.getBoolean(BundleKey.IS_FOOTER_VIEW_ENABLED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar = view.findViewById(R.id.toolbar)
        backgroundView = view.findViewById(R.id.backgroundView)
        gestureFrameLayout = view.findViewById(R.id.gestureFrameLayout)
        playerView = view.findViewById(R.id.playerView)
        controllerView = view.findViewById(R.id.controllerView)
        playOrPauseButton = view.findViewById(R.id.playOrPauseButton)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        footerView = view.findViewById(R.id.footerView)
        titleView = view.findViewById(R.id.titleView)
        subtitleView = view.findViewById(R.id.subtitleView)

        setupToolbar()
        setupBackgroundView()
        setupGestureFrameLayout()
        setupInfo()
        setupPlayer()
        setupControllerView()
        setupFooterView()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(MimeTypes.BASE_TYPE_VIDEO)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSourceFactory(
            ExoPlayerLibraryInfo.DEFAULT_USER_AGENT,
            20 * 1000,
            20 * 1000,
            true
        )

        val mediaSource = DefaultMediaSourceFactory(requireContext())
            .setDrmHttpDataSourceFactory(httpDataSourceFactory)
            .createMediaSource(mediaItem)

        player?.setMediaSource(mediaSource)
        player?.prepare()

        gestureFrameLayout.positionAnimator.addPositionUpdateListener { position, isLeaving ->
            val isFinished = position == 0F && isLeaving

            toolbar.alpha = position
            backgroundView.alpha = position
            controllerView.alpha = position

            if (isFooterViewEnabled) {
                footerView.alpha = position
            }

            if (isLeaving) {
                controllerView.visibility = View.INVISIBLE
            } else {
                controllerView.visibility = View.VISIBLE
            }

            if (isFinished) {
                toolbar.visibility = View.INVISIBLE
                backgroundView.visibility = View.INVISIBLE

                if (isFooterViewEnabled) {
                    footerView.visibility = View.INVISIBLE
                }
            } else {
                toolbar.visibility = View.VISIBLE
                backgroundView.visibility = View.VISIBLE

                if (isFooterViewEnabled) {
                    footerView.visibility = View.VISIBLE
                }
            }

            gestureFrameLayout.visibility = if (isFinished) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            if (isFinished) {
                if (!isMovieShowCalled) {
                    callback?.onMovieShow(0L)
                    isMovieShowCalled = true
                }

                gestureFrameLayout.controller.settings.disableBounds()
                gestureFrameLayout.positionAnimator.setState(0F, false, false)

                gestureFrameLayout.postDelayed({ super.dismiss() }, 17L)
            }
        }

        gestureFrameLayout.positionAnimator.enter(startViewPosition, savedInstanceState == null)

        gestureFrameLayout.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                gestureFrameLayout.viewTreeObserver.removeOnPreDrawListener(this)
                callback?.onMovieHide(17L)
                return true
            }
        })
        gestureFrameLayout.invalidate()
    }

    override fun onPause() {
        super.onPause()
        playerView.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        playerView.onPause()
        releasePlayer()
    }

    override fun onCancel(dialog: DialogInterface) {
        dismiss()
    }

    override fun dismiss() {
        if (!gestureFrameLayout.positionAnimator.isLeaving) {
            gestureFrameLayout.positionAnimator.exit(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isMovieShowCalled) {
            callback?.onMovieShow(0L)
            isMovieShowCalled = true
        }

        if (screenView?.viewTreeObserver?.isAlive == true) {
            screenView?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }

        onGlobalLayoutListener = null
        screenView = null

        callback = null
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun setupBackgroundView() {
        backgroundView.visibility = View.VISIBLE
    }

    private fun setupGestureFrameLayout() {
        // Settings
        gestureFrameLayout.controller.settings
            .setAnimationsDuration(225L)
            .setBoundsType(com.alexvasilkov.gestures.Settings.Bounds.NORMAL)
            .setDoubleTapEnabled(false)
            .setExitEnabled(true)
            .setExitType(com.alexvasilkov.gestures.Settings.ExitType.SCROLL)
            .setFillViewport(true)
            .setFitMethod(com.alexvasilkov.gestures.Settings.Fit.INSIDE)
            .setFlingEnabled(true)
            .setGravity(Gravity.CENTER)
            .setMaxZoom(2.5F)
            .setMinZoom(0F)
            .setPanEnabled(true)
            .setZoomEnabled(true)

        // Click actions
        gestureFrameLayout.setOnClickListener {
            if (isOverlayViewVisible) {
                toolbar.animate()
                    .alpha(0.0F)
                    .setDuration(50L)
                    .withEndAction {
                        toolbar.visibility = View.INVISIBLE
                    }
                    .start()

                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                controllerView.visibility = View.INVISIBLE

                if (isFooterViewEnabled) {
                    footerView.animate()
                        .alpha(0.0F)
                        .setDuration(50L)
                        .withEndAction {
                            footerView.visibility = View.INVISIBLE
                        }
                        .start()
                }

                isOverlayViewVisible = false
            } else {
                toolbar.animate()
                    .alpha(1.0F)
                    .setDuration(50L)
                    .withStartAction {
                        toolbar.visibility = View.VISIBLE
                    }
                    .start()

                controllerViewAnimation?.cancel()
                controllerViewAnimation = null

                controllerView.visibility = View.VISIBLE

                if (isFooterViewEnabled) {
                    footerView.animate()
                        .alpha(1.0F)
                        .setDuration(50L)
                        .withStartAction {
                            footerView.visibility = View.VISIBLE
                        }
                        .start()
                }

                isOverlayViewVisible = true
            }
        }
    }

    private fun setupInfo() {
        titleView.text = title

        if (subtitle.isNullOrBlank()) {
            subtitleView.visibility = View.GONE
        } else {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        }
    }

    private fun setupPlayer() {
        if (player == null) {
            player = SimpleExoPlayer.Builder(requireContext())
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .build()

            playerView.player = player
            playerView.setShowPreviousButton(false)
            playerView.setShowNextButton(false)
            playerView.setShowRewindButton(false)
            playerView.setShowRewindButton(false)
            playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            playerView.setShowFastForwardButton(false)
            playerView.setUseSensorRotation(false)
            playerView.useController = false
            playerView.controllerAutoShow = false

            player?.playWhenReady = true
            player?.pauseAtEndOfMediaItems = true
            player?.addListener(eventListener)
            player?.repeatMode = SimpleExoPlayer.REPEAT_MODE_OFF
            player?.setWakeMode(C.WAKE_MODE_NONE)
        }
    }

    private fun setupControllerView() {
        playOrPauseButton.setOnClickListener {
            if (player?.isPlaying == true) {
                player?.pause()
            } else {
                player?.play()
            }
        }
    }

    private fun setupFooterView() {
        if (isFooterViewEnabled) {
            footerView.visibility = View.VISIBLE
        } else {
            footerView.visibility = View.GONE
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
                if (isPlaying) {
                    playOrPauseButton.setIconResource(R.drawable.exo_icon_pause)

                    controllerViewAnimation = controllerView.animate()
                        .setStartDelay(2500L)
                        .withStartAction {
                            controllerView.visibility = View.VISIBLE
                        }
                        .withEndAction {
                            controllerView.visibility = View.INVISIBLE
                        }
                    controllerViewAnimation?.start()
                } else {
                    playOrPauseButton.setIconResource(R.drawable.exo_icon_play)
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    player?.seekTo(0)

                    toolbar.alpha = 1.0F
                    toolbar.visibility = View.VISIBLE

                    controllerView.alpha = 1.0F
                    controllerView.visibility = View.VISIBLE

                    if (isFooterViewEnabled) {
                        footerView.alpha = 1.0F
                        footerView.visibility = View.VISIBLE
                    }
                }

                if (state == Player.STATE_BUFFERING) {
                    playOrPauseButton.visibility = View.INVISIBLE
                    progressIndicator.visibility = View.VISIBLE
                } else {
                    progressIndicator.visibility = View.INVISIBLE
                    playOrPauseButton.visibility = View.VISIBLE
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                error.printStackTrace()
            }
        }
    }

    /**
     * [CinemaDialogFragmentListener] implementation
     */

    override fun onTrackViewPosition(view: View) {
        onTrackViewPosition(ViewPosition.from(view))
    }

    override fun onTrackViewPosition(viewPosition: ViewPosition) {
        if (this::gestureFrameLayout.isInitialized) {
            if (gestureFrameLayout.positionAnimator.position > 0f) {
                gestureFrameLayout.positionAnimator.update(viewPosition)
            }
        }
    }

    override fun setScreenView(view: View): CinemaDialogFragment {
        this.screenView = view
        return this
    }

    interface Callback {
        fun onMovieShow(delay: Long = 0L)
        fun onMovieHide(delay: Long = 0L)
    }

}