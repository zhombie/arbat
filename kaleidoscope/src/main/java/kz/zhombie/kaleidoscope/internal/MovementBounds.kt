package kz.zhombie.kaleidoscope.internal

import android.graphics.*
import kz.zhombie.kaleidoscope.Settings
import kz.zhombie.kaleidoscope.State
import kz.zhombie.kaleidoscope.utils.GravityUtils
import kz.zhombie.kaleidoscope.utils.MathUtils

/**
 * Encapsulates logic related to movement bounds restriction. It will also apply image gravity
 * provided by [Settings.getGravity] method.
 *
 * Movement bounds can be represented using regular rectangle most of the time. But if fit method
 * is set to [Settings.FitMethod.OUTSIDE] and image has rotation != 0 then movement bounds will be
 * a rotated rectangle. That will complicate restrictions logic a bit.
 */
class MovementBounds constructor(private val settings: Settings) {

    companion object {
        // Temporary objects
        private val tmpMatrix = Matrix()
        private val tmpPointArr = FloatArray(2)
        private val tmpPoint = Point()
        private val tmpRect = Rect()
        private val tmpRectF: RectF = RectF()
    }

    // State bounds parameters
    private val bounds: RectF = RectF()
    private var boundsRotation = 0f
    private var boundsPivotX = 0f
    private var boundsPivotY = 0f

    /**
     * Calculating bounds for x &amp; y values to keep image within
     * viewport and taking image gravity into account (see [Settings.setGravity]).
     *
     * @param state State for which to calculate movement bounds.
     * @return Current movement bounds object for calls chaining.
     */
    fun set(state: State): MovementBounds {
        val area: RectF = tmpRectF
        GravityUtils.getMovementAreaPosition(settings, tmpRect)
        area.set(tmpRect)

        val position = tmpRect

        if (settings.getFitMethod() === Settings.FitMethod.OUTSIDE) {
            // For OUTSIDE fit method we will rotate area rect instead of image rect,
            // that will help us correctly fit movement area inside image rect
            boundsRotation = state.getRotation()
            boundsPivotX = area.centerX()
            boundsPivotY = area.centerY()

            if (!State.equals(boundsRotation, 0F)) {
                tmpMatrix.setRotate(-boundsRotation, boundsPivotX, boundsPivotY)
                tmpMatrix.mapRect(area)
            }
        } else {
            boundsRotation = 0F
            boundsPivotY = 0F
            boundsPivotX = boundsPivotY
        }

        state.get(tmpMatrix)

        if (!State.equals(boundsRotation, 0F)) {
            // Removing image rotation
            tmpMatrix.postRotate(-boundsRotation, boundsPivotX, boundsPivotY)
        }
        GravityUtils.getImagePosition(tmpMatrix, settings, position)

        when (settings.getBoundsType()) {
            Settings.Bounds.NORMAL -> calculateNormalBounds(area, position)
            Settings.Bounds.INSIDE -> calculateInsideBounds(area, position)
            Settings.Bounds.OUTSIDE -> calculateOutsideBounds(area, position)
            Settings.Bounds.PIVOT -> calculatePivotBounds(position)
            Settings.Bounds.NONE ->
                // Infinite bounds with overflow prevention
                bounds.set(
                    (Int.MIN_VALUE shr 2).toFloat(),
                    (Int.MIN_VALUE shr 2).toFloat(),
                    (Int.MAX_VALUE shr 2).toFloat(),
                    (Int.MAX_VALUE shr 2).toFloat()
                )
        }

        // We should also adjust bounds position, since top-left corner of rotated image rectangle
        // will be somewhere on the edge of non-rotated bounding rectangle.
        // Note: for OUTSIDE fit method image rotation was skipped above, so we will not need
        // to adjust bounds here.
        if (settings.getFitMethod() !== Settings.FitMethod.OUTSIDE) {
            state.get(tmpMatrix)

            val imageRect: RectF = tmpRectF
            imageRect.set(0F, 0F, settings.getImageWidth().toFloat(), settings.getImageHeight().toFloat())
            tmpMatrix.mapRect(imageRect)

            tmpPointArr[0] = 0F
            tmpPointArr[1] = 0F
            tmpMatrix.mapPoints(tmpPointArr)

            bounds.offset(tmpPointArr[0] - imageRect.left, tmpPointArr[1] - imageRect.top)
        }
        return this
    }

