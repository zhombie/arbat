package kz.zhombie.kaleidoscope.utils

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.view.View
import kz.zhombie.kaleidoscope.State
import kz.zhombie.kaleidoscope.views.interfaces.ClipView

/**
 * Helper class to implement view clipping (with [ClipView] interface).
 *
 * Usage: call [clipView] method when needed and override
 * [View.draw] method:
 *
 * fun draw(canvas: Canvas) {
 *     clipHelper.onPreDraw(canvas)
 *     super.draw(canvas)
 *     clipHelper.onPostDraw(canvas)
 * }
 */
class ClipHelper constructor(private val view: View) : ClipView {

    companion object {
        private val tmpMatrix = Matrix()
    }

    private var isClipping = false
    private val clipRect: RectF = RectF()
    private var clipRotation = 0f
    private val clipBounds: RectF = RectF()
    private val clipBoundsOld: RectF = RectF()

    override fun clipView(rect: RectF?, rotation: Float) {
        if (rect == null) {
            if (isClipping) {
                isClipping = false
                view.invalidate()
            }
        } else {
            // Setting previous clip rect
            if (isClipping) {
                clipBoundsOld.set(clipBounds)
            } else {
                clipBoundsOld.set(0f, 0f, view.width.toFloat(), view.height.toFloat())
            }

            isClipping = true

            clipRect.set(rect)
            clipRotation = rotation

            // Computing upper bounds of clipping rect after rotation (if any)
            clipBounds.set(clipRect)
            if (!State.equals(rotation, 0f)) {
                tmpMatrix.setRotate(rotation, clipRect.centerX(), clipRect.centerY())
                tmpMatrix.mapRect(clipBounds)
            }

            view.invalidate()
        }
    }

    fun onPreDraw(canvas: Canvas) {
        if (isClipping) {
            canvas.save()
            if (State.equals(clipRotation, 0F)) {
                canvas.clipRect(clipRect)
            } else {
                // Note, that prior Android 4.3 (18) canvas matrix is not correctly applied to
                // clip rect, clip rect will be set to its upper bound, which is good enough for us.
                canvas.rotate(clipRotation, clipRect.centerX(), clipRect.centerY())
                canvas.clipRect(clipRect)
                canvas.rotate(-clipRotation, clipRect.centerX(), clipRect.centerY())
            }
        }
    }

    fun onPostDraw(canvas: Canvas) {
        if (isClipping) {
            canvas.restore()
        }
    }

}