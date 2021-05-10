package kz.zhombie.kaleidoscope.utils

import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator

/**
 * A simple class that animates float values.
 * Functionally similar to a [android.widget.Scroller].
 */
class FloatScroller {

    companion object {
        private const val DEFAULT_DURATION = 250L

        private fun interpolate(x1: Float, x2: Float, state: Float): Float {
            return x1 + (x2 - x1) * state
        }
    }

    private val interpolator: Interpolator

    init {
        interpolator = AccelerateDecelerateInterpolator()
    }

    private var isFinished = true

    private var startValue = 0F
    private var finalValue = 0F

    /**
     * Current value computed by [computeScroll].
     */
    private var currValue = 0F

    /**
     * The time the animation started, computed using [SystemClock.elapsedRealtime].
     */
    private var startRtc: Long = 0

    private var duration = DEFAULT_DURATION

    fun getDuration(): Long {
        return duration
    }

    fun setDuration(duration: Long) {
        this.duration = duration
    }

    /**
     * Force the finished field to a particular value.
     * Unlike [abortAnimation] the current value isn't set to the final value.
     *
     * @see android.widget.Scroller.forceFinished
     */
    fun forceFinished() {
        isFinished = true
    }

    /**
     * Aborts the animation, setting the current value to the final value.
     *
     * @see android.widget.Scroller.abortAnimation
     */
    // Public API
    fun abortAnimation() {
        isFinished = true
        currValue = finalValue
    }

    /**
     * Starts an animation from [startValue] to [finalValue].
     *
     * @param startValue Start value
     * @param finalValue Final value
     * @see android.widget.Scroller.startScroll
     */
    fun startScroll(startValue: Float, finalValue: Float) {
        isFinished = false
        startRtc = SystemClock.elapsedRealtime()

        this.startValue = startValue
        this.finalValue = finalValue
        currValue = startValue
    }

    /**
     * Computes the current value, returning true if the animation is still active and false if the
     * animation has finished.
     *
     * @return Computed scroll
     * @see android.widget.Scroller.computeScrollOffset
     */
    fun computeScroll(): Boolean {
        if (isFinished) {
            return false
        }

        val elapsed = SystemClock.elapsedRealtime() - startRtc
        if (elapsed >= duration) {
            isFinished = true
            currValue = finalValue
            return false
        }

        val time = interpolator.getInterpolation(elapsed.toFloat() / duration)
        currValue = interpolate(startValue, finalValue, time)
        return true
    }

    /**
     * @return Current state
     * @see android.widget.Scroller.isFinished
     */
    fun isFinished(): Boolean = isFinished

    /**
     * @return Starting value
     * @see android.widget.Scroller.getStartX
     */
    fun getStart(): Float = startValue

    /**
     * @return Final value
     * @see android.widget.Scroller.getFinalX
     */
    fun getFinal(): Float = finalValue

    /**
     * @return Current value
     * @see android.widget.Scroller.getCurrX
     */
    fun getCurr(): Float = currValue


}