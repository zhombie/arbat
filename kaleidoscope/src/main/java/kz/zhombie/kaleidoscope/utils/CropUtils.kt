package kz.zhombie.kaleidoscope.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kz.zhombie.kaleidoscope.GestureController
import kz.zhombie.kaleidoscope.Settings
import kz.zhombie.kaleidoscope.State
import kotlin.math.roundToInt

class CropUtils private constructor() {

    companion object {
        /**
         * Crops image drawable into bitmap according to current image position.
         *
         * @param drawable Image drawable
         * @param controller Image controller
         * @return Cropped image part
         */
        fun crop(drawable: Drawable?, controller: GestureController): Bitmap? {
            if (drawable == null) return null

            controller.stopAllAnimations()
            controller.updateState() // Applying state restrictions

            val settings: Settings = controller.settings
            val state: State = controller.state
            val zoom: Float = state.getZoom()

            // Computing crop size for base zoom level (zoom == 1)
            val width = (settings.getMovementAreaWidth() / zoom).roundToInt()
            val height = (settings.getMovementAreaHeight() / zoom).roundToInt()

            // Crop area coordinates within viewport
            val pos = Rect()
            GravityUtils.getMovementAreaPosition(settings, pos)

            val matrix = Matrix()
            state.get(matrix)
            // Scaling to base zoom level (zoom == 1)
            matrix.postScale(1f / zoom, 1f / zoom, pos.left.toFloat(), pos.top.toFloat())
            // Positioning crop area
            matrix.postTranslate(-pos.left.toFloat(), -pos.top.toFloat())

            return try {
                // Draw drawable into bitmap
                val dst: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(dst)
                canvas.concat(matrix)
                drawable.draw(canvas)

                dst
            } catch (e: OutOfMemoryError) {
                null // Not enough memory for cropped bitmap
            }
        }
    }

}