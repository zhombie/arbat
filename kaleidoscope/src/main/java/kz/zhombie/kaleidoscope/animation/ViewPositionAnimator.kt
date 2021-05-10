package kz.zhombie.kaleidoscope.animation

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.FloatRange
import androidx.fragment.app.Fragment
import kz.zhombie.kaleidoscope.GestureController
import kz.zhombie.kaleidoscope.GestureControllerForPager
import kz.zhombie.kaleidoscope.Settings
import kz.zhombie.kaleidoscope.State
import kz.zhombie.kaleidoscope.internal.AnimationEngine
import kz.zhombie.kaleidoscope.internal.GestureDebug
import kz.zhombie.kaleidoscope.utils.FloatScroller
import kz.zhombie.kaleidoscope.utils.GravityUtils
import kz.zhombie.kaleidoscope.utils.MathUtils
import kz.zhombie.kaleidoscope.views.interfaces.ClipBounds
import kz.zhombie.kaleidoscope.views.interfaces.ClipView
import kz.zhombie.kaleidoscope.views.interfaces.GestureView
import java.util.*
import kotlin.math.max

/**
 * Helper class to animate views from one position on screen to another.
 *
 * Animation can be performed from any view (e.g. [ImageView]) to any gestures controlled
 * view implementing [GestureView] (e.g. [GestureImageView]).
 *
 * Note, that initial and final views should have same aspect ratio for correct animation.
 * In case of [ImageView] initial and final images should have same aspect, but actual views
 * can have different aspects (e.g. animating from square thumb view with scale type
 * [ScaleType.CENTER_CROP] to rectangular full image view).
 *
 * To use this class first create an instance and then call [enter].
 * Alternatively you can manually pass initial view position using
 * [enter] method.
 * To exit back to initial view call [exit] method.
 * You can listen for position changes using
 * [addPositionUpdateListener].
 * If initial view was changed you should call [.update] method to update to new view.
 * You can also manually update initial view position using [.update] method.
 */
class ViewPositionAnimator constructor(to: GestureView) {

