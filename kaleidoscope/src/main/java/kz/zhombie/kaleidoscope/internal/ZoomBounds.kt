package kz.zhombie.kaleidoscope.internal

import android.graphics.Matrix
import android.graphics.RectF
import kz.zhombie.kaleidoscope.Settings
import kz.zhombie.kaleidoscope.State
import kz.zhombie.kaleidoscope.utils.MathUtils
import kotlin.math.max
import kotlin.math.min

/**
 * Encapsulates logic related to movement bounds restriction. It will also apply image gravity
 * provided by [Settings.getGravity] method.
 *
 * Movement bounds can be represented using regular rectangle most of the time. But if fit method
 * is set to [Settings.FitMethod.OUTSIDE] and image has rotation != 0 then movement bounds will be
 * a rotated rectangle. That will complicate restrictions logic a bit.
 */
class ZoomBounds constructor(private val settings: Settings) {

    companion object {
        // Temporary objects
        private val tmpMatrix = Matrix()
        private val tmpRectF: RectF = RectF()
    }

    // State bounds parameters
    private var minZoom: Float = 0F
    private var maxZoom: Float = 0F
    private var fitZoom: Float = 0F

    fun getMinZoom(): Float = minZoom

    fun getMaxZoom(): Float = maxZoom

    fun getFitZoom(): Float = fitZoom

    /**
     * Calculates min, max and "fit" zoom levels for given state and according to current settings.
     *
     * @param state State for which to calculate zoom bounds.
     * @return Current zoom bounds object for calls chaining.
     */
    fun set(state: State): ZoomBounds {
        var imageWidth: Float = settings.getImageWidth().toFloat()
        var imageHeight: Float = settings.getImageHeight().toFloat()

        var areaWidth: Float = settings.getMovementAreaWidth().toFloat()
        var areaHeight: Float = settings.getMovementAreaHeight().toFloat()

        if (imageWidth == 0F || imageHeight == 0F || areaWidth == 0F || areaHeight == 0F) {
            fitZoom = 1F
            maxZoom = fitZoom
            minZoom = maxZoom
            return this
        }

        minZoom = settings.getMinZoom()
        maxZoom = settings.getMaxZoom()

        val rotation: Float = state.getRotation()

        if (!State.equals(rotation, 0F)) {
            if (settings.getFitMethod() === Settings.FitMethod.OUTSIDE) {
                // Computing movement area size taking rotation into account. We need to inverse
                // rotation, since it will be applied to the area, not to the image itself.
                tmpMatrix.setRotate(-rotation)
                tmpRectF.set(0F, 0F, areaWidth, areaHeight)
                tmpMatrix.mapRect(tmpRectF)
                areaWidth = tmpRectF.width()
                areaHeight = tmpRectF.height()
            } else {
                // Computing image bounding size taking rotation into account.
                tmpMatrix.setRotate(rotation)
                tmpRectF.set(0F, 0F, imageWidth, imageHeight)
                tmpMatrix.mapRect(tmpRectF)
                imageWidth = tmpRectF.width()
                imageHeight = tmpRectF.height()
            }
        }

        fitZoom = when (settings.getFitMethod()) {
            Settings.FitMethod.HORIZONTAL -> areaWidth / imageWidth
            Settings.FitMethod.VERTICAL -> areaHeight / imageHeight
            Settings.FitMethod.INSIDE -> min(areaWidth / imageWidth, areaHeight / imageHeight)
            Settings.FitMethod.OUTSIDE -> max(areaWidth / imageWidth, areaHeight / imageHeight)
            Settings.FitMethod.NONE -> if (minZoom > 0F) minZoom else 1F
        }

        if (minZoom <= 0F) {
            minZoom = fitZoom
        }
        if (maxZoom <= 0F) {
            maxZoom = fitZoom
        }

        if (fitZoom > maxZoom) {
            if (settings.isFillViewport()) {
                // zooming to fill entire viewport
                maxZoom = fitZoom
            } else {
                // restricting fit zoom
                fitZoom = maxZoom
            }
        }
        // Now we have: fitZoom <= maxZoom

        if (minZoom > maxZoom) {
            minZoom = maxZoom
        }
        // Now we have: minZoom <= maxZoom

        if (fitZoom < minZoom) {
            if (settings.isFillViewport()) {
                // zooming to fill entire viewport
                minZoom = fitZoom
            } else {
                // restricting fit zoom
                fitZoom = minZoom
            }
        }
        // Now we have: minZoom <= fitZoom <= maxZoom
        return this
    }

    fun restrict(zoom: Float, extraZoom: Float): Float {
        return MathUtils.restrict(zoom, minZoom / extraZoom, maxZoom * extraZoom)
    }

}