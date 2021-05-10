package kz.zhombie.kaleidoscope

import android.graphics.Matrix
import java.lang.Float.floatToIntBits
import java.lang.Float.isNaN
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Represents 2d transformation state.
 */
// Public API (fields and methods)
class State {

    companion object {
        const val EPSILON = 0.001F

        /**
         * Compares two float values, allowing small difference (see [.EPSILON]).
         *
         * @param v1 First value
         * @param v2 Second value
         * @return True if both values are close enough to be considered as equal
         */
        fun equals(v1: Float, v2: Float): Boolean {
            return v1 >= v2 - EPSILON && v1 <= v2 + EPSILON
        }

        /**
         * Compares two float values, allowing small difference (see [.EPSILON]).
         *
         * @param v1 First value
         * @param v2 Second value
         * @return Positive int if first value is greater than second, negative int if second value
         * is greater than first or 0 if both values are close enough to be considered as equal
         */
        fun compare(v1: Float, v2: Float): Int {
            return if (v1 > v2 + EPSILON) 1 else if (v1 < v2 - EPSILON) -1 else 0
        }

        private fun nonNaN(value: Float): Float {
            require(!isNaN(value)) { "Provided float is NaN" }
            return value
        }
    }

    private val matrix = Matrix()
    private val matrixValues = FloatArray(9)

    private var x = 0F
    private var y = 0F
    private var zoom = 1F
    private var rotation = 0F

    fun getX(): Float = x

    fun getY(): Float = y

    fun getZoom(): Float = zoom

    /**
     * @return Rotation in degrees within the range [-180..180].
     */
    fun getRotation(): Float = rotation

    /**
     * @return `true` if `x == 0f && y == 0f && zoom == 1f && rotation == 0f`
     */
    fun isEmpty(): Boolean = x == 0F && y == 0F && zoom == 1F && rotation == 0F

    /**
     * Applying state to provided matrix. Matrix will contain translation, scale and rotation.
     *
     * @param matrix Target matrix
     */
    fun get(matrix: Matrix) {
        matrix.set(this.matrix)
    }

    fun translateBy(dx: Float, dy: Float) {
        matrix.postTranslate(nonNaN(dx), nonNaN(dy))
        updateFromMatrix(updateZoom = false, updateRotation = false) // only translation is changed
    }

    fun translateTo(x: Float, y: Float) {
        matrix.postTranslate(-this.x + nonNaN(x), -this.y + nonNaN(y))
        updateFromMatrix(updateZoom = false, updateRotation = false) // only translation is changed
    }

    fun zoomBy(factor: Float, pivotX: Float, pivotY: Float) {
        nonNaN(factor)
        matrix.postScale(factor, factor, nonNaN(pivotX), nonNaN(pivotY))
        updateFromMatrix(updateZoom = true, updateRotation = false) // zoom & translation are changed
    }

    fun zoomTo(zoom: Float, pivotX: Float, pivotY: Float) {
        nonNaN(zoom)
        matrix.postScale(zoom / this.zoom, zoom / this.zoom, nonNaN(pivotX), nonNaN(pivotY))
        updateFromMatrix(updateZoom = true, updateRotation = false) // zoom & translation are changed
    }

    fun rotateBy(angle: Float, pivotX: Float, pivotY: Float) {
        matrix.postRotate(nonNaN(angle), nonNaN(pivotX), nonNaN(pivotY))
        updateFromMatrix(updateZoom = false, updateRotation = true) // rotation & translation are changed
    }

    fun rotateTo(angle: Float, pivotX: Float, pivotY: Float) {
        matrix.postRotate(-rotation + nonNaN(angle), nonNaN(pivotX), nonNaN(pivotY))
        updateFromMatrix(updateZoom = false, updateRotation = true) // rotation & translation are changed
    }

    fun set(x: Float, y: Float, zoom: Float, initialRotation: Float) {
        // Keeping rotation within the range [-180..180]
        var rotation = initialRotation
        while (rotation < -180f) {
            rotation += 360f
        }
        while (rotation > 180f) {
            rotation -= 360f
        }

        this.x = nonNaN(x)
        this.y = nonNaN(y)
        this.zoom = nonNaN(zoom)
        this.rotation = nonNaN(rotation)

        // Note, that order is vital here
        matrix.reset()
        if (zoom != 1F) {
            matrix.postScale(zoom, zoom)
        }
        if (rotation != 0F) {
            matrix.postRotate(rotation)
        }
        matrix.postTranslate(x, y)
    }

    /**
     * Applying state from given matrix. Matrix should contain correct translation/scale/rotation.
     *
     * @param matrix Source matrix
     */
    fun set(matrix: Matrix) {
        this.matrix.set(matrix)
        updateFromMatrix(updateZoom = true, updateRotation = true)
    }

    fun set(other: State) {
        x = other.x
        y = other.y
        zoom = other.zoom
        rotation = other.rotation
        matrix.set(other.matrix)
    }

    fun copy(): State {
        val copy = State()
        copy.set(this)
        return copy
    }

    /**
     * Applying state from current matrix.
     *
     * Having matrix:
     * <pre>
     *     | a  b  tx |
     * A = | c  d  ty |
     *     | 0  0  1  |
     *
     * x = tx
     * y = ty
     * scale = sqrt(b^2+d^2)
     * rotation = atan(c/d) = atan(-b/a)
     * </pre>
     * See [here](http://stackoverflow.com/questions/4361242).
     *
     * @param updateZoom Whether to extract zoom from matrix
     * @param updateRotation Whether to extract rotation from matrix
     */
    private fun updateFromMatrix(updateZoom: Boolean, updateRotation: Boolean) {
        matrix.getValues(matrixValues)
        x = matrixValues[2]
        y = matrixValues[5]
        if (updateZoom) {
            zoom = hypot(matrixValues[1].toDouble(), matrixValues[4].toDouble()).toFloat()
        }
        if (updateRotation) {
            rotation = Math.toDegrees(atan2(matrixValues[3].toDouble(), matrixValues[4].toDouble())).toFloat()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other !is State) return false
        return (equals(other.x, x) &&
                equals(other.y, y) &&
                equals(other.zoom, zoom) &&
                equals(other.rotation, rotation))
    }

    override fun hashCode(): Int {
        var result = if (x != +0.0F) floatToIntBits(x) else 0
        result = 31 * result + if (y != +0.0F) floatToIntBits(y) else 0
        result = 31 * result + if (zoom != +0.0F) floatToIntBits(zoom) else 0
        result = 31 * result + if (rotation != +0.0F) floatToIntBits(rotation) else 0
        return result
    }

    override fun toString(): String {
        return "{x=$x, y=$y, zoom=$zoom, rotation=$rotation}"
    }

}