package kz.zhombie.kaleidoscope.utils

import android.graphics.Matrix
import android.graphics.RectF
import androidx.annotation.Size
import kz.zhombie.kaleidoscope.State
import java.lang.Float.isNaN
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MathUtils private constructor() {

    companion object {
        private val tmpMatrix = Matrix()
        private val tmpMatrixInverse = Matrix()

        /**
         * Keeps value within provided bounds.
         *
         * @param value Value to be restricted
         * @param minValue Min value
         * @param maxValue Max value
         * @return Restricted value
         */
        fun restrict(value: Float, minValue: Float, maxValue: Float): Float {
            return max(minValue, min(value, maxValue))
        }

        /**
         * Interpolates from start value to the end one by given factor (from 0 to 1).
         *
         * @param start Start value
         * @param end End value
         * @param factor Factor
         * @return Interpolated value
         */
        fun interpolate(start: Float, end: Float, factor: Float): Float {
            return start + (end - start) * factor
        }

        /**
         * Interpolates from start rect to the end rect by given factor (from 0 to 1),
         * storing result into out rect.
         *
         * @param out Interpolated rectangle (output)
         * @param start Start rectangle
         * @param end End rectangle
         * @param factor Factor
         */
        fun interpolate(out: RectF, start: RectF, end: RectF, factor: Float) {
            out.left = interpolate(start.left, end.left, factor)
            out.top = interpolate(start.top, end.top, factor)
            out.right = interpolate(start.right, end.right, factor)
            out.bottom = interpolate(start.bottom, end.bottom, factor)
        }

        /**
         * Interpolates from start state to end state by given factor (from 0 to 1),
         * storing result into out state. All operations (translation, zoom, rotation) will be
         * performed within specified pivot points, assuming start and end pivot points represent
         * same physical point on the image.
         *
         * @param out Interpolated state (output)
         * @param start Start state
         * @param startPivotX Pivot point's X coordinate in start state coordinates
         * @param startPivotY Pivot point's Y coordinate in start state coordinates
         * @param end End state
         * @param endPivotX Pivot point's X coordinate in end state coordinates
         * @param endPivotY Pivot point's Y coordinate in end state coordinates
         * @param factor Factor
         */
        fun interpolate(
            out: State,
            start: State,
            startPivotX: Float,
            startPivotY: Float,
            end: State,
            endPivotX: Float,
            endPivotY: Float,
            factor: Float
        ) {
            out.set(start)

            if (!State.equals(start.getZoom(), end.getZoom())) {
                val zoom = interpolate(start.getZoom(), end.getZoom(), factor)
                out.zoomTo(zoom, startPivotX, startPivotY)
            }

            // Getting rotations
            val startRotation: Float = start.getRotation()
            val endRotation: Float = end.getRotation()

            var rotation = Float.NaN

            // Choosing shortest path to interpolate
            if (abs(startRotation - endRotation) <= 180F) {
                if (!State.equals(startRotation, endRotation)) {
                    rotation = interpolate(startRotation, endRotation, factor)
                }
            } else {
                // Keeping rotation positive
                val startRotationPositive = if (startRotation < 0F) startRotation + 360F else startRotation
                val endRotationPositive = if (endRotation < 0F) endRotation + 360F else endRotation
                if (!State.equals(startRotationPositive, endRotationPositive)) {
                    rotation = interpolate(startRotationPositive, endRotationPositive, factor)
                }
            }

            if (!isNaN(rotation)) {
                out.rotateTo(rotation, startPivotX, startPivotY)
            }

            val dx = interpolate(0F, endPivotX - startPivotX, factor)
            val dy = interpolate(0F, endPivotY - startPivotY, factor)
            out.translateBy(dx, dy)
        }

        fun computeNewPosition(@Size(2) point: FloatArray, initialState: State, finalState: State) {
            initialState.get(tmpMatrix)
            tmpMatrix.invert(tmpMatrixInverse)
            tmpMatrixInverse.mapPoints(point)
            finalState.get(tmpMatrix)
            tmpMatrix.mapPoints(point)
        }
    }

}