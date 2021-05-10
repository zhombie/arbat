package kz.zhombie.kaleidoscope.internal.detectors

import android.view.View
import android.content.Context
import android.view.MotionEvent
import kz.zhombie.kaleidoscope.internal.detectors.RotationGestureDetector.OnRotationGestureListener
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Detects rotation transformation gestures using the supplied [MotionEvent]s.
 * The [OnRotationGestureListener] callback will notify users when a particular
 * gesture event has occurred.
 *
 * To use this class:
 *
 * Create an instance of the [RotationGestureDetector] for your [View]
 * In the [View.onTouchEvent] method ensure you call
 * [onTouchEvent]. The methods defined in your callback will be executed
 * when the events occur.
 *
 * Creates a RotationGestureDetector with the supplied listener.
 * You may only use this constructor from a [android.os.Looper] thread.
 *
 * @param context the application's context
 * @param listener the listener invoked for all the callbacks.
 */
class RotationGestureDetector constructor(
    private val context: Context,
    private val listener: OnRotationGestureListener
) {

    companion object {
        private const val ROTATION_SLOP = 5f
    }

    private var focusX = 0f
    private var focusY = 0f
    private var initialAngle = 0f
    private var currAngle = 0f
    private var prevAngle = 0f
    private var isInProgress = false
    private var isGestureAccepted = false

    /**
     * Accepts MotionEvents and dispatches events to a [OnRotationGestureListener]
     * when appropriate.
     *
     * Applications should pass a complete and consistent event stream to this method.
     * A complete and consistent event stream involves all MotionEvents from the initial
     * [MotionEvent.ACTION_DOWN] to the final [MotionEvent.ACTION_UP] or [MotionEvent.ACTION_CANCEL].
     *
     * @param event The event to process
     * @return true if the event was processed and the detector wants to receive the
     * rest of the MotionEvents in this event stream.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                cancelRotation()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    // Second finger is placed
                    currAngle = computeRotation(event)
                    prevAngle = currAngle
                    initialAngle = prevAngle
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2 && (!isInProgress || isGestureAccepted)) {
                    // Moving 2 or more fingers on the screen
                    currAngle = computeRotation(event)
                    focusX = 0.5f * (event.getX(1) + event.getX(0))
                    focusY = 0.5f * (event.getY(1) + event.getY(0))
                    val isAlreadyStarted = isInProgress
                    tryStartRotation()
                    val isAccepted = !isAlreadyStarted || processRotation()
                    if (isAccepted) {
                        prevAngle = currAngle
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) {
                    // Only one finger is left
                    cancelRotation()
                }
            }
            else -> {
            }
        }
        return true
    }

    private fun tryStartRotation() {
        if (isInProgress || abs(initialAngle - currAngle) < ROTATION_SLOP) return
        isInProgress = true
        isGestureAccepted = listener.onRotationBegin(this)
    }

    private fun cancelRotation() {
        if (!isInProgress) return
        isInProgress = false
        if (isGestureAccepted) {
            listener.onRotationEnd(this)
            isGestureAccepted = false
        }
    }

    private fun processRotation(): Boolean {
        return isInProgress && isGestureAccepted && listener.onRotate(this)
    }

    private fun computeRotation(event: MotionEvent): Float {
        return Math.toDegrees(
            atan2(
                (event.getY(1) - event.getY(0)).toDouble(),
                (event.getX(1) - event.getX(0)).toDouble()
            )
        ).toFloat()
    }

    /**
     * @return `true` if a rotation gesture is in progress
     */
    // To keep similar to standard ScaleGestureDetector
    fun isInProgress(): Boolean {
        return isInProgress
    }

    /**
     * Get the X coordinate of the current gesture's focal point. If a gesture is in progress,
     * the focal point is between each of the pointers forming the gesture.
     *
     * If [isInProgress] would return false, the result of this function is undefined.
     *
     * @return X coordinate of the focal point in pixels.
     */
    fun getFocusX(): Float {
        return focusX
    }

    /**
     * Get the Y coordinate of the current gesture's focal point. If a gesture is in progress,
     * the focal point is between each of the pointers forming the gesture.
     *
     * If [isInProgress] would return false, the result of this function is undefined.
     *
     * @return Y coordinate of the focal point in pixels.
     */
    fun getFocusY(): Float {
        return focusY
    }

    /**
     * Return the rotation delta in degrees from the previous rotation event to the current event.
     *
     * @return The current rotation delta in degrees.
     */
    fun getRotationDelta(): Float {
        return currAngle - prevAngle
    }

    /**
     * The listener for receiving notifications when gestures occur.
     *
     * An application will receive events in the following order:
     *
     *  * One [OnRotationGestureListener.onRotationBegin]
     *  * Zero or more [OnRotationGestureListener.onRotate]
     *  * One [OnRotationGestureListener.onRotationEnd]
     *
     */
    interface OnRotationGestureListener {
        /**
         * Responds to rotation events for a gesture in progress. Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         * @return Whether or not the detector should consider this event as handled. If an event
         * was not handled, the detector will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example, only wants to update
         * rotation angle if the change is greater than 0.01.
         */
        fun onRotate(detector: RotationGestureDetector): Boolean

        /**
         * Responds to the beginning of a rotation gesture. Reported by new pointers going down.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         * @return Whether or not the detector should continue recognizing this gesture.
         * For example, if a gesture is beginning with a focal point outside of a region where
         * it makes sense, onRotationBegin() may return false to ignore the rest of the gesture.
         */
        fun onRotationBegin(detector: RotationGestureDetector): Boolean

        /**
         * Responds to the end of a rotation gesture. Reported by existing pointers going up.
         *
         *
         * Once a rotation has ended, [RotationGestureDetector.getFocusX] and
         * [RotationGestureDetector.getFocusY] will return focal point of the pointers
         * remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to retrieve extended info
         * about event state.
         */
        fun onRotationEnd(detector: RotationGestureDetector)
    }

}