package kz.zhombie.kaleidoscope

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.view.*
import android.widget.OverScroller
import kz.zhombie.kaleidoscope.GestureController.*
import kz.zhombie.kaleidoscope.GestureController.StateSource.*
import kz.zhombie.kaleidoscope.internal.AnimationEngine
import kz.zhombie.kaleidoscope.internal.ExitController
import kz.zhombie.kaleidoscope.internal.MovementBounds
import kz.zhombie.kaleidoscope.internal.detectors.RotationGestureDetector
import kz.zhombie.kaleidoscope.internal.detectors.ScaleGestureDetectorFixed
import kz.zhombie.kaleidoscope.utils.FloatScroller
import kz.zhombie.kaleidoscope.utils.GravityUtils
import kz.zhombie.kaleidoscope.utils.MathUtils
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Handles touch events to update view's position state ([State]) based on current
 * setup ([Settings]).
 * Settings can be obtained and altered through [getSettings].
 * Note, that some settings are required in order to correctly update state, see [Settings].
 *
 * This class implements [View.OnTouchListener] to delegate touches from view to controller.
 *
 * State can also be manipulated directly with [getState], [updateState]
 * and [resetState]. You can also access [getStateController] for some additional
 * stuff.
 *
 * State can be animated with [animateStateTo] method.
 * See also [stopFlingAnimation], [stopStateAnimation] and [stopAllAnimations] methods.
 *
 * All state changes will be passed to [OnStateChangeListener].
 * See [addOnStateChangeListener] and [removeOnStateChangeListener] methods.
 *
 * Additional touch events can be listened with [OnGestureListener] and
 * [SimpleOnGestureListener] using [setOnGesturesListener] method.
 *
 * State source changes (whether state is being changed by user or by animation) can be
 * listened with [OnStateSourceChangeListener] using
 * [setOnStateSourceChangeListener] method.
 */
open class GestureController constructor(view: View) : View.OnTouchListener {

    companion object {
        private const val FLING_COEFFICIENT = 0.9F

        // Temporary objects
        private val tmpPointF: PointF = PointF()
        private val tmpPoint = Point()
        private val tmpRectF: RectF = RectF()
        private val tmpPointArr = FloatArray(2)
    }

    // Control constants converted to pixels
    private val touchSlop: Int
    private val minVelocity: Int
    private val maxVelocity: Int

    private var gestureListener: OnGestureListener? = null
    private var sourceListener: OnStateSourceChangeListener? = null
    private val stateListeners: MutableList<OnStateChangeListener> = ArrayList()

    private val animationEngine: AnimationEngine

    // Various gesture detectors
    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector
    private val rotateDetector: RotationGestureDetector

    private var isInterceptTouchCalled = false
    private var isInterceptTouchDisallowed = false
    private var isScrollDetected = false
    private var isScaleDetected = false
    private var isRotationDetected = false

    private var pivotX = Float.NaN
    private var pivotY = Float.NaN
    private var endPivotX = Float.NaN
    private var endPivotY = Float.NaN

    private var isStateChangedDuringTouch = false
    private var isRestrictZoomRequested = false
    private var isRestrictRotationRequested = false
    private var isAnimatingInBounds = false

    private var stateSource = StateSource.NONE

    private val flingScroller: OverScroller
    private val stateScroller: FloatScroller

    private val flingBounds: MovementBounds
    private val stateStart = State()
    private val stateEnd = State()

    private val targetView: View

    private val settings: Settings
    private val state = State()
    private val prevState = State()
    private val stateController: StateController
    private val exitController: ExitController

