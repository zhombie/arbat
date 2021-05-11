package kz.zhombie.museum

import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver.*
import androidx.core.os.HandlerCompat
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.alexvasilkov.gestures.commons.DepthPageTransformer
import com.alexvasilkov.gestures.transition.GestureTransitions
import com.alexvasilkov.gestures.transition.ViewsTransitionAnimator
import com.alexvasilkov.gestures.transition.tracker.SimpleTracker
import kz.zhombie.museum.adapter.ViewPagerAdapter
import kz.zhombie.museum.base.BaseDialogFragment
import kz.zhombie.museum.exception.PaintingLoaderNullException
import kz.zhombie.museum.logging.Logger
import kz.zhombie.museum.model.Painting

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
        private var canvasView: View? = null
        private var paintings: List<Painting>? = null
        private var isFooterViewEnabled: Boolean = false
        private var callback: Callback? = null

        fun setPaintingLoader(paintingLoader: PaintingLoader): Builder {
            this.paintingLoader = paintingLoader
            return this
        }

        fun setCanvasView(canvasView: View): Builder {
            this.canvasView = canvasView
            return this
        }

        fun setPaintings(paintings: List<Painting>): Builder {
            this.paintings = paintings
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
                    isFooterViewEnabled = isFooterViewEnabled
                )
            ).apply {
                this@Builder.canvasView?.let { setCanvasView(it) }

                if (!Settings.hasPaintingLoader()) {
                    Settings.setPaintingLoader(requireNotNull(paintingLoader) { PaintingLoaderNullException() })
                }

                this@Builder.callback?.let { setCallback(it) }
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

    private var callback: Callback? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    private var isPictureShowCalled: Boolean = false

    private var canvasView: View? = null
    private var params: Params? = null

    override fun getTheme(): Int = R.style.Museum_Dialog_Fullscreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        params = BundleManager.get(arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewHolder = ViewHolder(view).also { viewHolder ->
            setupToolbar()
            setupGestureImageView()
            setupFooterView()

            val viewPagerListener = object : SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    val painting = viewPagerAdapter?.getItem(position)
                    setPaintingInfo(painting)
                }
            }

            viewPagerAdapter = ViewPagerAdapter(viewHolder.viewPager, Settings.getPaintingLoader())
            viewPagerAdapter?.paintings = params?.paintings ?: emptyList()
            viewHolder.viewPager.addOnPageChangeListener(viewPagerListener)
            viewHolder.viewPager.setPageTransformer(true, DepthPageTransformer())
            viewHolder.viewPager.adapter = viewPagerAdapter

            val viewPagerTracker: SimpleTracker = object : SimpleTracker() {
                override fun getViewAt(position: Int): View? {
                    val holder = viewPagerAdapter?.getViewHolder(position)
                    return if (holder == null) null else ViewPagerAdapter.getImage(holder)
                }
            }

            viewsTransitionAnimator = canvasView?.let { canvasView ->
                // Setting up and animating image transition
                GestureTransitions.from<Int>(canvasView)
                    .into(viewHolder.viewPager, viewPagerTracker)
            }

            if (viewsTransitionAnimator == null) {
                viewsTransitionAnimator = GestureTransitions.fromNone<Int>()
                    .into(viewHolder.viewPager, viewPagerTracker)
            }

            viewsTransitionAnimator?.addPositionUpdateListener(::applyFullViewPagerState)
        }

        if (savedInstanceState == null) {
            if (!params?.paintings.isNullOrEmpty()) {
                setPaintingInfo(params?.paintings?.first())
            }

            if (canvasView == null) {
                viewsTransitionAnimator?.enterSingle(true)
            } else {
                viewsTransitionAnimator?.enter(0, true)
            }
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
            viewsTransitionAnimator?.exit(true)
        } else {
            super.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Logger.debug(TAG, "onDestroy()")

        if (!isPictureShowCalled) {
            callback?.onImageShow(0L)
            isPictureShowCalled = true
        }

        canvasView = null

        callback = null
    }

    private fun setupToolbar() {
        viewHolder?.toolbar?.setNavigationOnClickListener { dismiss() }
    }

    private fun setPaintingInfo(painting: Painting?) {
        if (painting == null) return

        viewHolder?.titleView?.text = painting.info?.title

        if (painting.info?.subtitle.isNullOrBlank()) {
            viewHolder?.subtitleView?.text = null
        } else {
            viewHolder?.subtitleView?.text = painting.info?.subtitle
        }
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
                callback?.onImageShow(0L)
                isPictureShowCalled = true
            }

            HandlerCompat.createAsync(Looper.getMainLooper())
                .postDelayed({ dismiss() }, 15L)
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

    override fun setCanvasView(view: View): MuseumDialogFragment {
        this.canvasView = view
        return this
    }

    interface Callback {
        fun onImageShow(delay: Long = 0L)
        fun onImageHide(delay: Long = 0L)
    }

}