package kz.zhombie.museum

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver.*
import android.widget.LinearLayout
import androidx.fragment.app.FragmentManager
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.animation.ViewPosition
import com.alexvasilkov.gestures.views.GestureImageView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView

class MuseumDialogFragment private constructor(
) : BaseDialogFragment(R.layout.museum_fragment_dialog), MuseumDialogFragmentListener {

    companion object {
        private val TAG: String = MuseumDialogFragment::class.java.simpleName

        fun init(artworkLoader: ArtworkLoader, isLoggingEnabled: Boolean) {
            kz.zhombie.museum.Settings.setArtworkLoader(artworkLoader)
            kz.zhombie.museum.Settings.setLoggingEnabled(isLoggingEnabled)
        }

        private fun newInstance(
            uri: Uri,
            title: String? = null,
            subtitle: String? = null,
            startViewPosition: ViewPosition,
            isFooterViewEnabled: Boolean
        ): MuseumDialogFragment {
            val fragment = MuseumDialogFragment()
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
        private var artworkLoader: ArtworkLoader? = null
        private var artworkView: View? = null
        private var uri: Uri? = null
        private var title: String? = null
        private var subtitle: String? = null
        private var viewPosition: ViewPosition? = null
        private var isFooterViewEnabled: Boolean = false
        private var callback: Callback? = null

        fun setArtworkLoader(artworkLoader: ArtworkLoader): Builder {
            this.artworkLoader = artworkLoader
            return this
        }

        fun setArtworkView(artworkView: View): Builder {
            this.artworkView = artworkView
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

        fun build(): MuseumDialogFragment {
            return newInstance(
                uri = requireNotNull(uri) { "Museum artwork uri is mandatory value" },
                title = title,
                subtitle = subtitle,
                startViewPosition = requireNotNull(viewPosition) {
                    "Museum artwork needs start view position, in order to make smooth transition animation"
                },
                isFooterViewEnabled = isFooterViewEnabled
            ).apply {
                this@Builder.artworkView?.let { setArtworkView(it) }

                setArtworkLoader(
                    requireNotNull(
                        this@Builder.artworkLoader ?: kz.zhombie.museum.Settings.getArtworkLoader()
                    ) { "Museum artwork must be loaded somehow" }
                )

                this@Builder.callback?.let { setCallback(it) }
            }
        }

        fun show(fragmentManager: FragmentManager): MuseumDialogFragment {
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
    private lateinit var gestureImageView: GestureImageView
    private lateinit var footerView: LinearLayout
    private lateinit var titleView: MaterialTextView
    private lateinit var subtitleView: MaterialTextView

    private var artworkLoader: ArtworkLoader? = null

    private var callback: Callback? = null

    fun setArtworkLoader(artworkLoader: ArtworkLoader) {
        this.artworkLoader = artworkLoader
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private var isPictureShowCalled: Boolean = false

    private var isOverlayViewVisible: Boolean = true

    private var artworkView: View? = null
        set(value) {
            field = value

            if (value != null) {
                if (onGlobalLayoutListener == null) {
                    onGlobalLayoutListener = OnGlobalLayoutListener {
                        onTrackViewPosition(value)
                    }

                    if (value.viewTreeObserver.isAlive) {
                        value.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
                    }
                }
            }
        }

    private var onGlobalLayoutListener: OnGlobalLayoutListener? = null

    private lateinit var uri: Uri
    private var title: String? = null
    private var subtitle: String? = null
    private lateinit var startViewPosition: ViewPosition
    private var isFooterViewEnabled: Boolean = false

    override fun getTheme(): Int {
        return R.style.Museum_Dialog_Fullscreen
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
        gestureImageView = view.findViewById(R.id.gestureImageView)
        footerView = view.findViewById(R.id.footerView)
        titleView = view.findViewById(R.id.titleView)
        subtitleView = view.findViewById(R.id.subtitleView)

        setupToolbar()
        setupGestureImageView()
        setupInfo()
        setupFooterView()

        artworkLoader?.loadFullscreenImage(requireContext(), gestureImageView, uri)

        gestureImageView.positionAnimator.addPositionUpdateListener { position, isLeaving ->
            val isFinished = position == 0F && isLeaving

            toolbar.alpha = position
            backgroundView.alpha = position

            if (isFooterViewEnabled) {
                footerView.alpha = position
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

            gestureImageView.visibility = if (isFinished) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            if (isFinished) {
                Logger.debug(TAG, "isFinished")

                if (!isPictureShowCalled) {
                    callback?.onPictureShow(0L)
                    isPictureShowCalled = true
                }

                gestureImageView.controller.settings.disableBounds()
                gestureImageView.positionAnimator.setState(0F, false, false)

                gestureImageView.postDelayed({ super.dismiss() }, 17L)
            }
        }

        if (savedInstanceState == null) {
            gestureImageView.positionAnimator.enter(startViewPosition, true)

            gestureImageView.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    gestureImageView.viewTreeObserver.removeOnPreDrawListener(this)
                    callback?.onPictureHide(17L)
                    return true
                }
            })
            gestureImageView.invalidate()
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        Logger.debug(TAG, "onCancel()")

        dismiss()
    }

    override fun dismiss() {
        Logger.debug(TAG, "dismiss()")

        Logger.debug(TAG, "dismiss() -> isLeaving: ${gestureImageView.positionAnimator.isLeaving}")
        if (!gestureImageView.positionAnimator.isLeaving) {
            gestureImageView.positionAnimator.exit(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Logger.debug(TAG, "onDestroy()")

        if (!isPictureShowCalled) {
            callback?.onPictureShow(0L)
            isPictureShowCalled = true
        }

        if (artworkView?.viewTreeObserver?.isAlive == true) {
            artworkView?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }

        onGlobalLayoutListener = null
        artworkView = null

        callback = null
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun setupGestureImageView() {
        // Settings
        gestureImageView.controller.settings
            .setAnimationsDuration(225L)
            .setBoundsType(Settings.Bounds.NORMAL)
            .setDoubleTapEnabled(true)
            .setExitEnabled(true)
            .setExitType(Settings.ExitType.SCROLL)
            .setFillViewport(true)
            .setFitMethod(Settings.Fit.INSIDE)
            .setFlingEnabled(true)
            .setGravity(Gravity.CENTER)
            .setMaxZoom(2.5F)
            .setMinZoom(0F)
            .setPanEnabled(true)
            .setRotationEnabled(true)
            .setRestrictRotation(true)
            .setZoomEnabled(true)

        // Click actions
        gestureImageView.setOnClickListener {
            if (isOverlayViewVisible) {
                toolbar.animate()
                    .alpha(0.0F)
                    .setDuration(100L)
                    .withEndAction {
                        toolbar.visibility = View.INVISIBLE
                    }
                    .start()

                if (isFooterViewEnabled) {
                    footerView.animate()
                        .alpha(0.0F)
                        .setDuration(100L)
                        .withEndAction {
                            footerView.visibility = View.INVISIBLE
                        }
                        .start()
                }

                isOverlayViewVisible = false
            } else {
                toolbar.animate()
                    .alpha(1.0F)
                    .setDuration(100L)
                    .withStartAction {
                        toolbar.visibility = View.VISIBLE
                    }
                    .start()

                if (isFooterViewEnabled) {
                    footerView.animate()
                        .alpha(1.0F)
                        .setDuration(100L)
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

    private fun setupFooterView() {
        if (isFooterViewEnabled) {
            footerView.visibility = View.VISIBLE
        } else {
            footerView.visibility = View.GONE
        }
    }

    /**
     * [MuseumDialogFragmentListener] implementation
     */

    override fun onTrackViewPosition(view: View) {
        onTrackViewPosition(ViewPosition.from(view))
    }

    override fun onTrackViewPosition(viewPosition: ViewPosition) {
        if (this::gestureImageView.isInitialized) {
            if (gestureImageView.positionAnimator.position > 0f) {
                gestureImageView.positionAnimator.update(viewPosition)
            }
        }
    }

    override fun setArtworkView(view: View): MuseumDialogFragment {
        this.artworkView = view
        return this
    }

    interface Callback {
        fun onPictureShow(delay: Long = 0L)
        fun onPictureHide(delay: Long = 0L)
    }

}