    init {
        val context = view.context

        targetView = view
        settings = Settings()
        stateController = StateController(settings)

        animationEngine = LocalAnimationEngine(view)
        val internalListener = InternalGesturesListener()
        gestureDetector = GestureDetector(context, internalListener)
        scaleDetector = ScaleGestureDetectorFixed(context, internalListener)
        rotateDetector = RotationGestureDetector(context, internalListener)

        exitController = ExitController(view, this)

        flingScroller = OverScroller(context)
        stateScroller = FloatScroller()

        flingBounds = MovementBounds(settings)

        val configuration: ViewConfiguration = ViewConfiguration.get(context)
        touchSlop = configuration.scaledTouchSlop
        minVelocity = configuration.scaledMinimumFlingVelocity
        maxVelocity = configuration.scaledMaximumFlingVelocity
    }

    /**
     * Sets listener for basic touch events.
     *
     * @param listener Gestures listener
     * @see OnGestureListener
     */
    // Public API
    fun setOnGesturesListener(listener: OnGestureListener?) {
        gestureListener = listener
    }

    /**
     * Sets listener for state source changes.
     *
     * @param listener State's source changes listener
     * @see OnStateSourceChangeListener
     */
    // Public API
    fun setOnStateSourceChangeListener(listener: OnStateSourceChangeListener?) {
        sourceListener = listener
    }

    /**
     * Adds listener for state changes.
     *
     * @param listener State changes listener
     * @see OnStateChangeListener
     */
    fun addOnStateChangeListener(listener: OnStateChangeListener) {
        stateListeners.add(listener)
    }

    /**
     * Removes listener for state changes.
     *
     * @param listener State changes listener to be removed
     * @see .addOnStateChangeListener
     */
    // Public API
    fun removeOnStateChangeListener(listener: OnStateChangeListener) {
        stateListeners.remove(listener)
    }

    /**
     * Returns settings that can be updated.
     *
     * Note: call [updateState], [resetState] or [animateKeepInBounds]
     * after settings was changed to correctly apply state restrictions.
     *
     * @return Gesture view's settings
     */
    fun getSettings(): Settings {
        return settings
    }

    /**
     * Current state.
     *
     * If this state is changed from outside you should call
     * [GestureController.updateState] or [animateKeepInBounds]
     * to properly apply changes.
     *
     * @return Current state
     */
    fun getState(): State {
        return state
    }

    /**
     * @return State controller to get computed min/max zoom levels or calculate movement area
     */
    fun getStateController(): StateController {
        return stateController
    }

    /**
     * Applies state restrictions and notifies [OnStateChangeListener] listeners.
     */
    fun updateState() {
        // Applying zoom patch (needed in case if image size is changed)
        stateController.applyZoomPatch(state)
        stateController.applyZoomPatch(prevState)
        stateController.applyZoomPatch(stateStart)
        stateController.applyZoomPatch(stateEnd)
        exitController.applyZoomPatch()
        val reset = stateController.updateState(state)
        if (reset) {
            notifyStateReset()
        } else {
            notifyStateUpdated()
        }
    }

    /**
     * Resets to initial state (default position, min zoom level) and notifies
     * [OnStateChangeListener] listeners.
     *
     *
     * Should be called when image size is changed.
     *
     *
     * See [Settings.setImage].
     */
    fun resetState() {
        stopAllAnimations()
        val reset = stateController.resetState(state)
        if (reset) {
            notifyStateReset()
        } else {
            notifyStateUpdated()
        }
    }

    /**
     * Sets pivot point for zooming when keeping image in bounds.
     *
     * @param pivotX Pivot point's X coordinate
     * @param pivotY Pivot point's Y coordinate
     * @see .animateKeepInBounds
     * @see .animateStateTo
     */
    fun setPivot(pivotX: Float, pivotY: Float) {
        this.pivotX = pivotX
        this.pivotY = pivotY
    }

    /**
     * Animates to correct position withing the bounds.
     *
     * @return `true` if animation started, `false` otherwise. Animation may
     * not be started if image already withing the bounds.
     */
    // Public API
    fun animateKeepInBounds(): Boolean {
        return animateStateTo(state, true)
    }

