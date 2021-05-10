package kz.zhombie.kaleidoscope.utils

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity
import kz.zhombie.kaleidoscope.Settings
import kz.zhombie.kaleidoscope.State
import kotlin.math.roundToInt

class GravityUtils private constructor() {

    companion object {
        private val tmpMatrix = Matrix()
        private val tmpRectF: RectF = RectF()

        private val tmpRect1 = Rect()
        private val tmpRect2 = Rect()

        /**
         * Calculates image position (scaled and rotated) within viewport area with gravity applied.
         *
         * @param state Image state
         * @param settings Image settings
         * @param out Output rectangle
         */
        fun getImagePosition(state: State, settings: Settings, out: Rect) {
            state.get(tmpMatrix)
            getImagePosition(tmpMatrix, settings, out)
        }

        /**
         * Calculates image position (scaled and rotated) within viewport area with gravity applied.
         *
         * @param matrix Image matrix
         * @param settings Image settings
         * @param out Output rectangle
         */
        fun getImagePosition(matrix: Matrix, settings: Settings, out: Rect) {
            tmpRectF.set(0F, 0F, settings.getImageWidth().toFloat(), settings.getImageHeight().toFloat())

            matrix.mapRect(tmpRectF)

            val w = tmpRectF.width().roundToInt()
            val h = tmpRectF.height().roundToInt()

            // Calculating image position basing on gravity
            tmpRect1[0, 0, settings.getViewportW()] = settings.getViewportH()
            Gravity.apply(settings.getGravity(), w, h, tmpRect1, out)
        }

        /**
         * Calculates movement area position within viewport area with gravity applied.
         *
         * @param settings Image settings
         * @param out Output rectangle
         */
        fun getMovementAreaPosition(settings: Settings, out: Rect) {
            tmpRect1[0, 0, settings.getViewportW()] = settings.getViewportH()
            Gravity.apply(
                settings.getGravity(),
                settings.getMovementAreaWidth(),
                settings.getMovementAreaHeight(),
                tmpRect1,
                out
            )
        }

        /**
         * Calculates default pivot point for scale and rotation.
         *
         * @param settings Image settings
         * @param out Output point
         */
        fun getDefaultPivot(settings: Settings, out: Point) {
            getMovementAreaPosition(settings, tmpRect2)
            Gravity.apply(settings.getGravity(), 0, 0, tmpRect2, tmpRect1)
            out[tmpRect1.left] = tmpRect1.top
        }
    }

}