    private fun calculateNormalBounds(area: RectF, position: Rect) {
        // horizontal bounds
        if (area.width() < position.width()) {
            // image is bigger than movement area -> restricting image movement with moving area
            bounds.left = area.left - (position.width() - area.width())
            bounds.right = area.left
        } else {
            // image is smaller than viewport -> positioning image according to calculated gravity
            // and restricting image movement in this direction
            bounds.right = position.left.toFloat()
            bounds.left = bounds.right
        }

        // vertical bounds
        if (area.height() < position.height()) {
            // image is bigger than viewport -> restricting image movement with viewport bounds
            bounds.top = area.top - (position.height() - area.height())
            bounds.bottom = area.top
        } else {
            // image is smaller than viewport -> positioning image according to calculated gravity
            // and restricting image movement in this direction
            bounds.bottom = position.top.toFloat()
            bounds.top = position.top.toFloat()
        }
    }

    private fun calculateInsideBounds(area: RectF, position: Rect) {
        // horizontal bounds
        if (area.width() < position.width()) {
            // image is bigger than movement area -> restricting image movement with moving area
            bounds.left = area.left - (position.width() - area.width())
            bounds.right = area.left
        } else {
            // image is smaller than viewport -> allow image to move inside the area
            bounds.left = area.left
            bounds.right = area.right - position.width()
        }

        // vertical bounds
        if (area.height() < position.height()) {
            // image is bigger than viewport -> restricting image movement with viewport bounds
            bounds.top = area.top - (position.height() - area.height())
            bounds.bottom = area.top
        } else {
            // image is smaller than viewport -> allow image to move inside the area
            bounds.top = area.top
            bounds.bottom = area.bottom - position.height()
        }
    }

    private fun calculateOutsideBounds(area: RectF, position: Rect) {
        bounds.left = area.left - position.width()
        bounds.right = area.right
        bounds.top = area.top - position.height()
        bounds.bottom = area.bottom
    }

    private fun calculatePivotBounds(position: Rect) {
        GravityUtils.getDefaultPivot(settings, tmpPoint)
        tmpPointArr[0] = tmpPoint.x.toFloat()
        tmpPointArr[1] = tmpPoint.y.toFloat()

        if (!State.equals(boundsRotation, 0F)) {
            tmpMatrix.setRotate(-boundsRotation, boundsPivotX, boundsPivotY)
            tmpMatrix.mapPoints(tmpPointArr)
        }

        bounds.left = tmpPointArr[0] - position.width()
        bounds.right = tmpPointArr[0]
        bounds.top = tmpPointArr[1] - position.height()
        bounds.bottom = tmpPointArr[1]
    }

    fun extend(x: Float, y: Float) {
        tmpPointArr[0] = x
        tmpPointArr[1] = y

        if (boundsRotation != 0f) {
            // Rotating given point so we can add it to bounds
            tmpMatrix.setRotate(-boundsRotation, boundsPivotX, boundsPivotY)
            tmpMatrix.mapPoints(tmpPointArr)
        }

        bounds.union(tmpPointArr[0], tmpPointArr[1])
    }

    fun getExternalBounds(out: RectF) {
        if (boundsRotation == 0f) {
            out.set(bounds)
        } else {
            tmpMatrix.setRotate(boundsRotation, boundsPivotX, boundsPivotY)
            tmpMatrix.mapRect(out, bounds)
        }
    }

    /**
     * Restricts x &amp; y coordinates to current bounds (as calculated in [.set]).
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param extraX Extra area bounds (horizontal)
     * @param extraY Extra area bounds (vertical)
     * @param out Output rectangle
     */
    fun restrict(x: Float, y: Float, extraX: Float, extraY: Float, out: PointF) {
        tmpPointArr[0] = x
        tmpPointArr[1] = y

        if (boundsRotation != 0f) {
            // Rotating given point so we can apply rectangular bounds.
            tmpMatrix.setRotate(-boundsRotation, boundsPivotX, boundsPivotY)
            tmpMatrix.mapPoints(tmpPointArr)
        }

        // Applying restrictions
        tmpPointArr[0] = MathUtils.restrict(
            tmpPointArr[0],
            bounds.left - extraX,
            bounds.right + extraX
        )

        tmpPointArr[1] = MathUtils.restrict(
            tmpPointArr[1],
            bounds.top - extraY,
            bounds.bottom + extraY
        )

        if (boundsRotation != 0F) {
            // Rotating restricted point back to original coordinates
            tmpMatrix.setRotate(boundsRotation, boundsPivotX, boundsPivotY)
            tmpMatrix.mapPoints(tmpPointArr)
        }

        out.set(tmpPointArr[0], tmpPointArr[1])
    }

    fun restrict(x: Float, y: Float, out: PointF) {
        restrict(x, y, 0F, 0F, out)
    }

}