    /**
     * Animates current state to provided end state.
     *
     * @param endState End state
     * @return `true` if animation started, `false` otherwise. Animation may
     * not be started if end state is `null` or equals to current state (after bounds
     * restrictions are applied).
     */
    fun animateStateTo(endState: State?): Boolean {
        return animateStateTo(endState, true)
    }

    private fun animateStateTo(endState: State?, keepInBounds: Boolean): Boolean {
        if (endState == null) {
            return false
        }
        stopAllAnimations()

        // Ensure we have a correct pivot point
        if (java.lang.Float.isNaN(pivotX) || java.lang.Float.isNaN(pivotY)) {
            GravityUtils.getDefaultPivot(settings, tmpPoint)
            pivotX = tmpPoint.x.toFloat()
            pivotY = tmpPoint.y.toFloat()
        }
        var endStateRestricted: State? = null
        if (keepInBounds) {
            endStateRestricted = stateController.restrictStateBoundsCopy(
                state = endState,
                prevState = prevState,
                pivotX = pivotX,
                pivotY = pivotY,
                allowOverscroll = false,
                allowOverzoom = false,
                restrictRotation = true
            )
        }
        if (endStateRestricted == null) {
            endStateRestricted = endState
        }
        if (endStateRestricted.equals(state)) {
            return false // Nothing to animate
        }
        isAnimatingInBounds = keepInBounds
        stateStart.set(state)
        stateEnd.set(endStateRestricted)

        // Computing new position of pivot point for correct state interpolation
        tmpPointArr[0] = pivotX
        tmpPointArr[1] = pivotY
        MathUtils.computeNewPosition(tmpPointArr, stateStart, stateEnd)
        endPivotX = tmpPointArr[0]
        endPivotY = tmpPointArr[1]
        stateScroller.setDuration(settings.getAnimationsDuration())
        stateScroller.startScroll(0f, 1f)
        animationEngine.start()
        notifyStateSourceChanged()
        return true
    }

    // Public API
    val isAnimatingState: Boolean
        get() = !stateScroller.isFinished()

    // Public API
    val isAnimatingFling: Boolean
        get() = !flingScroller.isFinished

    // Public API
    val isAnimating: Boolean
        get() = isAnimatingState || isAnimatingFling

    // Public API
    fun stopStateAnimation() {
        if (isAnimatingState) {
            stateScroller.forceFinished()
            onStateAnimationFinished(true)
        }
    }

    // Public API
    fun stopFlingAnimation() {
        if (isAnimatingFling) {
            flingScroller.forceFinished(true)
            onFlingAnimationFinished(true)
        }
    }

    fun stopAllAnimations() {
        stopStateAnimation()
        stopFlingAnimation()
    }

    // Public API (can be overridden)
    protected fun onStateAnimationFinished(forced: Boolean) {
        isAnimatingInBounds = false
        pivotX = Float.NaN
        pivotY = Float.NaN
        endPivotX = Float.NaN
        endPivotY = Float.NaN
        notifyStateSourceChanged()
    }

    // Public API (can be overridden)
    protected fun onFlingAnimationFinished(forced: Boolean) {
        if (!forced) {
            animateKeepInBounds()
        }
        notifyStateSourceChanged()
    }

    // Public API (can be overridden)
    protected fun notifyStateUpdated() {
        prevState.set(state)
        for (listener in stateListeners) {
            listener.onStateChanged(state)
        }
    }

    // Public API (can be overridden)
    protected fun notifyStateReset() {
        exitController.stopDetection()
        for (listener in stateListeners) {
            listener.onStateReset(prevState, state)
        }
        notifyStateUpdated()
    }

    private fun notifyStateSourceChanged() {
        var type = StateSource.NONE
        if (isAnimating) {
            type = StateSource.ANIMATION
        } else if (isScrollDetected || isScaleDetected || isRotationDetected) {
            type = StateSource.USER
        }
        if (stateSource != type) {
            stateSource = type
            sourceListener?.onStateSourceChanged(type)
        }
    }

