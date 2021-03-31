package kz.zhombie.museum

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.alexvasilkov.gestures.Settings
import com.alexvasilkov.gestures.animation.ViewPosition
import com.alexvasilkov.gestures.views.GestureImageView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView

class MuseumDialogFragment private constructor() : BaseDialogFragment(R.layout.museum_fragment_dialog),
    MuseumDialogFragmentListener {

    companion object {
        private val TAG: String = MuseumDialogFragment::class.java.simpleName

        fun init(artworkLoader: ArtworkLoader, isLoggingEnabled: Boolean) {
            kz.zhombie.museum.Settings.setArtworkLoader(artworkLoader)
            kz.zhombie.museum.Settings.setLoggingEnabled(isLoggingEnabled)
        }

        private fun newInstance(
            uri: Uri,
            title: String,
            subtitle: String? = null,
            startViewPosition: ViewPosition
        ): MuseumDialogFragment {
            val fragment = MuseumDialogFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(BundleKey.URI, uri.toString())
                putSerializable(BundleKey.TITLE, title)
                if (!subtitle.isNullOrBlank()) putSerializable(BundleKey.SUBTITLE, subtitle)
                putString(BundleKey.START_VIEW_POSITION, startViewPosition.pack())
            }
            return fragment
        }
    }

    class Builder {
        private var uri: Uri? = null
        private var title: String? = null
        private var subtitle: String? = null
        private var viewPosition: ViewPosition? = null
        private var artworkView: View? = null
        private var artworkLoader: ArtworkLoader? = null
        private var callback: Callback? = null

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

        fun setArtworkView(artworkView: View): Builder {
            this.artworkView = artworkView
            return this
        }

        fun setArtworkLoader(artworkLoader: ArtworkLoader): Builder {
            this.artworkLoader = artworkLoader
            return this
        }

        fun setCallback(callback: Callback): Builder {
            this.callback = callback
            return this
        }

        fun build(): MuseumDialogFragment {
            return newInstance(
                uri = requireNotNull(uri) { "Museum artwork uri is mandatory value" },
                title = requireNotNull(title) { "Museum artwork title is mandatory value" },
                subtitle = subtitle,
                startViewPosition = requireNotNull(viewPosition) {
                    "Museum artwork needs start view position, in order to make smooth transition animation"
                }
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
            val transaction = fragmentManager.beginTransaction()
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            transaction
                .add(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
            return fragment
        }
    }

    private object BundleKey {
        const val URI = "uri"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val START_VIEW_POSITION = "start_view_position"
    }

    private lateinit var appBarLayout: AppBarLayout
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

    private lateinit var uri: Uri
    private lateinit var title: String
    private var subtitle: String? = null
    private lateinit var startViewPosition: ViewPosition

//    override fun getTheme(): Int {
//        return R.style.Museum_Dialog_Fullscreen
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setStyle(STYLE_NORMAL, theme)

        val arguments = arguments
        require(arguments != null) { "Provide arguments!" }
        uri = Uri.parse(requireNotNull(arguments.getString(BundleKey.URI)))
        title = requireNotNull(arguments.getString(BundleKey.TITLE))
        subtitle = arguments.getString(BundleKey.SUBTITLE)
        startViewPosition = ViewPosition.unpack(arguments.getString(BundleKey.START_VIEW_POSITION))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appBarLayout = view.findViewById(R.id.appBarLayout)
        toolbar = view.findViewById(R.id.toolbar)
        backgroundView = view.findViewById(R.id.backgroundView)
        gestureImageView = view.findViewById(R.id.gestureImageView)
        footerView = view.findViewById(R.id.footerView)
        titleView = view.findViewById(R.id.titleView)
        subtitleView = view.findViewById(R.id.subtitleView)

        setupActionBar()
        setupGestureImageView()
        setupInfo()

        artworkLoader?.loadFullscreenImage(requireContext(), gestureImageView, uri)

        gestureImageView.positionAnimator.addPositionUpdateListener { position, isLeaving ->
            val isFinished = position == 0F && isLeaving

            appBarLayout.alpha = position
            backgroundView.alpha = position
            footerView.alpha = position

            if (isFinished) {
                appBarLayout.visibility = View.INVISIBLE
                backgroundView.visibility = View.INVISIBLE
                footerView.visibility = View.INVISIBLE
            } else {
                appBarLayout.visibility = View.VISIBLE
                backgroundView.visibility = View.VISIBLE
                footerView.visibility = View.VISIBLE
            }

            gestureImageView.visibility = if (isFinished) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            if (isFinished) {
                if (!isPictureShowCalled) {
                    callback?.onPictureShow(17L)
                    isPictureShowCalled = true
                }

                gestureImageView.controller.settings.disableBounds()
                gestureImageView.positionAnimator.setState(0F, false, false)

                gestureImageView.postDelayed({ super.dismiss() }, 17L)
            }
        }

        if (savedInstanceState == null) {
            gestureImageView.positionAnimator.enter(startViewPosition, true)

            gestureImageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
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
        dismiss()
    }

    override fun dismiss() {
        if (!gestureImageView.positionAnimator.isLeaving) {
            gestureImageView.positionAnimator.exit(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isPictureShowCalled) {
            callback?.onPictureShow(17L)
            isPictureShowCalled = true
        }

        if (artworkView?.viewTreeObserver?.isAlive == true) {
            artworkView?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }

        onGlobalLayoutListener = null
        artworkView = null

        callback = null
    }

    private fun setupActionBar() {
        val activity = activity
        if (activity is AppCompatActivity) {
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.setDisplayShowTitleEnabled(false)
            toolbar.setNavigationOnClickListener { dismiss() }
        }
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
                appBarLayout.animate()
                    .alpha(0.0F)
                    .setDuration(100L)
                    .withEndAction {
                        appBarLayout.visibility = View.INVISIBLE
                    }
                    .start()

                footerView.animate()
                    .alpha(0.0F)
                    .setDuration(100L)
                    .withEndAction {
                        footerView.visibility = View.INVISIBLE
                    }
                    .start()

                isOverlayViewVisible = false
            } else {
                appBarLayout.animate()
                    .alpha(1.0F)
                    .setDuration(100L)
                    .withStartAction {
                        appBarLayout.visibility = View.VISIBLE
                    }
                    .start()

                footerView.animate()
                    .alpha(1.0F)
                    .setDuration(100L)
                    .withStartAction {
                        footerView.visibility = View.VISIBLE
                    }
                    .start()

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