package kz.zhombie.kaleidoscope

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.Gravity
import kz.zhombie.kaleidoscope.internal.UnitsUtils

/**
 * Various settings needed for [GestureController] and for [StateController].
 *
 *
 * Required settings are viewport size ([setViewport])
 * and image size [setImage]
 */
// Public API
@Suppress("unused", "MemberVisibilityCanBePrivate")
class Settings internal constructor() {

    companion object {
        const val MAX_ZOOM = 2F
        const val OVERZOOM_FACTOR = 2F
        const val ANIMATIONS_DURATION = 300L
    }

    /**
     * Viewport area
     */
    private var viewportWidth = 0
    private var viewportHeight = 0

    /**
     * Movement area
     */
    private var movementAreaWidth = 0
    private var movementAreaHeight = 0

    private var isMovementAreaSpecified = false

    /**
     * Image size
     */
    private var imageWidth = 0
    private var imageHeight = 0

    /**
     * Min zoom level, default value is 0F, meaning min zoom will be adjusted to fit viewport
     */
    private var minZoom = 0F

    /**
     * Max zoom level, default value is [MAX_ZOOM]
     */
    private var maxZoom = MAX_ZOOM

    /**
     * Double tap zoom level, default value is -1. Defaults to [maxZoom] if < 0.
     */
    private var doubleTapZoom = -1F

    /**
     * Overzoom factor
     */
    private var overzoomFactor = OVERZOOM_FACTOR

    /**
     * Overscroll distance
     */
    private var overscrollDistanceX = 0F
    private var overscrollDistanceY = 0F

    /**
     * If [isFillViewport] is true:
     * Small images will be scaled to fit viewport even if it will require zooming above max zoom.
     * Big images will be scaled to fit viewport even if it will require zooming below min zoom.
     */
    private var isFillViewport = false

    /**
     * Image gravity inside viewport area
     */
    private var gravity: Int = Gravity.CENTER

    /**
     * Initial fitting within viewport area
     */
    private var fitMethod = FitMethod.INSIDE

    /**
     * Movement bounds restriction type
     */
    private var boundsType = Bounds.NORMAL

    /**
     * Whether panning is enabled or not
     */
    private var isPanEnabled = true

    /**
     * Whether fling (inertial motion after scroll) is enabled or not
     */
    private var isFlingEnabled = true

    /**
     * Whether zooming is enabled or not
     */
    private var isZoomEnabled = true

    /**
     * Whether rotation gesture is enabled or not
     */
    private var isRotationEnabled = false

    /**
     * Whether image rotation should stick to 90 degrees or can be free
     */
    private var isRestrictRotation = false

    /**
     * Whether zooming by double tap is enabled or not
     */
    private var isDoubleTapEnabled = true

    /**
     * Which gestures to use to detect exit.
     */
    private var exitType = ExitType.ALL

    /**
     * Counter for gestures disabling calls.
     */
    private var gesturesDisableCount = 0

    /**
     * Counter for bounds disabling calls.
     */
    private var boundsDisableCount = 0

    /**
     * Duration of animations.
     */
    private var animationsDuration = ANIMATIONS_DURATION

    fun init(context: Context, attrs: AttributeSet?) {
        if (attrs == null) return

        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.GestureView)
        movementAreaWidth = typedArray.getDimensionPixelSize(
            R.styleable.GestureView_gest_movementAreaWidth,
            movementAreaWidth
        )
        movementAreaHeight = typedArray.getDimensionPixelSize(
            R.styleable.GestureView_gest_movementAreaHeight,
            movementAreaHeight
        )
        isMovementAreaSpecified = movementAreaWidth > 0 && movementAreaHeight > 0

