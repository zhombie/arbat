package kz.zhombie.museum

import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.core.os.HandlerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.alexvasilkov.gestures.commons.DepthPageTransformer
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.alexvasilkov.gestures.transition.tracker.SimpleTracker
import kz.zhombie.museum.logging.Logger
import kz.zhombie.museum.model.Painting
import kz.zhombie.museum.model.Params
import kz.zhombie.museum.ui.ViewHolder
import kz.zhombie.museum.ui.adapter.ViewPagerAdapter
import kz.zhombie.museum.ui.base.BaseDialogFragment
import java.util.*
import kotlin.math.min

class MuseumDialogFragment private constructor(
) : BaseDialogFragment(R.layout.museum_fragment_dialog), MuseumDialogFragmentListener {

    companion object {
        private val TAG: String = MuseumDialogFragment::class.java.simpleName

        fun init(paintingLoader: PaintingLoader, isLoggingEnabled: Boolean) {
            Settings.setPermanentPaintingLoader(paintingLoader)
            Settings.setLoggingEnabled(isLoggingEnabled)
        }

        fun clear() {
            Settings.cleanupSettings()
        }

        private fun newInstance(params: Params): MuseumDialogFragment {
            val fragment = MuseumDialogFragment()
            fragment.arguments = BundleManager.build(params)
            return fragment
        }
    }

    class Builder {
        private var tag: String? = null

        private var paintingLoader: PaintingLoader? = null

        private var paintings: List<Painting>? = null

        private var imageView: ImageView? = null

        private var recyclerView: RecyclerView? = null
        private var startPosition: Int? = null
        private var delegate: RecyclerViewTransitionDelegate? = null

        private var isFooterViewEnabled: Boolean? = null

        private var callback: Callback? = null

        fun setTag(tag: String): Builder {
            this.tag = tag
            return this
        }

        fun setPaintingLoader(paintingLoader: PaintingLoader): Builder {
            this.paintingLoader = paintingLoader
            return this
        }

        fun setPainting(painting: Painting): Builder {
            return setPaintings(listOf(painting))
        }

        fun setPaintings(paintings: List<Painting>): Builder {
            this.paintings = paintings
            return this
        }

        fun setImageView(imageView: ImageView?): Builder {
            this.imageView = imageView
            return this
        }

        // TODO: Update code, in order to fix memory leak
        @Deprecated("Causes memory leak, that's why avoid usage of this method")
        fun setRecyclerView(recyclerView: RecyclerView?): Builder {
            this.recyclerView = recyclerView
            return this
        }

        fun setStartPosition(position: Int): Builder {
            this.startPosition = position
            return this
        }

        fun setRecyclerViewTransitionDelegate(delegate: RecyclerViewTransitionDelegate): Builder {
            this.delegate = delegate
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
            val paintings = paintings
            if (paintings.isNullOrEmpty()) throw IllegalStateException("Museum paintings is mandatory value")
            return newInstance(
                Params(
                    paintings = paintings,
                    startPosition = startPosition,
                    isFooterViewEnabled = isFooterViewEnabled
                )
            ).apply {
                this@Builder.paintingLoader?.let {
                    Settings.setTemporaryPaintingLoader(it)
                }

                setImageView(this@Builder.imageView)

                setRecyclerView(this@Builder.recyclerView)
                setRecyclerViewTransitionDelegate(this@Builder.delegate)

                setCallback(this@Builder.callback)
            }
        }

        fun show(fragmentManager: FragmentManager): MuseumDialogFragment {
            val fragment = build()
            fragment.isCancelable = true
            fragment.show(fragmentManager, tag)
            return fragment
        }

        fun dismissPrevious(fragmentManager: FragmentManager): Fragment? {
            val fragment = fragmentManager.findFragmentByTag(TAG)
            if (fragment is MuseumDialogFragment) {
                fragment.dismiss()
            }
            return fragment
        }

        fun showSafely(fragmentManager: FragmentManager): MuseumDialogFragment {
            dismissPrevious(fragmentManager)
            setTag(TAG)
            return show(fragmentManager)
        }
    }

    private val handler by lazy { HandlerCompat.createAsync(Looper.getMainLooper()) }

    // -------------------------------------------------

    private var imageView: ImageView? = null
    private var recyclerView: RecyclerView? = null

    private var params: Params? = null

    // -------------------------------------------------

    private var viewHolder: ViewHolder? = null

    private var viewPagerAdapter: ViewPagerAdapter? = null

    private var viewsTransitionAnimator: ViewsTransitionAnimator<Int>? = null

    private var isPaintingShowCalled: Boolean = false

    private var isOverlayViewVisible: Boolean = true

    // -------------------------------------------------

    private var delegate: RecyclerViewTransitionDelegate? = null
    private var callback: Callback? = null

    private fun setRecyclerViewTransitionDelegate(delegate: RecyclerViewTransitionDelegate?) {
        this.delegate = delegate
    }

    private fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    override fun getTheme(): Int = R.style.Museum_Dialog_Fullscreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        params = BundleManager.parse(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewHolder = ViewHolder(view)

        setupToolbar()
        setupGestureImageView()
        setupFooterView()
        setupViewPager()
        setupViewTransitionAnimator()

        if (savedInstanceState == null) {
            if (!params?.paintings.isNullOrEmpty()) {
                val painting = params?.paintings?.first()
                setPaintingInfo(painting)
            }

            var startPosition = params?.startPosition ?: 0

            if (startPosition < 0) {
                startPosition = 0
            }

            if (startPosition > (params?.paintings?.lastIndex ?: 0)) {
                startPosition = (params?.paintings?.lastIndex ?: 0)
            }

            Logger.debug(TAG, "startPosition: $startPosition")

            viewsTransitionAnimator?.enter(startPosition, true)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        Logger.debug(TAG, "onCancel()")

        dismiss()
    }

    override fun dismiss() {
        Logger.debug(TAG, "dismiss()")
        Logger.debug(TAG, "dismiss() -> ${viewsTransitionAnimator?.isLeaving}")

        if (viewsTransitionAnimator?.isLeaving == false) {
            if (imageView == null) {
                viewsTransitionAnimator?.exit(false)
            } else {
                viewsTransitionAnimator?.exit(true)
            }
        } else {
            super.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Logger.debug(TAG, "onDestroyView()")

        if (!isPaintingShowCalled) {
            isPaintingShowCalled = true

            Logger.debug(TAG, "onDestroy() -> isPaintingShowCalled: $isPaintingShowCalled")

            imageView?.visibility = View.VISIBLE
        }

        viewsTransitionAnimator = null

        viewPagerAdapter = null

        imageView = null
        recyclerView = null

        viewHolder = null
    }

    override fun onDestroy() {
        super.onDestroy()

        Logger.debug(TAG, "onDestroy()")

        params = null

        Settings.cleanupTemporarySettings()

        delegate = null

        callback?.onDestroy()
        callback = null
    }

    private fun setupToolbar() {
        viewHolder?.toolbar?.setNavigationOnClickListener { dismiss() }
    }

    private fun setupViewPager() = viewHolder?.let { viewHolder ->
        viewPagerAdapter = ViewPagerAdapter(Settings.getPaintingLoader()) {
            if (isOverlayViewVisible) {
                viewHolder.toolbar.animate()
                    .alpha(0.0F)
                    .setDuration(100L)
                    .withEndAction {
                        viewHolder.toolbar.visibility = View.INVISIBLE
                    }
                    .start()

                if (params?.isFooterViewEnabled == true) {
                    viewHolder.footerView.animate()
                        .alpha(0.0F)
                        .setDuration(100L)
                        .withEndAction {
                            viewHolder.footerView.visibility = View.INVISIBLE
                        }
                        .start()
                }

                isOverlayViewVisible = false
            } else {
                viewHolder.toolbar.animate()
                    .alpha(1.0F)
                    .setDuration(100L)
                    .withStartAction {
                        viewHolder.toolbar.visibility = View.VISIBLE
                    }
                    .start()

                if (params?.isFooterViewEnabled == true) {
                    viewHolder.footerView.animate()
                        .alpha(1.0F)
                        .setDuration(100L)
                        .withStartAction {
                            viewHolder.footerView.visibility = View.VISIBLE
                        }
                        .start()
                }

                isOverlayViewVisible = true
            }
        }

        viewPagerAdapter?.paintings = params?.paintings ?: emptyList()

        viewHolder.viewPager.offscreenPageLimit = min(params?.paintings?.size ?: 3, 3)

        viewHolder.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val painting = viewPagerAdapter?.getItem(position)
                setPaintingInfo(painting)
            }
        })

        viewHolder.viewPager.setPageTransformer(DepthPageTransformer())

        viewHolder.viewPager.adapter = viewPagerAdapter
    }

    private fun setupViewTransitionAnimator() = viewHolder?.let { viewHolder ->
        val viewPagerTracker: SimpleTracker = object : SimpleTracker() {
            override fun getViewAt(position: Int): View? {
                return viewPagerAdapter?.getImageView(position)
            }
        }

        viewsTransitionAnimator = when {
            imageView != null -> {
                GestureTransitions.from<Int>(imageView!!)
                    .into(viewHolder.viewPager, viewPagerTracker)
            }
            recyclerView != null -> {
                val recyclerViewTracker: SimpleTracker = object : SimpleTracker() {
                    override fun getViewAt(position: Int): View? {
                        val holder: RecyclerView.ViewHolder? = recyclerView?.findViewHolderForLayoutPosition(position)
                        return if (holder == null) null else delegate?.getImageView(holder)
                    }
                }

                GestureTransitions.from(recyclerView!!, recyclerViewTracker)
                    .into(viewHolder.viewPager, viewPagerTracker)
            }
            else -> {
                GestureTransitions.fromNone<Int>()
                    .into(viewHolder.viewPager, viewPagerTracker)
            }
        }

        // Setting up and animating image transition
        viewsTransitionAnimator?.addPositionUpdateListener(::applyFullViewPagerState)
    }

    private fun setPaintingInfo(painting: Painting?): Boolean {
        if (painting == null) return false

        viewHolder?.titleView?.text = painting.info?.title

        if (painting.info?.subtitle.isNullOrBlank()) {
            viewHolder?.subtitleView?.text = null
        } else {
            viewHolder?.subtitleView?.text = painting.info?.subtitle
        }

        return true
    }

    /**
     * Applying [ViewPager2] image animation state: fading out toolbar, title and background.
     */
    private fun applyFullViewPagerState(position: Float, isLeaving: Boolean) = viewHolder?.let { viewHolder ->
        Logger.debug(TAG, "applyFullPagerState() -> $position, $isLeaving")

        val isFinished = position == 0F && isLeaving

        viewHolder.toolbar.alpha = position
        viewHolder.backgroundView.alpha = position

        if (params?.isFooterViewEnabled == true) {
            viewHolder.footerView.alpha = position
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

        viewHolder.viewPager.visibility = if (isFinished) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }

        if (isFinished) {
            if (!isPaintingShowCalled) {
                isPaintingShowCalled = true

                Logger.debug(TAG, "applyFullViewPagerState() -> isPaintingShowCalled: $isPaintingShowCalled")

                imageView?.visibility = View.VISIBLE
            }

            handler.postDelayed({ dismiss() }, 25L)
        }
    }

    private fun setupGestureImageView() {
        // Click actions
//        gestureImageView.setOnClickListener {
//            if (isOverlayViewVisible) {
//                toolbar.animate()
//                    .alpha(0.0F)
//                    .setDuration(100L)
//                    .withEndAction {
//                        toolbar.visibility = View.INVISIBLE
//                    }
//                    .start()
//
//                if (isFooterViewEnabled) {
//                    footerView.animate()
//                        .alpha(0.0F)
//                        .setDuration(100L)
//                        .withEndAction {
//                            footerView.visibility = View.INVISIBLE
//                        }
//                        .start()
//                }
//
//                isOverlayViewVisible = false
//            } else {
//                toolbar.animate()
//                    .alpha(1.0F)
//                    .setDuration(100L)
//                    .withStartAction {
//                        toolbar.visibility = View.VISIBLE
//                    }
//                    .start()
//
//                if (isFooterViewEnabled) {
//                    footerView.animate()
//                        .alpha(1.0F)
//                        .setDuration(100L)
//                        .withStartAction {
//                            footerView.visibility = View.VISIBLE
//                        }
//                        .start()
//                }
//
//                isOverlayViewVisible = true
//            }
//        }
    }

    private fun setupFooterView() {
        if (params?.isFooterViewEnabled == true) {
            viewHolder?.footerView?.visibility = View.VISIBLE
        } else {
            viewHolder?.footerView?.visibility = View.GONE
        }
    }

    /**
     * [MuseumDialogFragmentListener] implementation
     */

    override fun setImageView(imageView: ImageView?): MuseumDialogFragment {
        this.imageView = imageView
        return this
    }

    override fun setRecyclerView(recyclerView: RecyclerView?): MuseumDialogFragment {
        this.recyclerView = recyclerView
        return this
    }

    interface Callback {
        fun onDestroy()
    }

    interface RecyclerViewTransitionDelegate {
        fun getImageView(holder: RecyclerView.ViewHolder): View?
    }

}