    companion object {
        private const val TAG = "ViewPositionAnimator"

        private val tmpMatrix = Matrix()
        private val tmpPointArr = FloatArray(2)
        private val tmpPoint = Point()

        private fun getDisplaySize(context: Context, rect: Rect) {
            fun getActivity(context: Context): Activity {
                if (context is Activity) {
                    return context
                }
                if (context is Fragment) {
                    return context.requireActivity()
                }
                throw IllegalArgumentException("Illegal context")
            }

            val activity = getActivity(context)
            val windowManager: WindowManager = activity.windowManager
            val metrics = DisplayMetrics()
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    context.display?.getRealMetrics(metrics)
                else ->
                    windowManager.defaultDisplay.getRealMetrics(metrics)
            }
            rect[0, 0, metrics.widthPixels] = metrics.heightPixels
        }
    }

    private val listeners: MutableList<PositionUpdateListener> = ArrayList()
    private val listenersToRemove: MutableList<PositionUpdateListener> = ArrayList()
    private var iteratingListeners = false

    private val positionScroller: FloatScroller = FloatScroller()
    private val animationEngine: AnimationEngine

    private val toController: GestureController
    private val toClipView: ClipView?
    private val toClipBounds: ClipBounds?

    private val fromState: State = State()
    private val toState: State = State()
    private var fromPivotX = 0f
    private var fromPivotY = 0f
    private var toPivotX = 0f
    private var toPivotY = 0f
    private val windowRect = Rect()
    private val fromClip: RectF = RectF()
    private val toClip: RectF = RectF()
    private val fromBoundsClip: RectF = RectF()
    private val toBoundsClip: RectF = RectF()
    private val clipRectTmp: RectF = RectF()
    private var fromPos: ViewPosition? = null
    private var toPos: ViewPosition? = null
    private var fromNonePos = false

    private var fromView: View? = null

    private var isActivated = false

    private var toPosition = 1F
    private var position = 0F

    private var isLeaving = true // Leaving by default
    private var isAnimating = false
    private var isApplyingPosition = false
    private var isApplyingPositionScheduled = false

    // Marks that update for 'From' or 'To' is needed
    private var isFromUpdated = false
    private var isToUpdated = false

    private val fromPosHolder = ViewPositionHolder()
    private val toPosHolder = ViewPositionHolder()

    private val fromPositionListener: ViewPositionHolder.OnViewPositionChangeListener =
        object : ViewPositionHolder.OnViewPositionChangeListener {
            override fun onViewPositionChanged(position: ViewPosition) {
                if (GestureDebug.isDebugAnimator()) {
                    Log.d(TAG, "'From' view position updated: " + position.pack())
                }
                fromPos = position
                requestUpdateFromState()
                applyCurrentPosition()
            }
        }

    init {
        require(to is View) { "Argument 'to' should be an instance of View" }

        toClipView = if (to is ClipView) to else null
        toClipBounds = if (to is ClipBounds) to else null
        animationEngine = LocalAnimationEngine(to)

        getDisplaySize(to.context, windowRect)

        toController = to.getController()
        toController.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateChanged(state: State) {
                // Applying zoom patch (needed in case if image size is changed)
                toController.getStateController().applyZoomPatch(fromState)
                toController.getStateController().applyZoomPatch(toState)
            }

            override fun onStateReset(oldState: State, newState: State) {
                if (!isActivated) return

                if (GestureDebug.isDebugAnimator()) {
                    Log.d(TAG, "State reset in listener: $newState")
                }

                setToState(newState, 1f) // We have to reset full state
                applyCurrentPosition()
            }
        })

        toPosHolder.init(to, object : ViewPositionHolder.OnViewPositionChangeListener {
            override fun onViewPositionChanged(position: ViewPosition) {
                if (GestureDebug.isDebugAnimator()) {
                    Log.d(TAG, "'To' view position updated: " + position.pack())
                }

                toPos = position
                requestUpdateToState()
                requestUpdateFromState() // Depends on 'to' position
                applyCurrentPosition()
            }
        })

        // Position updates are paused by default, until animation is started
        fromPosHolder.pause(true)
        toPosHolder.pause(true)
    }

    /**
     * Starts [enter] animation from no specific position (position will be calculated based on
     * gravity set in [Settings]).
     *
     * Note, that in most cases you should use [enter(v: View, b: Boolean)] or
     * [enter(p: ViewPosition, b: Boolean)] methods instead.
     *
     * @param withAnimation Whether to animate entering or immediately jump to entered state
     */
    fun enter(withAnimation: Boolean) {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Entering from none position, with animation = $withAnimation")
        }

        enterInternal(withAnimation)
        updateInternal()
    }

    /**
     * Starts [enter] animation from [from] view to [to].
     *
     * Note, if [from] view was changed (i.e. during list adapter refresh) you should
     * update to new view using [update] method.
     *
     * @param from 'From' view
     * @param withAnimation Whether to animate entering or immediately jump to entered state
     */
    fun enter(from: View, withAnimation: Boolean) {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Entering from view, with animation = $withAnimation")
        }

        enterInternal(withAnimation)
        updateInternal(from)
    }

    /**
     * Starts [enter] animation from [from] position to [to] view.
     *
     *
     * Note, if [from] view position was changed (i.e. during list adapter refresh) you
     * should update to new view using [update] method.
     *
     * @param fromPos 'From' view position
     * @param withAnimation Whether to animate entering or immediately jump to entered state
     */
    fun enter(fromPos: ViewPosition, withAnimation: Boolean) {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Entering from view position, with animation = $withAnimation")
        }

        enterInternal(withAnimation)
        updateInternal(fromPos)
    }

    /**
     * Updates initial view in case it was changed. You should not call this method if view stays
     * the same since animator should automatically detect view position changes.
     *
     * @param from New [from] view
     */
    fun update(from: View) {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Updating view")
        }

        updateInternal(from)
    }

    /**
     * Updates position of initial view in case it was changed.
     *
     * @param from New [from] view position
     */
    fun update(from: ViewPosition) {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Updating view position: " + from.pack())
        }

        updateInternal(from)
    }

    /**
     * Updates position of initial view to no specific position, in case [to] view is not available
     * anymore.
     */
    fun updateToNone() {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Updating view to no specific position")
        }

        updateInternal()
    }

    /**
     * Starts [exit] animation from [to] view back to [from].
     *
     * @param withAnimation Whether to animate exiting or immediately jump to initial state
     */
    fun exit(withAnimation: Boolean) {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Exiting, with animation = $withAnimation")
        }

        check(isActivated) { "You should call enter(...) before calling exit(...)" }

        // Resetting 'to' position if not animating exit already
        if (!(isAnimating && position <= toPosition) && position > 0) {
            setToState(toController.getState(), position)
        }

        // Starting animation from current position or applying initial state without animation
        setState(if (withAnimation) position else 0f, true, withAnimation)
    }

    private fun enterInternal(withAnimation: Boolean) {
        isActivated = true

        toController.updateState() // Ensure we are animating to correct state
        setToState(toController.getState(), 1f) // We are always entering to full mode

        // Starting animation from initial position or applying final state without animation
        setState(if (withAnimation) 0f else 1f, false, withAnimation)
    }

    private fun updateInternal(from: View) {
        cleanBeforeUpdateInternal()
        fromView = from
        fromPosHolder.init(from, fromPositionListener)
        from.visibility = View.INVISIBLE // We don't want duplicate view during animation
    }

    private fun updateInternal(from: ViewPosition) {
        cleanBeforeUpdateInternal()
        fromPos = from
        applyCurrentPosition()
    }

    private fun updateInternal() {
        cleanBeforeUpdateInternal()
        fromNonePos = true
        applyCurrentPosition()
    }

    private fun cleanBeforeUpdateInternal() {
        check(isActivated) { "You should call enter(...) before calling update(...)" }

        cleanup()
        requestUpdateFromState()
    }

    private fun cleanup() {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Cleaning up")
        }

        fromView?.visibility = View.VISIBLE // Switching back to visible

        toClipView?.clipView(null, 0f)

        fromPosHolder.clear()
        fromView = null
        fromPos = null
        fromNonePos = false
        isToUpdated = false
        isFromUpdated = isToUpdated
    }

    /**
     * Adds position state changes listener that will be notified during animations.
     *
     * @param listener Position listener
     */
    fun addPositionUpdateListener(listener: PositionUpdateListener) {
        listeners.add(listener)
        listenersToRemove.remove(listener)
    }

    /**
     * Removes position state changes listener as added by
     * [addPositionUpdateListener].
     *
     * Note, this method may be called inside listener's callback without throwing
     * [IndexOutOfBoundsException].
     *
     * @param listener Position listener to be removed
     */
    fun removePositionUpdateListener(listener: PositionUpdateListener) {
        if (iteratingListeners) {
            listenersToRemove.add(listener)
        } else {
            listeners.remove(listener)
        }
    }

    private fun ensurePositionUpdateListenersRemoved() {
        listeners.removeAll(listenersToRemove)
        listenersToRemove.clear()
    }

    /**
     * @return Target (to) position as set by [setToState].
     * Maybe useful to determine real animation position during exit gesture.
     *
     *
     * I.e. [getPosition] / [getToPosition] (changes from 0 to ∞)
     * represents interpolated position used to calculate intermediate state and bounds.
     */
    // We really need this method to point to itself
    fun getToPosition(): Float {
        return toPosition
    }

    /**
     * @return Current position within range `[0, 1]`, where `0` is for
     * initial (from) position and `1` is for final (to) position.
     *
     * Note, that final position can be changed by [.setToState], so if you
     * need to have real value of final position (instead of `1`) then you need to use
     * [getToPosition] method.
     */
    fun getPosition(): Float {
        return position
    }

    /**
     * @return Whether animator is in leaving state. Means that animation direction is
     * from final (to) position back to initial (from) position.
     */
    fun isLeaving(): Boolean {
        return isLeaving
    }

    /**
     * Specifies target ([to]) state and it's position which will be used to interpolate
     * current state for intermediate positions (i.e. during animation or exit gesture).
     * This allows you to set up correct state without changing current position
     * ([getPosition]).
     *
     * Only use this method if you understand what you do.
     *
     * @param state Target ('to') state
     * @param position Target ('to') position
     * @see .getToPosition
     */
    fun setToState(state: State, @FloatRange(from = 0.0, to = 1.0) position: Float) {
        require(position > 0) { "'To' position cannot be <= 0" }
        require(position <= 1f) { "'To' position cannot be > 1" }

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "State reset: $state at $position")
        }

        toPosition = position
        toState.set(state)
        requestUpdateToState()
        requestUpdateFromState()
    }

    /**
     * Stops current animation and sets position state to particular values.
     *
     * Note, that once animator reaches [state] = 0f and [isLeaving] = true
     * it will cleanup all internal stuff. So you will need to call [.enter]
     * or [enter] again in order to continue using animator.
     *
     * @param pos Current position
     * @param leaving Whether we we are in exiting direction (`true`) or in entering
     * (`false`)
     * @param animate Whether we should start animating from given position and in given direction
     */
    fun setState(
        @FloatRange(from = 0.0, to = 1.0) pos: Float,
        leaving: Boolean,
        animate: Boolean
    ) {
        check(isActivated) { "You should call enter(...) before calling setState(...)" }

        stopAnimation()
        position = if (pos < 0f) 0f else if (pos > 1f) 1f else pos
        isLeaving = leaving
        if (animate) {
            startAnimationInternal()
        }
        applyCurrentPosition()
    }

    private fun applyCurrentPosition() {
        if (!isActivated) return

        if (isApplyingPosition) {
            // Excluding possible nested calls, scheduling sequential call instead
            isApplyingPositionScheduled = true
            return
        }
        isApplyingPosition = true

        // We do not need to update while 'to' view is fully visible or fully closed
        val paused = if (isLeaving) position == 0f else position == 1f
        fromPosHolder.pause(paused)
        toPosHolder.pause(paused)

        // Perform state updates if needed
        if (!isToUpdated) {
            updateToState()
        }
        if (!isFromUpdated) {
            updateFromState()
        }

        if (GestureDebug.isDebugAnimator()) {
            Log.d(
                TAG, "Applying state: " + position + " / " + isLeaving
                        + ", 'to' ready = " + isToUpdated + ", 'from' ready = " + isFromUpdated
            )
        }

        val canUpdate = position < toPosition || isAnimating && position == toPosition
        if (isToUpdated && isFromUpdated && canUpdate) {
            val state: State = toController.getState()

            MathUtils.interpolate(
                state, fromState, fromPivotX, fromPivotY,
                toState, toPivotX, toPivotY, position / toPosition
            )

            toController.updateState()

            val skipClip = position >= toPosition || position == 0f && isLeaving
            val clipPosition = position / toPosition

            if (toClipView != null) {
                MathUtils.interpolate(clipRectTmp, fromClip, toClip, clipPosition)
                toClipView.clipView(if (skipClip) null else clipRectTmp, state.getRotation())
            }
            if (toClipBounds != null) {
                MathUtils.interpolate(clipRectTmp, fromBoundsClip, toBoundsClip, clipPosition)
                toClipBounds.clipBounds(if (skipClip) null else clipRectTmp)
            }
        }

        iteratingListeners = true
        var i = 0
        val size = listeners.size
        while (i < size) {
            if (isApplyingPositionScheduled) {
                break // No need to call listeners anymore
            }
            listeners[i].onPositionUpdate(position, isLeaving)
            i++
        }
        iteratingListeners = false
        ensurePositionUpdateListenersRemoved()

        if (position == 0f && isLeaving) {
            cleanup()
            isActivated = false
            toController.resetState() // Switching to initial state
        }

        isApplyingPosition = false

        if (isApplyingPositionScheduled) {
            isApplyingPositionScheduled = false
            applyCurrentPosition()
        }
    }

    /**
     * @return Whether view position animation is in progress or not.
     */
    fun isAnimating(): Boolean {
        return isAnimating
    }

    /**
     * Starts animation from current position ([position]) in current
     * direction ([isLeaving]).
     */
    private fun startAnimationInternal() {
        val duration: Long = toController.getSettings().getAnimationsDuration()
        val durationFraction = if (toPosition == 1f) {
            if (isLeaving) position else 1f - position
        } else if (isLeaving) {
            position / toPosition
        } else {
            (1f - position) / (1f - toPosition)
        }
        positionScroller.setDuration((duration * durationFraction).toLong())
        positionScroller.startScroll(position, if (isLeaving) 0f else 1f)
        animationEngine.start()
        onAnimationStarted()
    }

    /**
     * Stops current animation, if any.
     */
    // Public API
    fun stopAnimation() {
        positionScroller.forceFinished()
        onAnimationStopped()
    }

    private fun onAnimationStarted() {
        if (isAnimating) return

        isAnimating = true

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Animation started")
        }

        // Disabling bounds restrictions & any gestures
        toController.getSettings().disableBounds().disableGestures()
        // Stopping all currently playing state animations
        toController.stopAllAnimations()

        // Disabling ViewPager scroll
        if (toController is GestureControllerForPager) {
            toController.disableViewPager(true)
        }
    }

    private fun onAnimationStopped() {
        if (!isAnimating) return

        isAnimating = false

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Animation stopped")
        }

        // Restoring original settings
        toController.getSettings().enableBounds().enableGestures()

        // Enabling ViewPager scroll
        if (toController is GestureControllerForPager) {
            toController.disableViewPager(false)
        }

        toController.animateKeepInBounds()
    }

    private fun requestUpdateToState() {
        isToUpdated = false
    }

    private fun requestUpdateFromState() {
        isFromUpdated = false
    }

    private fun updateToState() {
        if (isToUpdated) return

        val settings: Settings = toController.getSettings()

        val toPos = toPos
        if (toPos == null || !settings.hasImageSize()) return

        toState.get(tmpMatrix)

        // 'To' clip is a 'To' image rect in 'To' view coordinates
        toClip.set(0f, 0f, settings.getImageWidth().toFloat(), settings.getImageHeight().toFloat())

        // Computing pivot point as center of the image after transformation
        tmpPointArr[0] = toClip.centerX()
        tmpPointArr[1] = toClip.centerY()
        tmpMatrix.mapPoints(tmpPointArr)

        toPivotX = tmpPointArr[0]
        toPivotY = tmpPointArr[1]

        // Computing clip rect in 'To' view coordinates without rotation
        tmpMatrix.postRotate(-toState.getRotation(), toPivotX, toPivotY)
        tmpMatrix.mapRect(toClip)
        toClip.offset(
            (toPos.viewport.left - toPos.view.left).toFloat(),
            (toPos.viewport.top - toPos.view.top).toFloat()
        )

        // 'To' bounds clip is entire window rect in 'To' view coordinates
        toBoundsClip.set(
            (windowRect.left - toPos.view.left).toFloat(),
            (windowRect.top - toPos.view.top).toFloat(),
            (windowRect.right - toPos.view.left).toFloat(),
            (windowRect.bottom - toPos.view.top).toFloat()
        )

        isToUpdated = true

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "'To' state updated")
        }
    }

    private fun updateFromState() {
        if (isFromUpdated) return

        val toPos = toPos

        val settings: Settings = toController.getSettings()
        if (fromNonePos && toPos != null) {
            fromPos = if (fromPos == null) ViewPosition.newInstance() else fromPos
            GravityUtils.getDefaultPivot(settings, tmpPoint)
            tmpPoint.offset(toPos.view.left, toPos.view.top) // Ensure we're in correct coordinates
            ViewPosition.apply(fromPos!!, tmpPoint)
        }

        val fromPos = fromPos
        if (toPos == null || fromPos == null || !settings.hasImageSize()) return

        // 'From' pivot point is a center of image in 'To' viewport coordinates
        fromPivotX = (fromPos.image.centerX() - toPos.viewport.left).toFloat()
        fromPivotY = (fromPos.image.centerY() - toPos.viewport.top).toFloat()

        // Computing starting zoom level
        val imageWidth: Float = settings.getImageWidth().toFloat()
        val imageHeight: Float = settings.getImageHeight().toFloat()
        val zoomW = if (imageWidth == 0f) 1f else fromPos.image.width() / imageWidth
        val zoomH = if (imageHeight == 0f) 1f else fromPos.image.height() / imageHeight
        val zoom = max(zoomW, zoomH)

        // Computing 'From' image in 'To' viewport coordinates.
        // If 'To' image has different aspect ratio it will be centered within the 'From' image.
        val fromX = fromPos.image.centerX() - 0.5f * imageWidth * zoom - toPos.viewport.left
        val fromY = fromPos.image.centerY() - 0.5f * imageHeight * zoom - toPos.viewport.top
        fromState.set(fromX, fromY, zoom, 0f)

        // 'From' clip is 'From' view rect in 'To' view coordinates
        fromClip.set(fromPos.viewport)
        fromClip.offset(-toPos.view.left.toFloat(), -toPos.view.top.toFloat())

        // 'From' bounds clip is a part of 'To' view which considered to be visible.
        // Meaning that if 'From' view is truncated in any direction this clipping should be
        // animated, otherwise it will look like part of 'From' view is instantly becoming visible.
        fromBoundsClip.set(
            (windowRect.left - toPos.view.left).toFloat(),
            (windowRect.top - toPos.view.top).toFloat(),
            (windowRect.right - toPos.view.left).toFloat(),
            (windowRect.bottom - toPos.view.top).toFloat()
        )
        fromBoundsClip.left = compareAndSetClipBound(
            fromBoundsClip.left, fromPos.view.left, fromPos.visible.left, toPos.view.left
        )
        fromBoundsClip.top = compareAndSetClipBound(
            fromBoundsClip.top, fromPos.view.top, fromPos.visible.top, toPos.view.top
        )
        fromBoundsClip.right = compareAndSetClipBound(
            fromBoundsClip.right, fromPos.view.right, fromPos.visible.right, toPos.view.left
        )
        fromBoundsClip.bottom = compareAndSetClipBound(
            fromBoundsClip.bottom, fromPos.view.bottom, fromPos.visible.bottom, toPos.view.top
        )

        isFromUpdated = true

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "'From' state updated")
        }
    }

    private fun compareAndSetClipBound(
        origBound: Float,
        viewPos: Int,
        visiblePos: Int,
        offset: Int
    ): Float {
        // Comparing allowing slack of 1 pixel
        return if (-1 <= viewPos - visiblePos && viewPos - visiblePos <= 1) {
            origBound // View is fully visible in this direction, no extra bounds
        } else {
            (visiblePos - offset).toFloat() // Returning 'From' view bound in 'To' view coordinates
        }
    }

    private inner class LocalAnimationEngine constructor(view: View) : AnimationEngine(view) {
        override fun onStep(): Boolean {
            if (!positionScroller.isFinished()) {
                positionScroller.computeScroll()
                position = positionScroller.getCurr()
                applyCurrentPosition()

                if (positionScroller.isFinished()) {
                    onAnimationStopped()
                }

                return true
            }
            return false
        }
    }

    interface PositionUpdateListener {
        /**
         * @param position Position within range `[0, 1]`, where `0` is for
         * initial (from) position and `1` is for final (to) position.
         * @param isLeaving `false` if transitioning from initial to final position
         * (entering) or `true` for reverse transition.
         */
        fun onPositionUpdate(position: Float, isLeaving: Boolean)
    }

}