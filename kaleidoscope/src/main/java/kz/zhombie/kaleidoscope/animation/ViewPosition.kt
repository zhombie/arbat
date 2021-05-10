package kz.zhombie.kaleidoscope.animation

import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import java.util.regex.Pattern

/**
 * Helper class to compute and store view position used for transitions.
 *
 * It consists of [view] rectangle, [viewport] rectangle (view rectangle minus
 * padding), [visible] rectangle (part of viewport which is visible on screen)
 * and [image] rectangle (position of the underlying image taking into account
 * [ImageView.getScaleType], or same as [viewport] if view is not an
 * [ImageView] or if [ImageView.getDrawable] is `null`).
 * All positions are in screen coordinates.
 *
 * To create instance of this class use [from] static method. But note, that view
 * should already be laid out and have correct [View.getWidth] and [View.getHeight]
 * values.
 *
 * You can also serialize and deserialize this class to string using [pack] and
 * [unpack] methods. This can be useful to pass view position between activities.
 */
// Public API (fields and methods)
class ViewPosition {
    val view: Rect
    val viewport: Rect
    val visible: Rect
    val image: Rect

    private constructor() {
        view = Rect()
        viewport = Rect()
        visible = Rect()
        image = Rect()
    }

    private constructor(view: Rect, viewport: Rect, visible: Rect, image: Rect) {
        this.view = view
        this.viewport = viewport
        this.visible = visible
        this.image = image
    }

    companion object {
        private const val DELIMITER = "#"
        private val SPLIT_PATTERN = Pattern.compile(DELIMITER)

        private val tmpLocation = IntArray(2)
        private val tmpMatrix = Matrix()
        private val tmpSrc = RectF()
        private val tmpDst = RectF()

        private val tmpViewRect = Rect()

        fun newInstance(): ViewPosition {
            return ViewPosition()
        }

        /**
         * Computes and returns view position. Note, that view should be already attached and laid out
         * before calling this method.
         *
         * @param view View for which we want to get on-screen location
         * @return View position
         */
        fun from(view: View): ViewPosition {
            val position = ViewPosition()
            position.init(view)
            return position
        }

        /**
         * Computes view position and stores it in given [position]. Note, that view should be already
         * attached and laid out before calling this method.
         *
         * @param position Output position
         * @param view View for which we want to get on-screen location
         * @return true if view position is changed, false otherwise
         */
        fun apply(position: ViewPosition, view: View): Boolean {
            return position.init(view)
        }

        /**
         * Computes minimal view position for given point.
         *
         * @param position Output view position
         * @param point Target point
         */
        fun apply(position: ViewPosition, point: Point) {
            position.view[point.x, point.y, point.x + 1] = point.y + 1
            position.viewport.set(position.view)
            position.visible.set(position.view)
            position.image.set(position.view)
        }

        /**
         * Restores ViewPosition from the string created by [.pack] method.
         *
         * @param str Serialized position string
         * @return De-serialized position
         */
        fun unpack(str: String): ViewPosition {
            val parts = TextUtils.split(str, SPLIT_PATTERN)
            require(parts.size == 4) { "Wrong ViewPosition string: $str" }
            val view = Rect.unflattenFromString(parts[0])
            val viewport = Rect.unflattenFromString(parts[1])
            val visible = Rect.unflattenFromString(parts[2])
            val image = Rect.unflattenFromString(parts[3])
            require(!(view == null || viewport == null || visible == null || image == null)) { "Wrong ViewPosition string: $str" }
            return ViewPosition(view, viewport, visible, image)
        }
    }

    // Public API
    fun set(position: ViewPosition) {
        view.set(position.view)
        viewport.set(position.viewport)
        visible.set(position.visible)
        image.set(position.image)
    }

    /**
     * @param targetView View for which we want to get on-screen location
     * @return true if view position is changed, false otherwise
     */
    private fun init(targetView: View): Boolean {
        // If view is not attached then we can't get it's position
        if (targetView.windowToken == null) {
            return false
        }

        tmpViewRect.set(view)

        targetView.getLocationInWindow(tmpLocation)

        view[0, 0, targetView.width] = targetView.height
        view.offset(tmpLocation[0], tmpLocation[1])

        viewport[targetView.paddingLeft, targetView.paddingTop, targetView.width - targetView.paddingRight] = targetView.height - targetView.paddingBottom
        viewport.offset(tmpLocation[0], tmpLocation[1])

        val isVisible = targetView.getGlobalVisibleRect(visible)
        if (!isVisible) {
            // Assuming we are starting from center of invisible view
            visible[view.centerX(), view.centerY(), view.centerX() + 1] = view.centerY() + 1
        }

        if (targetView is ImageView) {
            val drawable = targetView.drawable
            if (drawable == null) {
                image.set(viewport)
            } else {
                val drawableWidth = drawable.intrinsicWidth
                val drawableHeight = drawable.intrinsicHeight

                // Getting image position within the view
                ImageViewHelper.applyScaleType(
                    targetView.scaleType,
                    drawableWidth, drawableHeight, viewport.width(), viewport.height(),
                    targetView.imageMatrix, tmpMatrix
                )

                tmpSrc[0f, 0f, drawableWidth.toFloat()] = drawableHeight.toFloat()
                tmpMatrix.mapRect(tmpDst, tmpSrc)

                // Calculating image position on screen
                image.left = viewport.left + tmpDst.left.toInt()
                image.top = viewport.top + tmpDst.top.toInt()
                image.right = viewport.left + tmpDst.right.toInt()
                image.bottom = viewport.top + tmpDst.bottom.toInt()
            }
        } else {
            image.set(viewport)
        }

        return tmpViewRect != view
    }

    /**
     * Packs this ViewPosition into string, which can be passed i.e. between activities.
     *
     * @return Serialized position
     * @see [unpack]
     */
    fun pack(): String {
        val viewStr = view.flattenToString()
        val viewportStr = viewport.flattenToString()
        val visibleStr = visible.flattenToString()
        val imageStr = image.flattenToString()
        return TextUtils.join(DELIMITER, arrayOf(viewStr, viewportStr, visibleStr, imageStr))
    }

}