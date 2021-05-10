package kz.zhombie.kaleidoscope.internal.detectors

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.max
import kotlin.math.min

/**
 * 'Double tap and swipe' mode works bad for fast gestures. This class tries to fix this issue.
 */
class ScaleGestureDetectorFixed constructor(
    context: Context,
    listener: OnScaleGestureListener
) : ScaleGestureDetector(context, listener) {

    private var currY = 0f
    private var prevY = 0f

    init {
        warmUpScaleDetector()
    }

    /**
     * Scale detector is a little buggy when first time scale is occurred.
     * So we will feed it with fake motion event to warm it up.
     */
    private fun warmUpScaleDetector() {
        val time = System.currentTimeMillis()
        val event = MotionEvent.obtain(time, time, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        onTouchEvent(event)
        event.recycle()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onTouchEvent(event)

        val result = super.onTouchEvent(event)

        prevY = currY
        currY = event.y

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            prevY = event.y
        }

        return result
    }

    private fun isInDoubleTapMode(): Boolean {
        // Indirectly determine double tap mode
        return isQuickScaleEnabled && currentSpan == currentSpanY
    }

    override fun getScaleFactor(): Float {
        val factor = super.getScaleFactor()

        return if (isInDoubleTapMode()) {
            // We will filter buggy factors which may appear when crossing focus point.
            // We will also filter factors which are too far from 1, to make scaling smoother.
            if (currY > prevY && factor > 1f || currY < prevY && factor < 1f) {
                max(0.8f, min(factor, 1.25f))
            } else {
                1f
            }
        } else {
            factor
        }
    }

}