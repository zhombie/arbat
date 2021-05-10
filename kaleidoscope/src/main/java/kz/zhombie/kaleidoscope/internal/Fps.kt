package kz.zhombie.kaleidoscope.internal

import android.os.SystemClock
import android.util.Log
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal class Fps {

    companion object {
        private const val TAG = "GestureFps"
        private const val WARNING_TIME = 20L // Dropping less than 60 fps in average
        private const val ERROR_TIME = 40L // Dropping less than 30 fps in average
    }

    private var frameStart: Long = 0
    private var animationStart: Long = 0
    private var framesCount = 0

    fun start() {
        if (GestureDebug.isDebugFps()) {
            frameStart = SystemClock.uptimeMillis()
            animationStart = frameStart
            framesCount = 0
        }
    }

    fun stop() {
        if (GestureDebug.isDebugFps() && framesCount > 0) {
            val time = (SystemClock.uptimeMillis() - animationStart).toInt()
            Log.d(TAG, "Average FPS: " + (1000F * framesCount / time).roundToLong())
        }
    }

    fun step() {
        if (GestureDebug.isDebugFps()) {
            val frameTime = SystemClock.uptimeMillis() - frameStart
            if (frameTime > ERROR_TIME) {
                Log.e(TAG, "Frame time: $frameTime")
            } else if (frameTime > WARNING_TIME) {
                Log.w(TAG, "Frame time: $frameTime")
            }

            framesCount++
            frameStart = SystemClock.uptimeMillis()
        }
    }

}