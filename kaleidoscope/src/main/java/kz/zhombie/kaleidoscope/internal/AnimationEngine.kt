package kz.zhombie.kaleidoscope.internal

import android.view.View
import androidx.core.view.ViewCompat

abstract class AnimationEngine constructor(private val view: View) : Runnable {

    companion object {
        private const val FRAME_TIME = 10L
    }

    private val fps: Fps? = if (GestureDebug.isDebugFps()) Fps() else null

    override fun run() {
        val continueAnimation = onStep()

        if (fps != null) {
            fps.step()
            if (!continueAnimation) {
                fps.stop()
            }
        }

        if (continueAnimation) {
            scheduleNextStep()
        }
    }

    abstract fun onStep(): Boolean

    private fun scheduleNextStep() {
        view.removeCallbacks(this)
        ViewCompat.postOnAnimationDelayed(view, this, FRAME_TIME)
    }

    fun start() {
        fps?.start()
        scheduleNextStep()
    }

}