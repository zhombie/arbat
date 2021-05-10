package kz.zhombie.kaleidoscope

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import kz.zhombie.kaleidoscope.internal.MovementBounds
import kz.zhombie.kaleidoscope.internal.ZoomBounds
import kz.zhombie.kaleidoscope.utils.GravityUtils
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Helper class that holds reference to [Settings] object and controls some aspects of view
 * [State], such as movement bounds restrictions
 * (see [getMovementArea]) and dynamic min/max zoom levels
 * (see [getMinZoom] and [getMaxZoom]).
 */
class StateController internal constructor(private val settings: Settings) {

    companion object {
        // Temporary objects
        private val tmpState = State()
        private val tmpRect = Rect()
        private val tmpRectF: RectF = RectF()
        private val tmpPointF: PointF = PointF()
    }

    private val zoomBounds: ZoomBounds = ZoomBounds(settings)
    private val movBounds: MovementBounds = MovementBounds(settings)

    private var isResetRequired = true

    private var zoomPatch = 0F

    /**
     * Resets to initial state (min zoom, position according to gravity). Reset will only occur
     * when image and viewport sizes are known, otherwise reset will occur sometime in the future
     * when [updateState] method will be called.
     *
     * @param state State to be reset
     * @return `true` if reset was completed or `false` if reset is scheduled for future
     */
    fun resetState(state: State): Boolean {
        isResetRequired = true
        return updateState(state)
    }

    /**
     * Updates state (or resets state if reset was scheduled, see [resetState]).
     *
     * @param state State to be updated
     * @return `true` if state was reset to initial state or `false` if state was
     * updated.
     */
    fun updateState(state: State): Boolean {
        return if (isResetRequired) {
            // Applying initial state
            state[0f, 0f, zoomBounds.set(state).getFitZoom()] = 0f
            GravityUtils.getImagePosition(state, settings, tmpRect)
            state.translateTo(tmpRect.left.toFloat(), tmpRect.top.toFloat())

            // We can correctly reset state only when we have both image size and viewport size
            // but there can be a delay before we have all values properly set
            // (waiting for layout or waiting for image to be loaded)
            isResetRequired = !settings.hasImageSize() || !settings.hasViewportSize()
            !isResetRequired
        } else {
            // Restricts state's translation and zoom bounds, disallowing overscroll / overzoom.
            restrictStateBounds(
                state = state,
                prevState = state,
                pivotX = Float.NaN,
                pivotY = Float.NaN,
                allowOverscroll = false,
                allowOverzoom = false,
                restrictRotation = true
            )
            false
        }
    }

    fun setTempZoomPatch(factor: Float) {
        zoomPatch = factor
    }

    fun applyZoomPatch(state: State) {
        if (zoomPatch > 0f) {
            state[state.getX(), state.getY(), state.getZoom() * zoomPatch] = state.getRotation()
        }
    }

    fun applyZoomPatch(zoom: Float): Float {
        return if (zoomPatch > 0F) zoomPatch * zoom else zoom
    }

    /**
     * Maximizes zoom if it closer to min zoom or minimizes it if it closer to max zoom.
     *
     * @param state Current state
     * @param pivotX Pivot's X coordinate
     * @param pivotY Pivot's Y coordinate
     * @return End state for toggle animation.
     */
    fun toggleMinMaxZoom(state: State, pivotX: Float, pivotY: Float): State {
        zoomBounds.set(state)
        val minZoom: Float = zoomBounds.getFitZoom()
        val maxZoom = if (settings.getDoubleTapZoom() > 0f) {
            settings.getDoubleTapZoom()
        } else {
            zoomBounds.getMaxZoom()
        }

        val middleZoom = 0.5f * (minZoom + maxZoom)
        val targetZoom = if (state.getZoom() < middleZoom) maxZoom else minZoom

        val end = state.copy()
        end.zoomTo(targetZoom, pivotX, pivotY)
        return end
    }

