package kz.zhombie.kaleidoscope.animation

import android.view.View
import android.view.ViewTreeObserver
import kz.zhombie.kaleidoscope.animation.ViewPositionHolder.OnViewPositionChangeListener

/**
 * Helper class that monitors [View] position on screen and notifies
 * [OnViewPositionChangeListener] if any changes were detected.
 */
internal class ViewPositionHolder : ViewTreeObserver.OnPreDrawListener {

    companion object {
        private fun isLaidOut(view: View): Boolean {
            return view.isLaidOut
        }

        private fun isAttached(view: View): Boolean {
            return view.isAttachedToWindow
        }
    }

    private val viewPosition = ViewPosition.newInstance()

    private var listener: OnViewPositionChangeListener? = null
    private var view: View? = null
    private var attachListener: View.OnAttachStateChangeListener? = null
    private var isPaused: Boolean = false

    override fun onPreDraw(): Boolean {
        update()
        return true
    }

    fun init(view: View, listener: OnViewPositionChangeListener) {
        clear() // Cleaning up old listeners, just in case

        this.view = view
        this.listener = listener

        attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                onViewAttached(view, true)
            }

            override fun onViewDetachedFromWindow(view: View) {
                onViewAttached(view, false)
            }
        }
        view.addOnAttachStateChangeListener(attachListener)

        onViewAttached(view, isAttached(view))

        if (isLaidOut(view)) {
            update()
        }
    }

    private fun onViewAttached(view: View, attached: Boolean) {
        view.viewTreeObserver.removeOnPreDrawListener(this)
        if (attached) {
            view.viewTreeObserver.addOnPreDrawListener(this)
        }
    }

    fun clear() {
        if (view != null) {
            view!!.removeOnAttachStateChangeListener(attachListener)
            onViewAttached(view!!, false)
        }

        viewPosition.view.setEmpty()
        viewPosition.viewport.setEmpty()
        viewPosition.image.setEmpty()

        view = null
        attachListener = null
        listener = null
        isPaused = false
    }

    fun pause(paused: Boolean) {
        if (isPaused == paused) return

        isPaused = paused
        update()
    }

    private fun update() {
        if (view != null && listener != null && !isPaused) {
            val changed = ViewPosition.apply(viewPosition, view!!)
            if (changed) {
                listener!!.onViewPositionChanged(viewPosition)
            }
        }
    }

    internal interface OnViewPositionChangeListener {
        fun onViewPositionChanged(position: ViewPosition)
    }

}