package kz.zhombie.kaleidoscope

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.viewpager.widget.ViewPager
import kz.zhombie.kaleidoscope.internal.detectors.RotationGestureDetector
import kotlin.math.*

/**
 * Allows cross movement between view controlled by this [GestureController] and it's parent
 * [ViewPager] by splitting scroll movements between them.
 */
class GestureControllerForPager(view: View) : GestureController(view) {

    companion object {
        private const val SCROLL_THRESHOLD = 15f
        private const val OVERSCROLL_THRESHOLD_FACTOR = 4f

        // Temporary objects
        private val tmpMatrix = Matrix()
        private val tmpRectF: RectF = RectF()

        /**
         * Because ViewPager will immediately return true from onInterceptTouchEvent() method during
         * settling animation, we will have no chance to prevent it from doing this.
         * But this listener will be called if ViewPager intercepted touch event,
         * so we can try fix this behavior here.
         */
        private val PAGER_TOUCH_LISTENER: View.OnTouchListener = object : View.OnTouchListener {
            private var isTouchInProgress = false

            @SuppressLint("ClickableViewAccessibility") // Not needed for ViewPager
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                // ViewPager will steal touch events during settling regardless of
                // requestDisallowInterceptTouchEvent. We will prevent it here.
                if (!isTouchInProgress && event?.actionMasked == MotionEvent.ACTION_DOWN) {
                    isTouchInProgress = true
                    // Now ViewPager is in drag mode, so it should not intercept DOWN event
                    view?.dispatchTouchEvent(event)
                    isTouchInProgress = false
                    return true
                }

                // User can touch outside of child view, so we will not have a chance to settle
                // ViewPager. If so, this listener should be called and we will be able to settle
                // ViewPager manually.
                settleViewPagerIfFinished(view, event)
                return true // We should skip view pager touches to prevent some subtle bugs
            }
        }

        private fun obtainOnePointerEvent(event: MotionEvent): MotionEvent =
            MotionEvent.obtain(event.downTime, event.eventTime, event.action, event.x, event.y, event.metaState)