    /**
     * Restricts state's translation and zoom bounds.
     *
     * @param state State to be restricted
     * @param prevState Previous state to calculate overscroll and overzoom (optional)
     * @param pivotX Pivot's X coordinate
     * @param pivotY Pivot's Y coordinate
     * @param allowOverscroll Whether overscroll is allowed
     * @param allowOverzoom Whether overzoom is allowed
     * @param restrictRotation Whether rotation should be restricted to a nearest N*90 angle
     * @return End state to animate changes or null if no changes are required.
     */
    fun restrictStateBoundsCopy(
        state: State, prevState: State?,
        pivotX: Float, pivotY: Float,
        allowOverscroll: Boolean, allowOverzoom: Boolean,
        restrictRotation: Boolean
    ): State? {
        tmpState.set(state)
        val changed = restrictStateBounds(
            state = tmpState,
            prevState = prevState,
            pivotX = pivotX,
            pivotY = pivotY,
            allowOverscroll = allowOverscroll,
            allowOverzoom = allowOverzoom,
            restrictRotation = restrictRotation
        )
        return if (changed) tmpState.copy() else null
    }

    /**
     * Restricts state's translation and zoom bounds. If [prevState] is not null and
     * [allowOverscroll] `(allowOverzoom)` parameter is true then resilience
     * will be applied to translation (zoom) changes if they are out of bounds.
     *
     * @param state State to be restricted
     * @param prevState Previous state to calculate overscroll and overzoom (optional)
     * @param pivotX Pivot's X coordinate
     * @param pivotY Pivot's Y coordinate
     * @param allowOverscroll Whether overscroll is allowed
     * @param allowOverzoom Whether overzoom is allowed
     * @param restrictRotation Whether rotation should be restricted to a nearest N*90 angle
     * @return true if state was changed, false otherwise.
     */
    fun restrictStateBounds(
        state: State, prevState: State?,
        pivotX: Float, pivotY: Float,
        allowOverscroll: Boolean, allowOverzoom: Boolean,
        restrictRotation: Boolean
    ): Boolean {
        var innerPivotX = pivotX
        var innerPivotY = pivotY

        if (!settings.isRestrictBounds()) {
            return false
        }

        if (java.lang.Float.isNaN(innerPivotX) || java.lang.Float.isNaN(innerPivotY)) {
            innerPivotX = state.getX()
            innerPivotY = state.getY()
        }

        var isStateChanged = false

        if (restrictRotation && settings.isRestrictRotation()) {
            val rotation = (state.getRotation() / 90f).roundToInt() * 90f
            if (!State.equals(rotation, state.getRotation())) {
                state.rotateTo(rotation, innerPivotX, innerPivotY)
                isStateChanged = true
            }
        }

        zoomBounds.set(state)
        val minZoom: Float = zoomBounds.getMinZoom()
        val maxZoom: Float = zoomBounds.getMaxZoom()

        val extraZoom = if (allowOverzoom) settings.getOverzoomFactor() else 1f
        var zoom: Float = zoomBounds.restrict(state.getZoom(), extraZoom)

        // Applying elastic overzoom
        if (prevState != null) {
            zoom = applyZoomResilience(zoom, prevState.getZoom(), minZoom, maxZoom, extraZoom)
        }

        if (!State.equals(zoom, state.getZoom())) {
            state.zoomTo(zoom, innerPivotX, innerPivotY)
            isStateChanged = true
        }

        val extraX = if (allowOverscroll) settings.getOverscrollDistanceX() else 0f
        val extraY = if (allowOverscroll) settings.getOverscrollDistanceY() else 0f

        movBounds.set(state)
        movBounds.restrict(state.getX(), state.getY(), extraX, extraY, tmpPointF)
        var newX: Float = tmpPointF.x
        var newY: Float = tmpPointF.y

        if (zoom < minZoom) {
            // Decreasing overscroll if zooming less than minimum zoom
            var factor = (extraZoom * zoom / minZoom - 1f) / (extraZoom - 1f)
            factor = sqrt(factor.toDouble()).toFloat()

            movBounds.restrict(newX, newY, tmpPointF)
            val strictX: Float = tmpPointF.x
            val strictY: Float = tmpPointF.y

            newX = strictX + factor * (newX - strictX)
            newY = strictY + factor * (newY - strictY)
        }

        if (prevState != null) {
            movBounds.getExternalBounds(tmpRectF)
            newX = applyTranslationResilience(
                value = newX,
                prevValue = prevState.getX(),
                boundsMin = tmpRectF.left,
                boundsMax = tmpRectF.right,
                overscroll = extraX
            )
            newY = applyTranslationResilience(
                value = newY,
                prevValue = prevState.getY(),
                boundsMin = tmpRectF.top,
                boundsMax = tmpRectF.bottom,
                overscroll = extraY
            )
        }
        if (!State.equals(newX, state.getX()) || !State.equals(newY, state.getY())) {
            state.translateTo(newX, newY)
            isStateChanged = true
        }

        return isStateChanged
    }