        minZoom = typedArray.getFloat(R.styleable.GestureView_gest_minZoom, minZoom)
        maxZoom = typedArray.getFloat(R.styleable.GestureView_gest_maxZoom, maxZoom)
        doubleTapZoom = typedArray.getFloat(
            R.styleable.GestureView_gest_doubleTapZoom,
            doubleTapZoom
        )
        overzoomFactor = typedArray.getFloat(
            R.styleable.GestureView_gest_overzoomFactor,
            overzoomFactor
        )
        overscrollDistanceX = typedArray.getDimension(
            R.styleable.GestureView_gest_overscrollX,
            overscrollDistanceX
        )
        overscrollDistanceY = typedArray.getDimension(
            R.styleable.GestureView_gest_overscrollY,
            overscrollDistanceY
        )
        isFillViewport = typedArray.getBoolean(
            R.styleable.GestureView_gest_fillViewport,
            isFillViewport
        )
        gravity = typedArray.getInt(R.styleable.GestureView_gest_gravity, gravity)

        val fitMethodPos: Int = typedArray.getInteger(
            R.styleable.GestureView_gest_fitMethod,
            fitMethod.ordinal
        )
        fitMethod = FitMethod.values()[fitMethodPos]

        val boundsTypePos: Int = typedArray.getInteger(
            R.styleable.GestureView_gest_boundsType,
            boundsType.ordinal
        )
        boundsType = Bounds.values()[boundsTypePos]
        isPanEnabled = typedArray.getBoolean(R.styleable.GestureView_gest_panEnabled, isPanEnabled)
        isFlingEnabled = typedArray.getBoolean(
            R.styleable.GestureView_gest_flingEnabled,
            isFlingEnabled
        )
        isZoomEnabled = typedArray.getBoolean(
            R.styleable.GestureView_gest_zoomEnabled,
            isZoomEnabled
        )
        isRotationEnabled = typedArray.getBoolean(
            R.styleable.GestureView_gest_rotationEnabled,
            isRotationEnabled
        )
        isRestrictRotation = typedArray.getBoolean(
            R.styleable.GestureView_gest_restrictRotation,
            isRestrictRotation
        )
        isDoubleTapEnabled = typedArray.getBoolean(
            R.styleable.GestureView_gest_doubleTapEnabled,
            isDoubleTapEnabled
        )
        exitType = if (typedArray.getBoolean(R.styleable.GestureView_gest_exitEnabled, true)) {
            exitType
        } else {
            ExitType.NONE
        }
        animationsDuration = typedArray.getInt(
            R.styleable.GestureView_gest_animationDuration,
            animationsDuration.toInt()
        ).toLong()

        val disableGestures: Boolean = typedArray.getBoolean(
            R.styleable.GestureView_gest_disableGestures,
            false
        )
        if (disableGestures) {
            disableGestures()
        }

        val disableBounds: Boolean = typedArray.getBoolean(
            R.styleable.GestureView_gest_disableBounds,
            false
        )
        if (disableBounds) {
            disableBounds()
        }