        private fun settleViewPagerIfFinished(pager: ViewPager, event: MotionEvent) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                // Hack: if ViewPager is not settled we should force it to do so, fake drag will help
                try {
                    // Pager may throw an annoying exception if there are no internal page state items
                    pager.beginFakeDrag()
                    if (pager.isFakeDragging) {
                        pager.endFakeDrag()
                    }
                } catch (ignored: Exception) {
                }
            }
        }

        private fun transformToPagerEvent(event: MotionEvent, view: View, pager: ViewPager) {
            tmpMatrix.reset()
            transformMatrixToPager(tmpMatrix, view, pager)
            event.transform(tmpMatrix)
        }

        /**
         * Inspired by hidden method [android.view.View.transformMatrixToGlobal()].
         */
        private fun transformMatrixToPager(matrix: Matrix, view: View, pager: ViewPager) {
            if (view.parent is View) {
                val parent = view.parent as View
                if (parent !== pager) {
                    transformMatrixToPager(matrix, parent, pager)
                }
                matrix.preTranslate(-parent.scrollX.toFloat(), -parent.scrollY.toFloat())
            }
            matrix.preTranslate(view.left.toFloat(), view.top.toFloat())
            matrix.preConcat(view.matrix)
        }
    }

    private val touchSlop: Int = ViewConfiguration.get(view.context).scaledTouchSlop

    private var viewPager: ViewPager? = null
    private var isViewPagerDisabled = false

    private var isScrollGestureDetected = false
    private var isSkipViewPager = false

    private var viewPagerX = 0
    private var viewPagerSkippedX = 0f
    private var isViewPagerInterceptedScroll = false
    private var lastViewPagerEventX = 0f

    /**
     * Enables scroll inside [ViewPager]
     * (by enabling cross movement between ViewPager and it's child view).
     *
     * @param pager Target ViewPager
     */
    @SuppressLint("ClickableViewAccessibility")
    fun enableScrollInViewPager(pager: ViewPager) {
        viewPager = pager
        pager.setOnTouchListener(PAGER_TOUCH_LISTENER)

        // Disabling motion event splitting
        pager.isMotionEventSplittingEnabled = false
    }

    /**
     * Disables ViewPager scroll. Default is false.
     *
     * @param disable Whether to disable ViewPager scroll or not
     */
    fun disableViewPager(disable: Boolean) {
        isViewPagerDisabled = disable
    }

    @SuppressLint("ClickableViewAccessibility")  // performClick() will be called in super class
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        // We need to always receive touch events to pass them to ViewPager (if provided)
        val result = super.onTouch(view, event)
        return viewPager != null || result
    }

    override fun onTouchInternal(view: View, event: MotionEvent): Boolean {
        val viewPager = viewPager
        return if (viewPager == null) {
            super.onTouchInternal(view, event)
        } else {
            // Getting motion event in pager coordinates
            val pagerEvent: MotionEvent = MotionEvent.obtain(event)
            transformToPagerEvent(
                pagerEvent,
                view,
                viewPager
            )
            handleTouch(pagerEvent)
            val result = super.onTouchInternal(view, pagerEvent)
            pagerEvent.recycle()
            result
        }
    }

    override fun shouldDisallowInterceptTouch(event: MotionEvent): Boolean {
        // If ViewPager is set then we'll always disallow touch interception
        return viewPager != null || super.shouldDisallowInterceptTouch(event)
    }

    private fun handleTouch(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            if (event.pointerCount == 2) { // on first non-primary pointer
                // Skipping ViewPager fake dragging if we're not started dragging yet
                // to allow scale/rotation gestures
                isSkipViewPager = !hasViewPagerX()
            }
        }
    }

    override fun onDown(event: MotionEvent): Boolean {
        if (viewPager == null) {
            return super.onDown(event)
        }

        isSkipViewPager = false
        isViewPagerInterceptedScroll = false
        isScrollGestureDetected = false

        viewPagerX = computeInitialViewPagerScroll(event)
        lastViewPagerEventX = event.x
        viewPagerSkippedX = 0f

        passEventToViewPager(event)
        return super.onDown(event)
    }

    override fun onUpOrCancel(event: MotionEvent) {
        passEventToViewPager(event)
        super.onUpOrCancel(event)
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, dx: Float, dy: Float): Boolean {
        return if (viewPager == null) {
            super.onScroll(e1, e2, dx, dy)
        } else {
            if (!isScrollGestureDetected) {
                isScrollGestureDetected = true
                // First scroll event can stutter a bit, so we will ignore it for smoother scrolling
                return true
            }

            // Splitting movement between pager and view
            val fixedDistanceX = -scrollBy(e2, -dx)
            // Skipping vertical movement if ViewPager is dragged
            val fixedDistanceY = if (hasViewPagerX()) 0f else dy

            super.onScroll(e1, e2, fixedDistanceX, fixedDistanceY)
        }
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, vx: Float, vy: Float): Boolean {
        return !hasViewPagerX() && super.onFling(e1, e2, vx, vy)
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return !hasViewPagerX() && super.onScaleBegin(detector)
    }

    override fun onRotationBegin(detector: RotationGestureDetector): Boolean {
        return !hasViewPagerX() && super.onRotationBegin(detector)
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        return !hasViewPagerX() && super.onDoubleTapEvent(event)
    }

    /*
     * Scrolls ViewPager if view reached bounds. Returns distance at which view can be actually
     * scrolled. Here we will split given distance (dX) into movement of ViewPager and movement
     * of view itself.
     */
    private fun scrollBy(event: MotionEvent, dx: Float): Float {
        if (isSkipViewPager || isViewPagerDisabled) {
            return dx
        }
        val state: State = getState()
        getStateController().getMovementArea(state, tmpRectF)
        var pagerDx = splitPagerScroll(dx, state, tmpRectF)
        pagerDx = skipPagerMovement(pagerDx, state, tmpRectF)
        var viewDx = dx - pagerDx

        // Applying pager scroll
        val shouldFixViewX = isViewPagerInterceptedScroll && viewPagerX == 0
        val actualX = performViewPagerScroll(event, pagerDx)
        viewPagerX += actualX
        if (shouldFixViewX) { // Adding back scroll not handled by ViewPager
            viewDx += (pagerDx.roundToInt() - actualX).toFloat()
        }

        // Returning altered scroll left for image
        return viewDx
    }

    /*
     * Splits x scroll between viewpager and view.
     */
    private fun splitPagerScroll(dx: Float, state: State, movBounds: RectF): Float {
        return if (getSettings().isPanEnabled()) {
            val dir = sign(dx)
            val movementX = abs(dx) // always >= 0, no direction info
            val viewX = state.getX()

            // available movement distances (always >= 0, no direction info)
            var availableViewX: Float = if (dir < 0) {
                viewX - movBounds.left
            } else {
                movBounds.right - viewX
            }

            val availablePagerX = if (dir * viewPagerX < 0) {
                abs(viewPagerX).toFloat()
            } else {
                0F
            }

            // Not available if already overscrolled in same direction
            if (availableViewX < 0) {
                availableViewX = 0f
            }
            val pagerMovementX: Float = when {
                availablePagerX >= movementX -> {
                    // Only ViewPager is moved
                    movementX
                }
                availableViewX + availablePagerX >= movementX -> {
                    // Moving pager for full available distance and moving view for remaining distance
                    availablePagerX
                }
                else -> {
                    // Moving view for full available distance and moving pager for remaining distance
                    movementX - availableViewX
                }
            }
            pagerMovementX * dir // Applying direction
        } else {
            dx
        }
    }

    /*
     * Skips part of pager movement to make it harder scrolling pager when image is zoomed
     * or when image is over-scrolled in y direction.
     */
    private fun skipPagerMovement(pagerDx: Float, state: State, movBounds: RectF): Float {
        val overscrollDist: Float = getSettings().getOverscrollDistanceY() * OVERSCROLL_THRESHOLD_FACTOR
        var overscrollThreshold = 0f
        if (state.getY() < movBounds.top) {
            overscrollThreshold = (movBounds.top - state.getY()) / overscrollDist
        } else if (state.getY() > movBounds.bottom) {
            overscrollThreshold = (state.getY() - movBounds.bottom) / overscrollDist
        }
        val minZoom: Float = getStateController().getFitZoom(state)
        val zoomThreshold = if (minZoom == 0f) 0f else state.getZoom() / minZoom - 1f
        var pagerThreshold = max(overscrollThreshold, zoomThreshold)
        pagerThreshold = sqrt(max(0f, min(pagerThreshold, 1f)))
        pagerThreshold *= SCROLL_THRESHOLD * touchSlop

        // Resetting skipped amount when starting scrolling in different direction
        if (viewPagerSkippedX * pagerDx < 0f && viewPagerX == 0) {
            viewPagerSkippedX = 0f
        }

        // Ensuring we have full skipped amount if pager is scrolled
        if (hasViewPagerX()) {
            viewPagerSkippedX = pagerThreshold * sign(viewPagerX.toFloat())
        }

        // Skipping pager movement and accumulating skipped amount, if not passed threshold
        return if (abs(viewPagerSkippedX) < pagerThreshold && pagerDx * viewPagerSkippedX >= 0) {
            viewPagerSkippedX += pagerDx
            // Reverting over-skipped amount
            var over = abs(viewPagerSkippedX) - pagerThreshold
            over = max(0f, over) * sign(pagerDx)
            viewPagerSkippedX -= over
            over
        } else {
            pagerDx
        }
    }

    private fun computeInitialViewPagerScroll(downEvent: MotionEvent): Int {
        // ViewPager can be in intermediate position, we should compute correct initial scroll
        var scroll: Int = viewPager.getScrollX()
        val pageWidth: Int = viewPager.getWidth() + viewPager.getPageMargin()

        // After state restore ViewPager can return negative scroll, let's fix it
        while (scroll < 0) {
            scroll += pageWidth
        }
        val touchedItem = ((scroll + downEvent.x) / pageWidth).roundToInt()
        return pageWidth * touchedItem - scroll
    }

    private fun hasViewPagerX(): Boolean {
        // Looks like ViewPager has a rounding issue (it may be off by 1 in settled state)
        return viewPagerX < -1 || viewPagerX > 1
    }

    /*
     * Manually scrolls ViewPager and returns actual distance at which pager was scrolled.
     */
    private fun performViewPagerScroll(event: MotionEvent, pagerDx: Float): Int {
        val scrollBegin: Int = viewPager.getScrollX()
        lastViewPagerEventX += pagerDx
        passEventToViewPager(event)
        return scrollBegin - viewPager.getScrollX()
    }

    private fun passEventToViewPager(event: MotionEvent) {
        if (viewPager == null) return

        val fixedEvent: MotionEvent = obtainOnePointerEvent(event)
        fixedEvent.setLocation(lastViewPagerEventX, 0f)

        if (isViewPagerInterceptedScroll) {
            viewPager?.onTouchEvent(fixedEvent)
        } else {
            isViewPagerInterceptedScroll = viewPager?.onInterceptTouchEvent(fixedEvent) == true
        }

        // If ViewPager intercepted touch it will settle itself automatically,
        // but if touch was not intercepted we should settle it manually
        if (!isViewPagerInterceptedScroll && hasViewPagerX()) {
            settleViewPagerIfFinished(viewPager!!, event)
        }

        // Hack: ViewPager has bug when endFakeDrag() does not work properly. But we need to ensure
        // ViewPager is not in fake drag mode after settleViewPagerIfFinished()
        try {
            if (viewPager?.isFakeDragging == true) {
                viewPager?.endFakeDrag()
            }
        } catch (ignored: Exception) {
        }

        fixedEvent.recycle()
    }

}