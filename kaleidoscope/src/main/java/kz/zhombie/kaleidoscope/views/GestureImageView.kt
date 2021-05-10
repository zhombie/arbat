package kz.zhombie.kaleidoscope.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import kz.zhombie.kaleidoscope.GestureController
import kz.zhombie.kaleidoscope.GestureControllerForPager
import kz.zhombie.kaleidoscope.Settings
import kz.zhombie.kaleidoscope.State
import kz.zhombie.kaleidoscope.animation.ViewPositionAnimator
import kz.zhombie.kaleidoscope.internal.GestureDebug
import kz.zhombie.kaleidoscope.utils.ClipHelper
import kz.zhombie.kaleidoscope.utils.CropUtils
import kz.zhombie.kaleidoscope.views.interfaces.AnimatorView
import kz.zhombie.kaleidoscope.views.interfaces.ClipBounds
import kz.zhombie.kaleidoscope.views.interfaces.ClipView
import kz.zhombie.kaleidoscope.views.interfaces.GestureView
import kotlin.math.min

/**
 * [ImageView] implementation controlled by [GestureController] ([getController]).
 *
 *
 * View position can be animated with [ViewPositionAnimator] ([positionAnimator]).
 */
class GestureImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), GestureView, ClipView, ClipBounds, AnimatorView {

    private var controller: GestureControllerForPager? = null
    private val clipViewHelper: ClipHelper = ClipHelper(this)
    private val clipBoundsHelper: ClipHelper = ClipHelper(this)
    private val imageMatrix = Matrix()

    init {
        ensureControllerCreated()
        controller?.getSettings()?.init(context, attrs)
        controller?.addOnStateChangeListener(object : GestureController.OnStateChangeListener {
            override fun onStateChanged(state: State) {
                applyState(state)
            }

            override fun onStateReset(oldState: State, newState: State) {
                applyState(newState)
            }
        })
        scaleType = ScaleType.MATRIX
    }

    private var positionAnimator: ViewPositionAnimator? = null

    override fun getPositionAnimator(): ViewPositionAnimator {
        return positionAnimator
    }

    private fun setPositionAnimator(positionAnimator: ViewPositionAnimator) {
        if (positionAnimator == null) {
            positionAnimator = ViewPositionAnimator(this)
        }
        return positionAnimator
    }

    private fun ensureControllerCreated() {
        if (controller == null) {
            controller = GestureControllerForPager(this)
        }
    }

    override fun draw(canvas: Canvas) {
        clipBoundsHelper.onPreDraw(canvas)
        clipViewHelper.onPreDraw(canvas)
        super.draw(canvas)
        clipViewHelper.onPostDraw(canvas)
        clipBoundsHelper.onPostDraw(canvas)
        if (GestureDebug.isDrawDebugOverlay()) {
            DebugOverlay.drawDebug(this, canvas)
        }
    }

    override fun getController(): GestureControllerForPager {
        return controller
    }

    override fun clipView(rect: RectF?, rotation: Float) {
        clipViewHelper.clipView(rect, rotation)
    }

    override fun clipBounds(rect: RectF?) {
        clipBoundsHelper.clipView(rect, 0f)
    }

    /**
     * Crops bitmap as it is seen inside movement area: [Settings.setMovementArea].
     *
     *
     * Note, that size of cropped bitmap may vary from size of movement area,
     * since we will crop part of original image at base zoom level (zoom == 1).
     *
     * @return Cropped bitmap or null, if no image is set to this image view or if
     * [OutOfMemoryError] error was thrown during cropping.
     */
    fun crop(): Bitmap? {
        return CropUtils.crop(drawable, controller)
    }

    @SuppressLint("ClickableViewAccessibility")  // performClick() will be called by controller
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return controller?.onTouch(this, event) == true
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        controller?.getSettings()?.setViewport(
            width - paddingLeft - paddingRight,
            height - paddingTop - paddingBottom
        )
        controller?.resetState()
    }

    override fun setImageResource(resId: Int) {
        setImageDrawable(getDrawable(context, resId))
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)

        // Method setImageDrawable can be called from super constructor,
        // so we have to ensure controller instance is created at this point.
        ensureControllerCreated()
        val settings: Settings = controller.getSettings()

        // Saving old image size
        val oldWidth: Float = settings.getImageWidth().toFloat()
        val oldHeight: Float = settings.getImageHeight().toFloat()

        // Setting image size
        if (drawable == null) {
            settings.setImage(0, 0)
        } else if (drawable.intrinsicWidth == -1 || drawable.intrinsicHeight == -1) {
            settings.setImage(settings.getMovementAreaWidth(), settings.getMovementAreaHeight())
        } else {
            settings.setImage(drawable.intrinsicWidth, drawable.intrinsicHeight)
        }

        // Getting new image size
        val newWidth: Float = settings.getImageWidth().toFloat()
        val newHeight: Float = settings.getImageHeight().toFloat()
        if (newWidth > 0f && newHeight > 0f && oldWidth > 0f && oldHeight > 0f) {
            val scaleFactor = min(oldWidth / newWidth, oldHeight / newHeight)
            controller.getStateController().setTempZoomPatch(scaleFactor)
            controller.updateState()
            controller.getStateController().setTempZoomPatch(0f)
        } else {
            controller.resetState()
        }
    }

    protected fun applyState(state: State) {
        state.get(imageMatrix)
        setImageMatrix(imageMatrix)
    }

}