        typedArray.recycle()
    }

    /**
     * Setting viewport size.
     *
     * Should only be used when implementing custom [GestureView].
     *
     * @param width Viewport width
     * @param height Viewport height
     * @return Current settings object for calls chaining
     */
    fun setViewport(width: Int, height: Int): Settings {
        viewportWidth = width
        viewportHeight = height
        return this
    }

    /**
     * Setting movement area size. Viewport area will be used instead if no movement area is
     * specified.
     *
     * @param width Movement area width
     * @param height Movement area height
     * @return Current settings object for calls chaining
     */
    fun setMovementArea(width: Int, height: Int): Settings {
        isMovementAreaSpecified = true
        movementAreaWidth = width
        movementAreaHeight = height
        return this
    }

    /**
     * Setting full image size.
     *
     * Should only be used when implementing custom [GestureView].
     *
     * @param width Image width
     * @param height Image height
     * @return Current settings object for calls chaining
     */
    fun setImage(width: Int, height: Int): Settings {
        imageWidth = width
        imageHeight = height
        return this
    }

    /**
     * Setting min zoom level.
     *
     * Default value is 0.
     *
     * @param minZoom Min zoom level, or 0 to use zoom level which fits the image into the viewport.
     * @return Current settings object for calls chaining
     */
    fun setMinZoom(minZoom: Float): Settings {
        this.minZoom = minZoom
        return this
    }

    /**
     * Setting max zoom level.
     *
     * Default value is [MAX_ZOOM].
     *
     * @param maxZoom Max zoom level, or 0 to use zoom level which fits the image into the viewport.
     * @return Current settings object for calls chaining
     */
    fun setMaxZoom(maxZoom: Float): Settings {
        this.maxZoom = maxZoom
        return this
    }

    /**
     * Setting double tap zoom level, should not be greater than [maxZoom].
     * Defaults to [maxZoom] if <= 0.
     *
     * Default value is -1.
     *
     * @param doubleTapZoom Double tap zoom level
     * @return Current settings object for calls chaining
     */
    fun setDoubleTapZoom(doubleTapZoom: Float): Settings {
        this.doubleTapZoom = doubleTapZoom
        return this
    }

    /**
     * Setting overzoom factor. User will be able to "over zoom" up to this factor.
     * Cannot be < 1.
     *
     * Default value is [OVERZOOM_FACTOR].
     *
     * @param overzoomFactor Overzoom factor
     * @return Current settings object for calls chaining
     */
    fun setOverzoomFactor(overzoomFactor: Float): Settings {
        require(overzoomFactor >= 1F) { "Overzoom factor cannot be < 1" }
        this.overzoomFactor = overzoomFactor
        return this
    }

    /**
     * Setting overscroll distance in pixels. User will be able to "over scroll"
     * up to this distance. Cannot be < 0.
     *
     * Default value is 0.
     *
     * @param distanceX Horizontal overscroll distance in pixels
     * @param distanceY Vertical overscroll distance in pixels
     * @return Current settings object for calls chaining
     */
    fun setOverscrollDistance(distanceX: Float, distanceY: Float): Settings {
        require(!(distanceX < 0f || distanceY < 0f)) { "Overscroll distance cannot be < 0" }
        overscrollDistanceX = distanceX
        overscrollDistanceY = distanceY
        return this
    }

    /**
     * Same as [setOverscrollDistance] but accepts distance in DP.
     *
     * @param context Context
     * @param distanceXDp Horizontal overscroll distance in dp
     * @param distanceYDp Vertical overscroll distance in dp
     * @return Current settings object for calls chaining
     */
    fun setOverscrollDistance(context: Context, distanceXDp: Float, distanceYDp: Float): Settings {
        return setOverscrollDistance(
            UnitsUtils.toPixels(context, distanceXDp),
            UnitsUtils.toPixels(context, distanceYDp)
        )
    }

    /**
     * If set to true small images will be scaled to fit entire viewport (or entire movement area
     * if it was set) even if this will require zoom level above max zoom level. And big images
     * will be scaled to fit the viewport even if it will require zooming below min zoom.
     *
     * Default value is false.
     *
     * @param isFillViewport Whether image should fit viewport or not
     * @return Current settings object for calls chaining
     */
    fun setFillViewport(isFillViewport: Boolean): Settings {
        this.isFillViewport = isFillViewport
        return this
    }

    /**
     * Setting image gravity inside viewport area.
     *
     * Default value is [Gravity.CENTER].
     *
     * @param gravity Image gravity, one of [Gravity] constants
     * @return Current settings object for calls chaining
     */
    fun setGravity(gravity: Int): Settings {
        this.gravity = gravity
        return this
    }

    /**
     * Setting image fitting method within viewport area.
     *
     * Default value is [FitMethod.INSIDE].
     *
     * @param fitMethodMethod Fit method
     * @return Current settings object for calls chaining
     */
    fun setFitMethod(fitMethodMethod: FitMethod): Settings {
        this.fitMethod = fitMethodMethod
        return this
    }

    /**
     * Setting movement bounds restriction type.
     *
     * Default value is [Bounds.NORMAL].
     *
     * @param boundsType Bounds restrictions type
     * @return Current settings object for calls chaining
     */
    fun setBoundsType(boundsType: Bounds): Settings {
        this.boundsType = boundsType
        return this
    }

    /**
     * Sets whether panning is enabled or not.
     *
     * Default value is true.
     *
     * @param isPanEnabled Whether panning should be enabled or not
     * @return Current settings object for calls chaining
     */
    fun setPanEnabled(isPanEnabled: Boolean): Settings {
        this.isPanEnabled = isPanEnabled
        return this
    }

    /**
     * Sets whether fling (inertial motion after scroll) is enabled or not.
     *
     * Default value is true.
     *
     * @param isFlingEnabled Whether fling should be enabled or not
     * @return Current settings object for calls chaining
     */
    fun setFlingEnabled(isFlingEnabled: Boolean): Settings {
        this.isFlingEnabled = isFlingEnabled
        return this
    }

    /**
     * Sets whether zooming is enabled or not.
     *
     * Default value is true.
     *
     * @param isZoomEnabled Whether zooming should be enabled or not
     * @return Current settings object for calls chaining
     */
    fun setZoomEnabled(isZoomEnabled: Boolean): Settings {
        this.isZoomEnabled = isZoomEnabled
        return this
    }

    /**
     * Sets whether rotation gesture is enabled or not.
     *
     * Default value is false.
     *
     * @param isRotationEnabled Whether rotation should be enabled or not
     * @return Current settings object for calls chaining
     */
    fun setRotationEnabled(isRotationEnabled: Boolean): Settings {
        this.isRotationEnabled = isRotationEnabled
        return this
    }

    /**
     * Sets whether image rotation should stick to 90 degrees intervals or can be free.
     * Only applied when [isRestrictBounds] is true as well.
     *
     * Default value is false.
     *
     * @param restrict Whether rotation should be restricted or not
     * @return Current settings object for calls chaining
     */
    fun setRestrictRotation(restrict: Boolean): Settings {
        isRestrictRotation = restrict
        return this
    }

    /**
     * Sets whether zooming by double tap is enabled or not.
     *
     * Default value is true.
     *
     * @param isDoubleTapEnabled Whether double tap should be enabled or not
     * @return Current settings object for calls chaining
     */
    fun setDoubleTapEnabled(isDoubleTapEnabled: Boolean): Settings {
        this.isDoubleTapEnabled = isDoubleTapEnabled
        return this
    }

    /**
     * Sets whether to detect and animate exit from gesture views.
     *
     * Default value is true.
     *
     * @param enabled Whether exit gesture should be enabled or not
     * @return Current settings object for calls chaining
     */
    fun setExitEnabled(enabled: Boolean): Settings {
        exitType = if (enabled) ExitType.ALL else ExitType.NONE
        return this
    }

    /**
     * Sets which gestures to use to detect exit.
     *
     * Default value is [ExitType.ALL].
     *
     * @param type Exit type
     * @return Current settings object for calls chaining
     */
    fun setExitType(type: ExitType): Settings {
        exitType = type
        return this
    }

    /**
     * Disable all gestures.
     * Calls to this method are counted, so if you called it N times
     * you should call [enableGestures] N times to re-enable all gestures.
     *
     * Useful when you need to temporary disable touch gestures during animation or image loading.
     *
     * See also [enableGestures]
     *
     * @return Current settings object for calls chaining
     */
    fun disableGestures(): Settings {
        gesturesDisableCount++
        return this
    }

    /**
     * Re-enable all gestures disabled by [disableGestures] method.
     * Calls to this method are counted, so if you called [disableGestures] N times
     * you should call this method N times to re-enable all gestures.
     *
     * See also [disableGestures]
     *
     * @return Current settings object for calls chaining
     */
    fun enableGestures(): Settings {
        gesturesDisableCount--
        return this
    }

    /**
     * Disable bounds restrictions.
     * Calls to this method are counted, so if you called it N times
     * you should call [enableBounds] N times to re-enable bounds restrictions.
     *
     * Useful when you need to temporary disable bounds restrictions during animation.
     *
     * See also [enableBounds]
     *
     * @return Current settings object for calls chaining
     */
    fun disableBounds(): Settings {
        boundsDisableCount++
        return this
    }

    /**
     * Re-enable bounds restrictions disabled by [disableBounds] method.
     * Calls to this method are counted, so if you called [disableBounds] N times
     * you should call this method N times to re-enable bounds restrictions.
     *
     * See also [disableBounds]
     *
     * @return Current settings object for calls chaining
     */
    fun enableBounds(): Settings {
        boundsDisableCount--
        return this
    }

    /**
     * Duration of animations.
     *
     * @param duration Animation duration in milliseconds
     * @return Current settings object for calls chaining
     */
    fun setAnimationsDuration(duration: Long): Settings {
        require(duration >= 0L) { "Animations duration should be >= 0" }
        animationsDuration = duration
        return this
    }

    fun getViewportW(): Int = viewportWidth

    fun getViewportH(): Int = viewportHeight

    fun getMovementAreaWidth(): Int = if (isMovementAreaSpecified) {
        movementAreaWidth
    } else {
        viewportWidth
    }

    fun getMovementAreaHeight(): Int = if (isMovementAreaSpecified) {
        movementAreaHeight
    } else {
        viewportHeight
    }

    fun getImageWidth(): Int = imageWidth

    fun getImageHeight(): Int = imageHeight

    fun getMinZoom(): Float = minZoom

    fun getMaxZoom(): Float = maxZoom

    fun getDoubleTapZoom(): Float = doubleTapZoom

    fun getOverzoomFactor(): Float = overzoomFactor

    fun getOverscrollDistanceX(): Float = overscrollDistanceX

    fun getOverscrollDistanceY(): Float = overscrollDistanceY

    fun isFillViewport(): Boolean = isFillViewport

    fun getGravity(): Int = gravity

    fun getFitMethod(): FitMethod= fitMethod

    fun getBoundsType(): Bounds = boundsType

    fun isPanEnabled(): Boolean = isGesturesEnabled() && isPanEnabled

    fun isFlingEnabled(): Boolean = isGesturesEnabled() && isFlingEnabled

    fun isZoomEnabled(): Boolean = isGesturesEnabled() && isZoomEnabled

    fun isRotationEnabled(): Boolean = isGesturesEnabled() && isRotationEnabled

    fun isRestrictRotation(): Boolean = isRestrictRotation

    fun isDoubleTapEnabled(): Boolean = isGesturesEnabled() && isDoubleTapEnabled

    fun isExitEnabled(): Boolean = getExitType() != ExitType.NONE

    fun getExitType(): ExitType = if (isGesturesEnabled()) exitType else ExitType.NONE

    fun isGesturesEnabled(): Boolean = gesturesDisableCount <= 0

    fun isRestrictBounds(): Boolean = boundsDisableCount <= 0

    fun getAnimationsDuration(): Long = animationsDuration

    /**
     * @return Whether at least one of pan, zoom, rotation or double tap are enabled or not
     */
    fun isEnabled(): Boolean =
        (isGesturesEnabled() && (isPanEnabled || isZoomEnabled || isRotationEnabled || isDoubleTapEnabled))

    fun hasImageSize(): Boolean = imageWidth != 0 && imageHeight != 0

    fun hasViewportSize(): Boolean = viewportWidth != 0 && viewportHeight != 0

    enum class FitMethod {
        /**
         * Fit image width inside viewport area.
         */
        HORIZONTAL,

        /**
         * Fit image height inside viewport area.
         */
        VERTICAL,

        /**
         * Fit both image width and image height inside viewport area.
         */
        INSIDE,

        /**
         * Fit image width or image height inside viewport area, so the entire viewport is filled.
         */
        OUTSIDE,

        /**
         * Do not fit the image into viewport area.
         */
        NONE
    }

    enum class Bounds {
        /**
         * The image is moved within the movement area and always placed according to gravity
         * if less than the area.
         */
        NORMAL,

        /**
         * The image is moved within the movement area and can be freely moved inside the area
         * if less than the area.
         */
        INSIDE,

        /**
         * The image can be freely moved until it's completely outside of the movement area.
         */
        OUTSIDE,

        /**
         * The image can be freely moved until it contains a pivot point (e.g. center point if
         * the gravity is set to [Gravity.CENTER]).
         */
        PIVOT,

        /**
         * The image can be freely moved with no restrictions.
         */
        NONE
    }

    enum class ExitType {
        /**
         * To detect both scroll and zoom exit gestures.
         */
        ALL,

        /**
         * To detect only scroll-to-exit gesture.
         */
        SCROLL,

        /**
         * To detect only zoom-to-exit gesture.
         */
        ZOOM,

        /**
         * Do not detect exit gestures.
         */
        NONE
    }

}