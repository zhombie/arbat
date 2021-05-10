package kz.zhombie.kaleidoscope.animation

import android.graphics.Matrix
import android.graphics.Matrix.ScaleToFit
import android.graphics.RectF
import android.widget.ImageView.ScaleType
import kotlin.math.min

internal object ImageViewHelper {

    private val tmpSrc = RectF()
    private val tmpDst = RectF()

    /**
     * Helper method to calculate drawing matrix. Based on ImageView source code.
     */
    fun applyScaleType(
        type: ScaleType,
        dWidth: Int, dHeight: Int,
        vWidth: Int, vHeight: Int,
        imageMatrix: Matrix?, outMatrix: Matrix
    ) {
        if (ScaleType.CENTER == type) {
            // Center bitmap in view, no scaling.
            outMatrix.setTranslate((vWidth - dWidth) * 0.5F, (vHeight - dHeight) * 0.5F)
        } else if (ScaleType.CENTER_CROP == type) {
            val scale: Float
            var dx = 0F
            var dy = 0F

            if (dWidth * vHeight > vWidth * dHeight) {
                scale = vHeight.toFloat() / dHeight.toFloat()
                dx = (vWidth - dWidth * scale) * 0.5F
            } else {
                scale = vWidth.toFloat() / dWidth.toFloat()
                dy = (vHeight - dHeight * scale) * 0.5F
            }

            outMatrix.setScale(scale, scale)
            outMatrix.postTranslate(dx, dy)
        } else if (ScaleType.CENTER_INSIDE == type) {
            val scale = if (dWidth <= vWidth && dHeight <= vHeight) {
                1.0F
            } else {
                min(vWidth.toFloat() / dWidth.toFloat(), vHeight.toFloat() / dHeight.toFloat())
            }

            val dx = (vWidth - dWidth * scale) * 0.5F
            val dy = (vHeight - dHeight * scale) * 0.5F

            outMatrix.setScale(scale, scale)
            outMatrix.postTranslate(dx, dy)
        } else {
            val scaleToFit = scaleTypeToScaleToFit(type)
            if (scaleToFit == null) {
                outMatrix.set(imageMatrix)
            } else {
                // Generate the required transform.
                tmpSrc[0F, 0F, dWidth.toFloat()] = dHeight.toFloat()
                tmpDst[0F, 0F, vWidth.toFloat()] = vHeight.toFloat()
                outMatrix.setRectToRect(tmpSrc, tmpDst, scaleToFit)
            }
        }
    }

    private fun scaleTypeToScaleToFit(type: ScaleType): ScaleToFit? {
        return when (type) {
            ScaleType.FIT_XY -> ScaleToFit.FILL
            ScaleType.FIT_START -> ScaleToFit.START
            ScaleType.FIT_CENTER -> ScaleToFit.CENTER
            ScaleType.FIT_END -> ScaleToFit.END
            else -> null
        }
    }

}