    private fun applyZoomResilience(
        zoom: Float, prevZoom: Float,
        minZoom: Float, maxZoom: Float,
        overzoom: Float
    ): Float {
        if (overzoom == 1f) {
            return zoom
        }

        val minZoomOver = minZoom / overzoom
        val maxZoomOver = maxZoom * overzoom

        var resilience = 0f

        if (zoom < minZoom && zoom < prevZoom) {
            resilience = (minZoom - zoom) / (minZoom - minZoomOver)
        } else if (zoom > maxZoom && zoom > prevZoom) {
            resilience = (zoom - maxZoom) / (maxZoomOver - maxZoom)
        }

        return if (resilience == 0f) {
            zoom
        } else {
            resilience = sqrt(resilience)
            zoom + resilience * (prevZoom - zoom)
        }
    }

    private fun applyTranslationResilience(
        value: Float, prevValue: Float,
        boundsMin: Float, boundsMax: Float,
        overscroll: Float
    ): Float {
        if (overscroll == 0f) {
            return value
        }

        var resilience = 0f

        val avg = (value + prevValue) * 0.5f

        if (avg < boundsMin && value < prevValue) {
            resilience = (boundsMin - avg) / overscroll
        } else if (avg > boundsMax && value > prevValue) {
            resilience = (avg - boundsMax) / overscroll
        }

        return if (resilience == 0f) {
            value
        } else {
            if (resilience > 1f) {
                resilience = 1f
            }
            resilience = sqrt(resilience)
            value - resilience * (value - prevValue)
        }
    }

    /**
     * @param state Current state
     * @return Min zoom level as it's used by state controller.
     */
    fun getMinZoom(state: State): Float {
        return zoomBounds.set(state).getMinZoom()
    }

    /**
     * @param state Current state
     * @return Max zoom level as it's used by state controller.
     * Note, that it may be different from [Settings.getMaxZoom].
     */
    fun getMaxZoom(state: State): Float {
        return zoomBounds.set(state).getMaxZoom()
    }

    /**
     * @param state Current state
     * @return Zoom level which will fit the image into viewport (or min zoom level if
     * [Settings.getFitMethod] is [Settings.FitMethod.NONE]).
     */
    fun getFitZoom(state: State): Float {
        return zoomBounds.set(state).getFitZoom()
    }

    /**
     * Calculates area in which [State.getX] & [State.getY] values can change.
     * Note, that this is different from [Settings.setMovementArea] which defines
     * part of the viewport in which image can move.
     *
     * @param state Current state
     * @param out Output movement area rectangle
     */
    fun getMovementArea(state: State, out: RectF) {
        movBounds.set(state).getExternalBounds(out)
    }

}