    // -------------------
    //  Gestures handling
    // -------------------
    fun onInterceptTouch(view: View, event: MotionEvent): Boolean {
        isInterceptTouchCalled = true
        return onTouchInternal(view, event)
    }

    // performClick is called in gestures callbacks
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (!isInterceptTouchCalled) { // Preventing duplicate events
            onTouchInternal(view, event)
        }
        isInterceptTouchCalled = false
        return settings.isEnabled()
    }

    protected fun onTouchInternal(view: View, event: MotionEvent): Boolean {
        val viewportEvent: MotionEvent = MotionEvent.obtain(event)
        viewportEvent.offsetLocation(-view.paddingLeft.toFloat(), -view.paddingTop.toFloat())
        gestureDetector.setIsLongpressEnabled(view.isLongClickable)
        var result: Boolean = gestureDetector.onTouchEvent(viewportEvent)
        scaleDetector.onTouchEvent(viewportEvent)
        rotateDetector.onTouchEvent(viewportEvent)
        result = result || isScaleDetected || isRotationDetected
        notifyStateSourceChanged()
        if (exitController.isExitDetected()) {
            if (state != prevState) {
                notifyStateUpdated()
            }
        }
        if (isStateChangedDuringTouch) {
            isStateChangedDuringTouch = false
            stateController.restrictStateBounds(
                state = state,
                prevState = prevState,
                pivotX = pivotX,
                pivotY = pivotY,
                allowOverscroll = true,
                allowOverzoom = true,
                restrictRotation = false
            )
            if (state != prevState) {
                notifyStateUpdated()
            }
        }
        if (isRestrictZoomRequested || isRestrictRotationRequested) {
            isRestrictZoomRequested = false
            isRestrictRotationRequested = false
            if (!exitController.isExitDetected()) {
                val restrictedState = stateController.restrictStateBoundsCopy(
                    state = state,
                    prevState = prevState,
                    pivotX = pivotX,
                    pivotY = pivotY,
                    allowOverscroll = true,
                    allowOverzoom = false,
                    restrictRotation = true
                )
                animateStateTo(restrictedState, false)
            }
        }
        if (viewportEvent.actionMasked == MotionEvent.ACTION_UP || viewportEvent.actionMasked == MotionEvent.ACTION_CANCEL) {
            onUpOrCancel(viewportEvent)
            notifyStateSourceChanged()
        }
        if (!isInterceptTouchDisallowed && shouldDisallowInterceptTouch(viewportEvent)) {
            isInterceptTouchDisallowed = true
            val parent: ViewParent? = view.parent
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        viewportEvent.recycle()
        return result
    }

    protected fun shouldDisallowInterceptTouch(event: MotionEvent): Boolean {
        if (exitController.isExitDetected()) {
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {

                // If view can be panned then parent should not intercept touch events.
                // We should check it on DOWN event since parent may quickly take control over us
                // in case of a very fast MOVE action.
                stateController.getMovementArea(state, tmpRectF)
                val isPannable = (State.compare(tmpRectF.width(), 0f) > 0 || State.compare(
                    tmpRectF.height(),
                    0f
                ) > 0)
                if (settings.isPanEnabled() && (isPannable || !settings.isRestrictBounds())) {
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // If view can be zoomed or rotated then parent should not intercept touch events.
                return settings.isZoomEnabled() || settings.isRotationEnabled()
            }
            else -> {
            }
        }
        return false
    }

    protected fun onDown(event: MotionEvent): Boolean {
        isInterceptTouchDisallowed = false
        stopFlingAnimation()
        gestureListener?.onDown(event)
        return false
    }

    protected fun onUpOrCancel(event: MotionEvent) {
        isScrollDetected = false
        isScaleDetected = false
        isRotationDetected = false
        exitController.onUpOrCancel()
        if (!isAnimatingFling && !isAnimatingInBounds) {
            animateKeepInBounds()
        }
        gestureListener?.onUpOrCancel(event)
    }

    // Public API (can be overridden)
    protected fun onSingleTapUp(event: MotionEvent): Boolean {
        // If double tap is not enabled then it should be safe to propagate click event from here
        if (!settings.isDoubleTapEnabled()) {
            targetView.performClick()
        }
        return gestureListener?.onSingleTapUp(event) == true
    }

    // Public API (can be overridden)
    protected fun onLongPress(event: MotionEvent) {
        if (settings.isEnabled()) {
            targetView.performLongClick()
            gestureListener?.onLongPress(event)
        }
    }

    protected fun onScroll(e1: MotionEvent, e2: MotionEvent, dx: Float, dy: Float): Boolean {
        if (!settings.isPanEnabled() || isAnimatingState) {
            return false
        }
        val scrollConsumed: Boolean = exitController.onScroll(-dx, -dy)
        if (scrollConsumed) {
            return true
        }
        if (!isScrollDetected) {
            isScrollDetected = (abs(e2.x - e1.x) > touchSlop || abs(e2.y - e1.y) > touchSlop)

            // First scroll event can stutter a bit, so we will ignore it for smoother scrolling
            if (isScrollDetected) {
                // By returning false here we give children views a chance to intercept this scroll
                return false
            }
        }
        if (isScrollDetected) {
            state.translateBy(-dx, -dy)
            isStateChangedDuringTouch = true
        }
        return isScrollDetected
    }

    protected fun onFling(e1: MotionEvent, e2: MotionEvent, vx: Float, vy: Float): Boolean {
        if (!settings.isPanEnabled() || !settings.isFlingEnabled() || isAnimatingState) {
            return false
        }
        val flingConsumed: Boolean = exitController.onFling()
        if (flingConsumed) {
            return true
        }
        stopFlingAnimation()

        // Fling bounds including current position
        flingBounds.set(state).extend(state.x, state.y)
        flingScroller.fling(
            Math.round(state.x),
            Math.round(state.y),
            limitFlingVelocity(vx * FLING_COEFFICIENT),
            limitFlingVelocity(vy * FLING_COEFFICIENT),
            Int.MIN_VALUE,
            Int.MAX_VALUE,
            Int.MIN_VALUE,
            Int.MAX_VALUE
        )
        animationEngine.start()
        notifyStateSourceChanged()
        return true
    }

    private fun limitFlingVelocity(velocity: Float): Int {
        return when {
            abs(velocity) < minVelocity -> 0
            abs(velocity) >= maxVelocity -> sign(velocity).toInt() * maxVelocity
            else -> velocity.roundToInt()
        }
    }

    /**
     * @param dx Current X offset in the fling scroll
     * @param dy Current Y offset in the fling scroll
     * @return true if state was changed, false otherwise.
     */
    // Public API (can be overridden)
    protected fun onFlingScroll(dx: Int, dy: Int): Boolean {
        val prevX = state.getX()
        val prevY = state.getY()
        var toX = prevX + dx
        var toY = prevY + dy
        if (settings.isRestrictBounds()) {
            flingBounds.restrict(toX, toY, tmpPointF)
            toX = tmpPointF.x
            toY = tmpPointF.y
        }
        state.translateTo(toX, toY)
        return !State.equals(prevX, toX) || !State.equals(prevY, toY)
    }

    // Public API (can be overridden)
    protected fun onSingleTapConfirmed(event: MotionEvent?): Boolean {
        // If double tap is enabled we should propagate click only if we aren't in a double tap now
        if (settings.isDoubleTapEnabled()) {
            targetView.performClick()
        }
        return gestureListener?.onSingleTapConfirmed(event) == true
    }

    protected fun onDoubleTapEvent(event: MotionEvent): Boolean {
        if (!settings.isDoubleTapEnabled()) {
            return false
        }
        if (event.actionMasked != MotionEvent.ACTION_UP) {
            return false
        }

        // ScaleGestureDetector can perform zoom by "double tap & drag" since KITKAT,
        // so we should suppress our double tap in this case
        if (isScaleDetected) {
            return false
        }

        // Let user redefine double tap
        if (gestureListener?.onDoubleTap(event) == true) {
            return true
        }
        animateStateTo(stateController.toggleMinMaxZoom(state, event.x, event.y))
        return true
    }

    protected fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        isScaleDetected = settings.isZoomEnabled()
        if (isScaleDetected) {
            exitController.onScaleBegin()
        }
        return isScaleDetected
    }

    // Public API (can be overridden)
    protected fun onScale(detector: ScaleGestureDetector): Boolean {
        if (!settings.isZoomEnabled() || isAnimatingState) {
            return false // Ignoring scroll if animation is in progress
        }
        val scaleFactor: Float = detector.scaleFactor
        val scaleConsumed: Boolean = exitController.onScale(scaleFactor)
        if (scaleConsumed) {
            return true
        }
        pivotX = detector.focusX
        pivotY = detector.focusY
        state.zoomBy(scaleFactor, pivotX, pivotY)
        isStateChangedDuringTouch = true
        return true
    }

    // Public API (can be overridden)
    protected fun onScaleEnd(detector: ScaleGestureDetector?) {
        if (isScaleDetected) {
            exitController.onScaleEnd()
        }
        isScaleDetected = false
        isRestrictZoomRequested = true
    }

    protected fun onRotationBegin(detector: RotationGestureDetector?): Boolean {
        isRotationDetected = settings.isRotationEnabled()
        if (isRotationDetected) {
            exitController.onRotationBegin()
        }
        return isRotationDetected
    }

    // Public API (can be overridden)
    protected fun onRotate(detector: RotationGestureDetector): Boolean {
        if (!settings.isRotationEnabled() || isAnimatingState) {
            return false
        }
        val rotateConsumed: Boolean = exitController.onRotate()
        if (rotateConsumed) {
            return true
        }
        pivotX = detector.getFocusX()
        pivotY = detector.getFocusY()
        state.rotateBy(detector.getRotationDelta(), pivotX, pivotY)
        isStateChangedDuringTouch = true
        return true
    }

    // Public API (can be overridden)
    protected fun onRotationEnd(detector: RotationGestureDetector?) {
        if (isRotationDetected) {
            exitController.onRotationEnd()
        }
        isRotationDetected = false
        isRestrictRotationRequested = true
    }

    /**
     * Animation engine implementation to animate state changes.
     */
    private inner class LocalAnimationEngine internal constructor(view: View) : AnimationEngine(view) {
        override fun onStep(): Boolean {
            var shouldProceed = false
            if (isAnimatingFling) {
                val prevX: Int = flingScroller.currX
                val prevY: Int = flingScroller.currY
                if (flingScroller.computeScrollOffset()) {
                    val dx: Int = flingScroller.currX - prevX
                    val dy: Int = flingScroller.currY - prevY
                    if (!onFlingScroll(dx, dy)) {
                        stopFlingAnimation()
                    }
                    shouldProceed = true
                }
                if (!isAnimatingFling) {
                    onFlingAnimationFinished(false)
                }
            }
            if (isAnimatingState) {
                stateScroller.computeScroll()
                val factor: Float = stateScroller.getCurr()
                MathUtils.interpolate(
                    state,
                    stateStart, pivotX, pivotY,
                    stateEnd, endPivotX, endPivotY,
                    factor
                )
                shouldProceed = true
                if (!isAnimatingState) {
                    onStateAnimationFinished(false)
                }
            }
            if (shouldProceed) {
                notifyStateUpdated()
            }
            return shouldProceed
        }
    }

    // -------------------
    //  Listeners
    // -------------------

    /**
     * State changes listener.
     */
    interface OnStateChangeListener {
        fun onStateChanged(state: State)
        fun onStateReset(oldState: State, newState: State)
    }

    /**
     * State source changes listener.
     *
     * @see StateSource
     */
    // Public API
    interface OnStateSourceChangeListener {
        fun onStateSourceChanged(source: StateSource?)
    }

    /**
     * Source of state changes. Values: [NONE], [USER], [ANIMATION].
     */
    // Public API
    enum class StateSource {
        NONE, USER, ANIMATION
    }

    /**
     * Listener for different touch events.
     */
    // Public API
    interface OnGestureListener {
        /**
         * @param event Motion event
         * @see GestureDetector.OnGestureListener.onDown
         */
        fun onDown(event: MotionEvent)

        /**
         * @param event Motion event
         */
        fun onUpOrCancel(event: MotionEvent)

        /**
         * @param event Motion event
         * @return true if event was consumed, false otherwise.
         * @see GestureDetector.OnGestureListener.onSingleTapUp
         */
        fun onSingleTapUp(event: MotionEvent): Boolean

        /**
         * @param event Motion event
         * @return true if event was consumed, false otherwise.
         * @see GestureDetector.OnDoubleTapListener.onSingleTapConfirmed
         */
        fun onSingleTapConfirmed(event: MotionEvent): Boolean

        /**
         * Note, that long press is disabled by default, use [View.setLongClickable]
         * to enable it.
         *
         * @param event Motion event
         * @see GestureDetector.OnGestureListener.onLongPress
         */
        fun onLongPress(event: MotionEvent)

        /**
         * @param event Motion event
         * @return true if event was consumed, false otherwise.
         * @see GestureDetector.OnDoubleTapListener.onDoubleTap
         */
        fun onDoubleTap(event: MotionEvent): Boolean
    }

    /**
     * Simple implementation of [OnGestureListener].
     */
    // Public API
    class SimpleOnGestureListener : OnGestureListener {
        /**
         * {@inheritDoc}
         */
        override fun onDown(event: MotionEvent) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        override fun onUpOrCancel(event: MotionEvent) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            return false
        }

        /**
         * {@inheritDoc}
         */
        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            return false
        }

        /**
         * {@inheritDoc}
         */
        override fun onLongPress(event: MotionEvent) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        override fun onDoubleTap(event: MotionEvent): Boolean {
            return false
        }
    }

    /**
     * All listeners in one class.
     * It will also allow us to make all methods protected to cleanup public API.
     */
    private inner class InternalGesturesListener : GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener,
        RotationGestureDetector.OnRotationGestureListener {

        override fun onSingleTapConfirmed(event: MotionEvent?): Boolean {
            return this@GestureController.onSingleTapConfirmed(event)
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            return false
        }

        override fun onDoubleTapEvent(event: MotionEvent): Boolean {
            return this@GestureController.onDoubleTapEvent(event)
        }

        override fun onDown(event: MotionEvent): Boolean {
            return this@GestureController.onDown(event)
        }

        override fun onShowPress(event: MotionEvent) {
            // No-op
        }

        override fun onSingleTapUp(event: MotionEvent): Boolean {
            return this@GestureController.onSingleTapUp(event)
        }

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            return this@GestureController.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onLongPress(event: MotionEvent) {
            this@GestureController.onLongPress(event)
        }

        override fun onFling(
            e1: MotionEvent, e2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            return this@GestureController.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onRotate(detector: RotationGestureDetector): Boolean {
            return this@GestureController.onRotate(detector)
        }

        override fun onRotationBegin(detector: RotationGestureDetector): Boolean {
            return this@GestureController.onRotationBegin(detector)
        }

        override fun onRotationEnd(detector: RotationGestureDetector) {
            this@GestureController.onRotationEnd(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            return this@GestureController.onScale(detector)
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return this@GestureController.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            this@GestureController.onScaleEnd(detector)
        }
    }

}