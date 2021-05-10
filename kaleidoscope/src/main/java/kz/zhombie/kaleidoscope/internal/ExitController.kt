package kz.zhombie.kaleidoscope.internal

import android.graphics.Point
import android.graphics.RectF
import android.view.View
import kz.zhombie.kaleidoscope.GestureController
import kz.zhombie.kaleidoscope.Settings
import kz.zhombie.kaleidoscope.State
import kz.zhombie.kaleidoscope.utils.GravityUtils
import kz.zhombie.kaleidoscope.utils.MathUtils
import kz.zhombie.kaleidoscope.views.interfaces.AnimatorView
import kotlin.math.max
import kotlin.math.sign

class ExitController(
    view: View,
    private val gestureController: GestureController
) {

    companion object {
        private const val SCROLL_FACTOR = 0.5f
        private const val SCROLL_THRESHOLD = 30f
        private const val ZOOM_FACTOR = 0.75f
        private const val ZOOM_THRESHOLD = 0.75f
        private const val EXIT_THRESHOLD = 0.75f
        private const val MIN_EXIT_STATE = 0.01f // Ensure we'll not hit 0 accidentally

        // Temporary objects
        private val tmpArea: RectF = RectF()
        private val tmpPivot = Point()
    }

    init {
        animatorView = if (view is AnimatorView) view else null
        scrollThresholdScaled = UnitsUtils.toPixels(view.context, SCROLL_THRESHOLD)
    }

    private val scrollThresholdScaled: Float
    private val controller: GestureController
    private val animatorView: AnimatorView?
    private var exitState = 1f // 1f - fully opened, 0f - fully closed
    private var isZoomInAction = false
    private var isRotationInAction = false
    private var skipScrollDetection = false
    private var skipZoomDetection = false
    private var isScrollDetected = false
    private var isZoomDetected = false
    private var totalScrollX = 0f
    private var totalScrollY = 0f
    private var zoomAccumulator = 1f
    private var scrollDirection = 0f
    private var initialY = 0f
    private var initialZoom = 0f

    val isExitDetected: Boolean
        get() = isScrollDetected || isZoomDetected

    fun stopDetection() {
        if (isExitDetected) {
            exitState = 1f
            updateState()
            finishDetection()
        }
    }

    fun onUpOrCancel() {
        finishDetection()
    }

    /**
     * @param dx The distance along the X axis that has been scrolled since the last call
     * @param dy The distance along the Y axis that has been scrolled since the last call
     * @return true if scroll was consumed, false otherwise.
     */
    fun onScroll(dx: Float, dy: Float): Boolean {
        // Exit by scroll should not be detected if zoom or rotation is currently in place.
        // Also, we can detect scroll only if image is zoomed out and it reached movement bounds.
        var dy = dy
        if (!skipScrollDetection && !isExitDetected && canDetectExit()
            && canDetectScroll() && !canScroll(dy)
        ) {
            totalScrollX += dx
            totalScrollY += dy

            // Waiting until we scrolled enough to trigger exit detection or to skip it
            if (Math.abs(totalScrollY) > scrollThresholdScaled) {
                isScrollDetected = true
                initialY = controller.getState().getY()
                startDetection()
            } else if (Math.abs(totalScrollX) > scrollThresholdScaled) {
                skipScrollDetection = true
            }
        }
        if (isScrollDetected) {
            // Initializing scroll direction with current direction, if not initialized yet
            if (scrollDirection == 0f) {
                scrollDirection = sign(dy)
            }

            // Gradually decreasing scrolled distance when scrolling beyond exit point
            if (exitState < EXIT_THRESHOLD && sign(dy) == scrollDirection) {
                dy *= exitState / EXIT_THRESHOLD
            }

            // Updating exit state depending on the amount scrolled in relation to total space
            val total: Float = scrollDirection * SCROLL_FACTOR * max(
                controller.getSettings().getMovementAreaWidth(),
                controller.getSettings().getMovementAreaHeight()
            )
            exitState = 1f - (controller.getState().getY() + dy - initialY) / total
            exitState = MathUtils.restrict(exitState, MIN_EXIT_STATE, 1f)
            if (exitState == 1f) {
                // Scrolling to initial position
                controller.getState().translateTo(controller.getState().getX(), initialY)
            } else {
                // Applying scrolled distance
                controller.getState().translateBy(0f, dy)
            }
            updateState()
            if (exitState == 1f) {
                finishDetection()
            }
            return true
        }
        return isExitDetected
    }

    /**
     * @return true if fling was consumed, false otherwise.
     */
    fun onFling(): Boolean {
        return isExitDetected
    }

    fun onScaleBegin() {
        isZoomInAction = true
    }

    fun onScaleEnd() {
        isZoomInAction = false
        skipZoomDetection = false
        if (isZoomDetected) {
            finishDetection()
        }
    }

    /**
     * @param scaleFactor Current scaling factor
     * @return true if scale was consumed, false otherwise.
     */
    fun onScale(scaleFactor: Float): Boolean {
        // Exit by zoom should not be detected if rotation is currently in place.
        // Also, we can detect zoom only if image is zoomed out and we are zooming out.
        if (!canDetectZoom()) {
            skipZoomDetection = true
        }
        if (!skipZoomDetection && !isExitDetected && canDetectExit() && scaleFactor < 1f) {

            // Waiting until we zoomed enough to trigger exit detection
            zoomAccumulator *= scaleFactor
            if (zoomAccumulator < ZOOM_THRESHOLD) {
                isZoomDetected = true
                initialZoom = controller.getState().getZoom()
                startDetection()
            }
        }
        if (isZoomDetected) {
            // Updating exit state by applying zoom factor
            exitState = controller.getState().getZoom() * scaleFactor / initialZoom
            exitState = MathUtils.restrict(exitState, MIN_EXIT_STATE, 1f)
            GravityUtils.getDefaultPivot(controller.getSettings(), tmpPivot)
            if (exitState == 1f) {
                // Zooming to initial level using default pivot point
                controller.getState().zoomTo(initialZoom, tmpPivot.x.toFloat(), tmpPivot.y.toFloat())
            } else {
                // Applying zoom factor using default pivot point
                val scaleFactorFixed = 1f + (scaleFactor - 1f) * ZOOM_FACTOR
                controller.getState().zoomBy(scaleFactorFixed, tmpPivot.x.toFloat(), tmpPivot.y.toFloat())
            }
            updateState()
            if (exitState == 1f) {
                finishDetection()
                return true
            }
        }
        return isExitDetected
    }

    fun applyZoomPatch() {
        // Applying zoom patch (needed in case if image size is changed)
        initialZoom = controller.getStateController().applyZoomPatch(initialZoom)
    }

    fun onRotationBegin() {
        isRotationInAction = true
    }

    fun onRotationEnd() {
        isRotationInAction = false
    }

    /**
     * @return true if rotation was consumed, false otherwise.
     */
    fun onRotate(): Boolean {
        return isExitDetected
    }

    private fun canDetectExit(): Boolean {
        return controller.getSettings()
            .isExitEnabled() && animatorView != null && !animatorView.getPositionAnimator()
            .isLeaving()
    }

    private fun canDetectScroll(): Boolean {
        val exitType: Settings.ExitType = controller.getSettings().getExitType()
        return ((exitType === Settings.ExitType.ALL || exitType === Settings.ExitType.SCROLL)
                && !isZoomInAction && !isRotationInAction && isZoomedOut)
    }

    private fun canDetectZoom(): Boolean {
        val exitType: Settings.ExitType = controller.getSettings().getExitType()
        return ((exitType === Settings.ExitType.ALL || exitType === Settings.ExitType.ZOOM) && !isRotationInAction && isZoomedOut)
    }

    private fun canScroll(dy: Float): Boolean {
        if (!controller.getSettings().isRestrictBounds()) {
            return true
        }
        val state: State = controller.getState()
        controller.getStateController().getMovementArea(state, tmpArea)
        return (dy > 0f && State.compare(state.getY(), tmpArea.bottom) < 0f || dy < 0f && State.compare(state.getY(), tmpArea.top) > 0f)
    }

    private val isZoomedOut: Boolean
        private get() {
            val state: State = controller.getState()
            val minZoom: Float = controller.getStateController().getMinZoom(state)
            return State.compare(state.getZoom(), minZoom) <= 0
        }

    private fun startDetection() {
        controller.getSettings().disableBounds()
        if (controller is GestureControllerForPager) {
            (controller as GestureControllerForPager).disableViewPager(true)
        }
    }

    private fun finishDetection() {
        if (isExitDetected) {
            if (controller is GestureControllerForPager) {
                (controller as GestureControllerForPager).disableViewPager(false)
            }
            controller.getSettings().enableBounds()
            val animator: ViewPositionAnimator = animatorView.getPositionAnimator()
            if (!animator.isAnimating() && canDetectExit()) {
                // Exiting or returning to initial state if the view is not yet animating
                val position: Float = animator.getPosition()
                val isLeaving = position < EXIT_THRESHOLD
                if (isLeaving) {
                    animator.exit(true)
                } else {
                    val y: Float = controller.getState().getY()
                    val zoom: Float = controller.getState().getZoom()
                    val isScrolledBack = isScrollDetected && State.equals(y, initialY)
                    val isZoomedBack = isZoomDetected && State.equals(zoom, initialZoom)
                    if (position < 1f) {
                        animator.setState(position, false, true)

                        // Animating bounds if user didn't scroll or zoom to initial position
                        // manually. We will also need to temporary re-enable bounds restrictions
                        // disabled by position animator (a bit hacky though).
                        if (!isScrolledBack && !isZoomedBack) {
                            controller.getSettings().enableBounds()
                            controller.animateKeepInBounds()
                            controller.getSettings().disableBounds()
                        }
                    }
                }
            }
        }
        isScrollDetected = false
        isZoomDetected = false
        skipScrollDetection = false
        exitState = 1f
        scrollDirection = 0f
        totalScrollX = 0f
        totalScrollY = 0f
        zoomAccumulator = 1f
    }

    private fun updateState() {
        if (canDetectExit()) {
            animatorView?.getPositionAnimator()?.setToState(controller.getState(), exitState)
            animatorView?.getPositionAnimator()?.setState(
                pos = exitState,
                leaving = false,
                animate = false
            )
        }
    }

}