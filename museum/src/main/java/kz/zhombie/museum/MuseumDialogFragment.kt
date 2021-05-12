package kz.zhombie.museum

import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.core.os.HandlerCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.alexvasilkov.gestures.commons.DepthPageTransformer
import com.alexvasilkov.gestures.commons.RecyclePagerAdapter
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.alexvasilkov.gestures.transition.tracker.SimpleTracker
import kz.zhombie.museum.adapter.ViewPagerAdapter
import kz.zhombie.museum.base.BaseDialogFragment
import kz.zhombie.museum.exception.PaintingLoaderNullException
import kz.zhombie.museum.logging.Logger
import kz.zhombie.museum.model.Painting
import java.util.*
import kotlin.math.min

class MuseumDialogFragment private constructor(
) : BaseDialogFragment(R.layout.museum_fragment_dialog), MuseumDialogFragmentListener {

    companion object {
        private val TAG: String = MuseumDialogFragment::class.java.simpleName

        fun init(paintingLoader: PaintingLoader, isLoggingEnabled: Boolean) {
            Settings.setPaintingLoader(paintingLoader)
            Settings.setLoggingEnabled(isLoggingEnabled)
        }

        private fun newInstance(params: Params): MuseumDialogFragment {
            val fragment = MuseumDialogFragment()
            fragment.arguments = BundleManager.build(params)
            return fragment
        }
    }

    class Builder {
        private var paintingLoader: PaintingLoader? = null

        private var paintings: List<Painting>? = null

        private var imageView: ImageView? = null

        private var recyclerView: RecyclerView? = null
        private var startPosition: Int? = null
        private var delegate: RecyclerViewTransitionDelegate? = null

        private var isFooterViewEnabled: Boolean? = null

        private var callback: Callback? = null

        fun setPaintingLoader(paintingLoader: PaintingLoader): Builder {
            this.paintingLoader = paintingLoader
            return this
        }

        fun setPaintings(paintings: List<Painting>): Builder {
            this.paintings = paintings
            return this
        }

        fun setImageView(imageView: ImageView?): Builder {
            this.imageView = imageView
            return this
        }

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
                if (!Settings.hasPaintingLoader()) {
                    Settings.setPaintingLoader(requireNotNull(paintingLoader) { PaintingLoaderNullException() })
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
            fragment.show(fragmentManager, null)
            return fragment
        }
    }

    private var viewHolder: ViewHolder? = null

    private var viewPagerAdapter: ViewPagerAdapter? = null

    private var viewsTransitionAnimator: ViewsTransitionAnimator<Int>? = null

    private var imageView: ImageView? = null
    private var recyclerView: RecyclerView? = null

    private var params: Params? = null

    private var isPictureShowCalled: Boolean = false

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

            val startPosition = if (params?.startPosition == null) {
                0
            } else {
                params?.startPosition ?: 0
            }

            Logger.debug(TAG, "startPosition: " + params?.startPosition)

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

    override fun onDestroy() {
        super.onDestroy()

        Logger.debug(TAG, "onDestroy()")

        if (!isPictureShowCalled) {
            isPictureShowCalled = true

            if (imageView != null) {
                HandlerCompat.createAsync(Looper.getMainLooper())
                    .postDelayed({ imageView?.visibility = View.VISIBLE }, 25L)
            }
        }

        callback = null

        viewsTransitionAnimator = null

        viewPagerAdapter = null

        imageView = null
        recyclerView = null

        params = null
    }

    private fun setupToolbar() {
        viewHolder?.toolbar?.setNavigationOnClickListener { dismiss() }
    }

    private fun setupViewPager() = viewHolder?.let { viewHolder ->
        viewPagerAdapter = ViewPagerAdapter(viewHolder.viewPager, Settings.getPaintingLoader())
        viewPagerAdapter?.paintings = params?.paintings ?: emptyList()
        viewHolder.viewPager.offscreenPageLimit = min(params?.paintings?.size ?: 3, 3)

        val viewPagerListener = object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                val painting = viewPagerAdapter?.getItem(position)
                setPaintingInfo(painting)
            }
        }
        viewHolder.viewPager.addOnPageChangeListener(viewPagerListener)

        viewHolder.viewPager.setPageTransformer(true, DepthPageTransformer())
        viewHolder.viewPager.adapter = viewPagerAdapter
    }

    private fun setupViewTransitionAnimator() = viewHolder?.let { viewHolder ->
        val viewPagerTracker: SimpleTracker = object : SimpleTracker() {
            override fun getViewAt(position: Int): View? {
                val holder: RecyclePagerAdapter.ViewHolder? = viewPagerAdapter?.getViewHolder(position)
                return if (holder == null) null else ViewPagerAdapter.getImageView(holder)
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
     * Applying [ViewPager] image animation state: fading out toolbar, title and background.
     */
    private fun applyFullViewPagerState(position: Float, isLeaving: Boolean) {
        Logger.debug(TAG, "applyFullPagerState() -> $position, $isLeaving")

        val isFinished = position == 0F && isLeaving

        viewHolder?.backgroundView?.visibility = if (position == 0F) View.INVISIBLE else View.VISIBLE
        viewHolder?.backgroundView?.alpha = position

        viewHolder?.toolbar?.visibility = if (position == 0F) View.INVISIBLE else View.VISIBLE
        viewHolder?.toolbar?.alpha = if (isSystemUiShown()) position else 0F

        viewHolder?.footerView?.visibility = if (position == 1F) View.VISIBLE else View.INVISIBLE

        if (isLeaving && position == 0f) {
            viewPagerAdapter?.setActivated(false)
        }

        if (isFinished) {
            Logger.debug(TAG, "isFinished")

            if (!isPictureShowCalled) {
                isPictureShowCalled = true

                if (imageView != null) {
                    HandlerCompat.createAsync(Looper.getMainLooper())
                        .postDelayed({ imageView?.visibility = View.VISIBLE }, 0L)
                }
            }

            HandlerCompat.createAsync(Looper.getMainLooper())
                .postDelayed({ dismiss() }, 25L)
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
    }

    interface RecyclerViewTransitionDelegate {
        fun getImageView(holder: RecyclerView.ViewHolder): View?
    }

}